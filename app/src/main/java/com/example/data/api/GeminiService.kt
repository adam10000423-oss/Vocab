package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.dictionary.DictionaryEntry
import com.example.util.OcrCardCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * AI vocabulary client. The Android app never receives a Gemini API key; all
 * requests go through the companion backend configured by BACKEND_BASE_URL.
 */
object GeminiService {
    private const val TAG = "VocabularyApi"
    private const val MAX_DOCUMENT_CHARACTERS = 20_000
    private const val MAX_BATCH_WORDS = 80

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    @Volatile
    private var personalConfigs: List<PersonalAiConfig> = emptyList()
    private val providerRotation = ConcurrentHashMap<AiProvider, AtomicInteger>()
    @Volatile
    private var wordDetailsPrompt: String = AiPromptDefaults.WORD_DETAILS
    @Volatile
    private var imagePrompt: String = AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION

    fun configurePersonalApi(config: PersonalAiConfig?) {
        configurePersonalApis(listOfNotNull(config))
    }

    fun configurePersonalApis(configs: List<PersonalAiConfig>) {
        personalConfigs = configs.distinctBy { it.credentialId }
        _lastError.value = null
    }

    fun configureWordDetailsPrompt(prompt: String) {
        wordDetailsPrompt = prompt.trim().take(4_000).ifBlank { AiPromptDefaults.WORD_DETAILS }
    }

    fun configureImagePrompt(prompt: String) {
        imagePrompt = prompt.trim().take(4_000)
            .ifBlank { AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION }
    }

    suspend fun analyzeImages(
        images: List<DirectAiService.ImagePayload>
    ): List<OcrCardCandidate> {
        if (personalConfigs.isEmpty()) error("請先在設定中加入至少一組 AI API Key")
        return runCatching {
            withPersonalFailover { config ->
                DirectAiService.analyzeImages(config, images, imagePrompt)
            }
        }
            .onSuccess { _lastError.value = null }
            .onFailure { _lastError.value = it.message ?: "多模態圖片辨識失敗" }
            .getOrThrow()
    }

    private suspend fun post(path: String, payload: JSONObject): Any? = withContext(Dispatchers.IO) {
        val url = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}$path"
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(responseText).optString("error")
                    }.getOrNull().orEmpty().ifBlank { "AI 服務暫時無法使用 (${response.code})" }
                    _lastError.value = message
                    Log.w(TAG, "Backend request failed with HTTP ${response.code}")
                    return@withContext null
                }
                val root = JSONObject(responseText)
                _lastError.value = null
                root.opt("data")
            }
        } catch (error: Exception) {
            _lastError.value = "無法連線至 AI 服務，已改用離線資料"
            Log.w(TAG, "Backend request unavailable", error)
            null
        }
    }

    suspend fun fetchWordDetails(word: String): DictionaryEntry? {
        val normalized = word.trim()
        if (normalized.isBlank() || normalized.length > 100) return null
        if (personalConfigs.isNotEmpty()) {
            return runCatching {
                withPersonalFailover { config ->
                    DirectAiService.fetchWordDetails(config, normalized, wordDetailsPrompt)
                        ?: error("AI 未回傳完整的 KK 音標與單字資料")
                }
            }
                .onFailure { _lastError.value = it.message ?: "個人 AI API 呼叫失敗" }
                .getOrNull()
                .also { if (it != null) _lastError.value = null }
        }
        val data = post(
            "/api/v1/vocabulary/details",
            JSONObject()
                .put("word", normalized)
                .put("instructions", wordDetailsPrompt)
        ) as? JSONObject ?: return null

        return data.toDictionaryEntry(normalized)
    }

    suspend fun batchAnalyzeOcrText(rawText: String): List<OcrCardCandidate> {
        val text = rawText.trim().take(MAX_DOCUMENT_CHARACTERS)
        if (text.isBlank()) return emptyList()
        if (personalConfigs.isNotEmpty()) {
            return runCatching {
                withPersonalFailover { config -> DirectAiService.analyzeText(config, text) }
            }
                .onFailure { _lastError.value = it.message ?: "個人 AI API 呼叫失敗" }
                .getOrDefault(emptyList())
                .also { result ->
                    if (result.isNotEmpty()) _lastError.value = null
                }
        }
        val data = post(
            "/api/v1/vocabulary/analyze",
            JSONObject().put("text", text)
        ) as? JSONArray ?: return emptyList()

        return data.toCandidates()
    }

    suspend fun batchEnrichCandidates(candidates: List<OcrCardCandidate>): List<OcrCardCandidate> {
        if (candidates.isEmpty()) return candidates
        if (personalConfigs.isNotEmpty()) {
            return runCatching {
                withPersonalFailover { config -> DirectAiService.enrich(config, candidates) }
            }
                .onSuccess { _lastError.value = null }
                .onFailure { _lastError.value = it.message ?: "個人 AI API 呼叫失敗" }
                .getOrDefault(candidates)
        }
        val words = candidates
            .map { it.word.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MAX_BATCH_WORDS)
        if (words.isEmpty()) return candidates

        val data = post(
            "/api/v1/vocabulary/enrich",
            JSONObject().put("words", JSONArray(words))
        ) as? JSONArray ?: return candidates

        val enrichedByWord = data.toCandidates().associateBy { it.word.lowercase() }
        return candidates.map { candidate ->
            val enriched = enrichedByWord[candidate.word.trim().lowercase()] ?: return@map candidate
            candidate.copy(
                phonetic = candidate.phonetic.ifBlank { enriched.phonetic },
                partOfSpeech = candidate.partOfSpeech.ifBlank { enriched.partOfSpeech },
                definition = if (
                    candidate.definition.isBlank() || candidate.definition.contains("待補充")
                ) enriched.definition else candidate.definition,
                exampleSentence = candidate.exampleSentence.ifBlank { enriched.exampleSentence },
                exampleTranslation = candidate.exampleTranslation.ifBlank {
                    enriched.exampleTranslation
                }
            )
        }
    }

    suspend fun formatImportedCards(
        candidates: List<com.example.data.importer.ExternalCardCandidate>,
        customInstructions: String
    ): Map<Int, DictionaryEntry> {
        if (personalConfigs.isEmpty()) error("請先在設定中加入至少一組 AI API Key")
        return withPersonalFailover { config ->
            DirectAiService.formatImportedCards(config, candidates, customInstructions)
        }
    }

    suspend fun runAssistant(
        prompt: String,
        images: List<DirectAiService.ImagePayload> = emptyList()
    ): JSONObject {
        if (personalConfigs.isEmpty()) error("請先在設定中加入至少一組 AI API Key")
        return runCatching {
            withPersonalFailover { config ->
                DirectAiService.generateAssistantJson(config, prompt, images)
            }
        }
            .onSuccess { _lastError.value = null }
            .onFailure { _lastError.value = it.message ?: "AI 助手暫時無法使用" }
            .getOrThrow()
    }

    private suspend fun <T> withPersonalFailover(
        request: suspend (PersonalAiConfig) -> T
    ): T {
        var lastError: Throwable? = null
        val available = personalConfigs
            .groupBy { it.provider }
            .flatMap { (provider, configs) ->
                if (configs.size < 2) configs else {
                    val start = providerRotation
                        .getOrPut(provider) { AtomicInteger(0) }
                        .getAndIncrement().mod(configs.size)
                    configs.drop(start) + configs.take(start)
                }
            }
        available.forEach { config ->
            try {
                return request(config)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: error("沒有可用的個人 AI API")
    }

    private fun JSONObject.toDictionaryEntry(fallbackWord: String): DictionaryEntry? {
        val returnedWord = optString("word").trim()
        val definition = optString("definition").trim()
        val example = optString("exampleSentence").trim()
        val partOfSpeech = optString("partOfSpeech").trim()
        val translation = optString("exampleTranslation").trim()
        if (
            returnedWord.isBlank() ||
            !returnedWord.equals(fallbackWord, ignoreCase = true) ||
            definition.isBlank() ||
            partOfSpeech.isBlank() ||
            example.isBlank() ||
            translation.isBlank()
        ) {
            _lastError.value = "AI 回傳內容未通過品質驗證，未套用任何資料"
            return null
        }
        return DictionaryEntry(
            word = returnedWord,
            phonetic = optString("phonetic").trim(),
            partOfSpeech = partOfSpeech,
            definition = definition,
            exampleSentence = example,
            exampleTranslation = translation,
            category = "AI 補充"
        )
    }

    private fun JSONArray.toCandidates(): List<OcrCardCandidate> = buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val word = item.optString("word").trim()
            if (word.isBlank()) continue
            add(
                OcrCardCandidate(
                    word = word,
                    phonetic = item.optString("phonetic").trim(),
                    partOfSpeech = item.optString("partOfSpeech").trim(),
                    definition = item.optString("definition").trim(),
                    exampleSentence = item.optString("exampleSentence").trim(),
                    exampleTranslation = item.optString("exampleTranslation").trim(),
                    isSelected = true
                )
            )
        }
    }
}
