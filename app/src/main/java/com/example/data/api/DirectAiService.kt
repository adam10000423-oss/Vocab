package com.example.data.api

import com.example.data.dictionary.DictionaryEntry
import com.example.data.importer.ExternalCardCandidate
import com.example.util.OcrCardCandidate
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DirectAiService {
    private const val SYSTEM_PROMPT =
        "You are a careful English lexicographer for Traditional Chinese learners. " +
            "Return JSON only. Never invent a word or silently substitute another word."

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    data class AiUsageSnapshot(
        val label: String,
        val isLimited: Boolean = false
    )

    class AiQuotaException(message: String) : IllegalStateException(message)

    private val _usage = MutableStateFlow<Map<AiProvider, AiUsageSnapshot>>(emptyMap())
    val usage: StateFlow<Map<AiProvider, AiUsageSnapshot>> = _usage.asStateFlow()

    data class ImagePayload(
        val bytes: ByteArray,
        val mimeType: String
    )

    suspend fun fetchWordDetails(
        config: PersonalAiConfig,
        word: String,
        customInstructions: String = AiPromptDefaults.WORD_DETAILS
    ): DictionaryEntry? {
        val normalized = word.trim().take(100)
        if (normalized.isBlank()) return null
        val prompt = """
            ${customInstructions.trim().take(4_000)}

            Look up the exact English word "$normalized".
            Return one JSON object with exactly these string fields:
            word, phonetic, partOfSpeech, definition, exampleSentence, exampleTranslation.
            definition and exampleTranslation must be Traditional Chinese.
            exampleSentence must be a natural English sentence.
        """.trimIndent()
        val root = generateJson(config, prompt) as? JSONObject ?: return null
        val returnedWord = root.optString("word").trim()
        val definition = root.optString("definition").trim()
        val example = root.optString("exampleSentence").trim()
        val partOfSpeech = root.optString("partOfSpeech").trim()
        val translation = root.optString("exampleTranslation").trim()
        val phonetic = root.optString("phonetic").trim()
        if (
            !returnedWord.equals(normalized, ignoreCase = true) ||
            phonetic.isBlank() ||
            definition.isBlank() ||
            partOfSpeech.isBlank() ||
            example.isBlank() ||
            translation.isBlank()
        ) {
            return null
        }
        return DictionaryEntry(
            word = returnedWord,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            definition = definition,
            exampleSentence = example,
            exampleTranslation = translation,
            category = "AI 補充"
        )
    }

    suspend fun formatImportedCards(
        config: PersonalAiConfig,
        candidates: List<ExternalCardCandidate>,
        customInstructions: String = AiPromptDefaults.WORD_DETAILS
    ): Map<Int, DictionaryEntry> {
        val results = linkedMapOf<Int, DictionaryEntry>()
        candidates.withIndex().chunked(10).forEach batch@ { chunk ->
            val sourceCards = JSONArray().apply {
                chunk.forEach { indexed ->
                    put(
                        JSONObject()
                            .put("inputIndex", indexed.index)
                            .put("front", indexed.value.word.trim().take(500))
                            .put("back", indexed.value.definition.trim().take(3_000))
                    )
                }
            }
            val prompt = """
                ${customInstructions.trim().take(4_000)}

                Convert the following imported flashcards into this app's structured vocabulary format.
                The Quizlet front is the exact word and the Quizlet back is the authoritative source.
                Preserve the exact front word and all useful meanings from the back.
                Do not add, remove, merge, reorder, translate, or substitute input cards.
                If the back contains an English example and Traditional Chinese translation, extract them.
                If an example is missing, create one natural English example and its faithful Traditional Chinese translation.

                Return exactly one JSON object:
                {"cards":[{"inputIndex":0,"word":"","phonetic":"","partOfSpeech":"","definition":"","exampleSentence":"","exampleTranslation":""}]}

                Requirements:
                - one output for every inputIndex
                - word must exactly match its front
                - partOfSpeech and definition must not be blank
                - definition and exampleTranslation must be Traditional Chinese
                - exampleSentence must be English
                - do not include markdown or commentary

                INPUT_CARDS:
                $sourceCards
            """.trimIndent()
            val root = try {
                generateJson(config, prompt) as? JSONObject
            } catch (error: AiQuotaException) {
                throw error
            } catch (_: Exception) {
                null
            } ?: return@batch
            val items = root.optJSONArray("cards") ?: return@batch
            for (itemIndex in 0 until items.length()) {
                val item = items.optJSONObject(itemIndex) ?: continue
                val inputIndex = item.optInt("inputIndex", -1)
                val original = candidates.getOrNull(inputIndex) ?: continue
                val word = item.optString("word").trim()
                val partOfSpeech = item.optString("partOfSpeech").trim()
                val definition = item.optString("definition").trim()
                val exampleSentence = item.optString("exampleSentence").trim()
                val exampleTranslation = item.optString("exampleTranslation").trim()
                val hasChineseDefinition = definition.any { it.code in 0x3400..0x9FFF }
                val hasEnglishExample = exampleSentence.any { it in 'A'..'Z' || it in 'a'..'z' }
                val hasChineseTranslation = exampleTranslation.any { it.code in 0x3400..0x9FFF }
                if (
                    !word.equals(original.word.trim(), ignoreCase = false) ||
                    partOfSpeech.isBlank() ||
                    definition.isBlank() ||
                    exampleSentence.isBlank() ||
                    exampleTranslation.isBlank() ||
                    !hasChineseDefinition ||
                    !hasEnglishExample ||
                    !hasChineseTranslation
                ) {
                    continue
                }
                results[inputIndex] = DictionaryEntry(
                    word = word,
                    phonetic = item.optString("phonetic").trim(),
                    partOfSpeech = partOfSpeech,
                    definition = definition,
                    exampleSentence = exampleSentence,
                    exampleTranslation = exampleTranslation,
                    category = "AI 匯入整理"
                )
            }
        }
        return results
    }

    suspend fun analyzeText(config: PersonalAiConfig, text: String): List<OcrCardCandidate> {
        val normalized = text.trim().take(20_000)
        if (normalized.isBlank()) return emptyList()
        val prompt = """
            Extract at most 80 useful English vocabulary words from the text below.
            Return a JSON array. Every element must contain string fields:
            word, phonetic, partOfSpeech, definition, exampleSentence.
            Use Traditional Chinese for definition and retain only real English words.

            TEXT:
            $normalized
        """.trimIndent()
        return (generateJson(config, prompt) as? JSONArray).toCandidates()
    }

    suspend fun analyzeImages(
        config: PersonalAiConfig,
        images: List<ImagePayload>,
        customInstructions: String = AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION
    ): List<OcrCardCandidate> {
        val accepted = images
            .take(10)
            .filter {
                it.bytes.isNotEmpty() &&
                    it.bytes.size <= 8 * 1024 * 1024 &&
                    it.mimeType in setOf("image/jpeg", "image/png", "image/webp", "image/gif")
            }
        require(accepted.isNotEmpty()) { "沒有可傳送給 AI 的圖片" }

        val prompt = """
            ${customInstructions.trim().take(4_000)}

            依照圖片提供的原始順序輸出 cards。若圖片沒有可辨識的英文單字，回傳 {"cards":[]}。
        """.trimIndent()
        val root = generateJson(config, prompt, accepted) as? JSONObject
            ?: error("AI 沒有回傳正確的 JSON 物件")
        return root.optJSONArray("cards")
            .toCandidates()
            .filter { candidate ->
                candidate.word.matches(Regex("^[A-Za-z][A-Za-z' -]{1,99}$"))
            }
            .distinctBy { it.word.trim().lowercase() }
            .take(200)
    }

    suspend fun enrich(config: PersonalAiConfig, candidates: List<OcrCardCandidate>): List<OcrCardCandidate> {
        if (candidates.isEmpty()) return candidates
        val words = candidates.map { it.word.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(200)
        val enriched = linkedMapOf<String, OcrCardCandidate>()
        words.chunked(12).forEach { wordBatch ->
            val prompt = """
                Provide reliable dictionary data for exactly these English words:
                ${wordBatch.joinToString(", ")}

                Return exactly one JSON object:
                {"cards":[{"word":"","phonetic":"","partOfSpeech":"","definition":"","exampleSentence":"","exampleTranslation":""}]}

                Requirements:
                - Return at most one card for each requested word and no other words.
                - Preserve the exact requested spelling.
                - definition and exampleTranslation use Traditional Chinese.
                - exampleSentence is a complete natural English sentence.
                - phonetic is required and must use reliable American KK notation wrapped in /.../.
                - Never substitute another word.
                - Do not include Markdown or commentary.
            """.trimIndent()
            val payload = generateJson(config, prompt)
            val items = when (payload) {
                is JSONObject -> payload.optJSONArray("cards")
                is JSONArray -> payload
                else -> null
            }
            val requested = wordBatch.toSet()
            items.toCandidates()
                .filter { it.word in requested }
                .forEach { enriched[it.word.lowercase()] = it }
        }
        return candidates.map { original ->
            val item = enriched[original.word.trim().lowercase()] ?: return@map original
            original.copy(
                phonetic = original.phonetic.ifBlank { item.phonetic },
                partOfSpeech = original.partOfSpeech.ifBlank { item.partOfSpeech },
                definition = original.definition.takeUnless {
                    it.isBlank() || it.contains("待補充")
                } ?: item.definition,
                exampleSentence = original.exampleSentence.ifBlank { item.exampleSentence },
                exampleTranslation = original.exampleTranslation.ifBlank {
                    item.exampleTranslation
                }
            )
        }
    }

    suspend fun listModels(config: PersonalAiConfig): List<String> = withContext(Dispatchers.IO) {
        val request = when (config.provider) {
            AiProvider.GEMINI -> Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=${config.apiKey}")
                .get().build()
            AiProvider.OPENAI -> Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer ${config.apiKey}").get().build()
            AiProvider.ANTHROPIC -> Request.Builder()
                .url("https://api.anthropic.com/v1/models")
                .header("x-api-key", config.apiKey)
                .header("anthropic-version", "2023-06-01").get().build()
            AiProvider.OPENROUTER -> Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .header("Authorization", "Bearer ${config.apiKey}").get().build()
        }
        execute(request, config.provider).let { body ->
            val root = JSONObject(body)
            when (config.provider) {
                AiProvider.GEMINI -> root.optJSONArray("models").strings("name")
                    .map { it.removePrefix("models/") }
                else -> root.optJSONArray("data").strings("id")
            }
                .filterNot { it.contains("embedding", true) || it.contains("image", true) }
                .distinct()
                .sorted()
                .take(100)
        }
    }

    suspend fun test(config: PersonalAiConfig): Result<Unit> = runCatching {
        check(fetchWordDetails(config, "apple") != null) {
            "API 已回應，但選定模型沒有回傳合格的字典資料"
        }
        analyzeImages(
            config = config,
            images = listOf(
                ImagePayload(
                    bytes = Base64.decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
                        Base64.DEFAULT
                    ),
                    mimeType = "image/png"
                )
            ),
            customInstructions = "這是多模態連線測試。圖片若沒有英文單字，必須回傳 {\"cards\":[]}。"
        )
    }

    suspend fun refreshUsage(config: PersonalAiConfig): AiUsageSnapshot =
        withContext(Dispatchers.IO) {
            if (config.provider != AiProvider.OPENROUTER) {
                return@withContext setUsage(
                    config.provider,
                    AiUsageSnapshot(
                        when (config.provider) {
                            AiProvider.GEMINI -> "Google 未提供 Key 剩餘總額；請至 AI Studio 查看"
                            AiProvider.OPENAI -> "OpenAI 未提供 Key 剩餘總額；請至用量頁查看"
                            AiProvider.ANTHROPIC -> "Anthropic 未提供 Key 剩餘總額；請至 Console 查看"
                            AiProvider.OPENROUTER -> error("unreachable")
                        }
                    )
                )
            }
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/credits")
                .header("Authorization", "Bearer ${config.apiKey}")
                .get()
                .build()
            runCatching {
                val root = JSONObject(execute(request, config.provider))
                val data = root.getJSONObject("data")
                val remaining = (
                    data.optDouble("total_credits", 0.0) -
                        data.optDouble("total_usage", 0.0)
                    ).coerceAtLeast(0.0)
                setUsage(
                    config.provider,
                    AiUsageSnapshot(
                        label = "剩餘 US$ %.4f".format(remaining),
                        isLimited = remaining <= 0.0
                    )
                )
            }.getOrElse {
                setUsage(
                    config.provider,
                    AiUsageSnapshot("此 Key 無法直接查詢餘額；請至 OpenRouter 查看")
                )
            }
        }

    suspend fun generateAssistantJson(
        config: PersonalAiConfig,
        prompt: String,
        images: List<ImagePayload> = emptyList()
    ): JSONObject {
        require(prompt.isNotBlank()) { "AI 助手指令不能空白" }
        val boundedPrompt = if (prompt.length <= 60_000) {
            prompt
        } else {
            buildString(60_000) {
                append(prompt.take(38_000))
                append("\n\n［中間過長的 App 卡片內容已省略］\n\n")
                append(prompt.takeLast(21_900))
            }
        }
        return generateJson(config, boundedPrompt, images.take(5)) as? JSONObject
            ?: error("AI 助手沒有回傳正確的 JSON 物件")
    }

    private suspend fun generateJson(
        config: PersonalAiConfig,
        userPrompt: String,
        images: List<ImagePayload> = emptyList()
    ): Any? =
        withContext(Dispatchers.IO) {
            val response = when (config.provider) {
                AiProvider.GEMINI -> gemini(config, userPrompt, images)
                AiProvider.OPENAI -> openAi(config, userPrompt, images)
                AiProvider.ANTHROPIC -> anthropic(config, userPrompt, images)
                AiProvider.OPENROUTER -> openRouter(config, userPrompt, images)
            }
            parseJsonPayload(response)
        }

    private fun gemini(
        config: PersonalAiConfig,
        prompt: String,
        images: List<ImagePayload>
    ): String {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        images.forEach { image ->
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", image.mimeType)
                        .put("data", image.base64())
                )
            )
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))
            ))
            .put("contents", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 4096)
            )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=${config.apiKey}")
            .post(jsonBody(body))
            .build()
        val root = JSONObject(execute(request, config.provider))
        return root.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun openAi(
        config: PersonalAiConfig,
        prompt: String,
        images: List<ImagePayload>
    ): String {
        val userContent = JSONArray()
            .put(JSONObject().put("type", "input_text").put("text", prompt))
        images.forEach { image ->
            userContent.put(
                JSONObject()
                    .put("type", "input_image")
                    .put("detail", "high")
                    .put("image_url", image.dataUrl())
            )
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("store", false)
            .put("max_output_tokens", 4096)
            .put("input", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", userContent)))
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(jsonBody(body)).build()
        val root = JSONObject(execute(request, config.provider))
        root.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }
        val output = root.getJSONArray("output")
        for (i in 0 until output.length()) {
            val content = output.getJSONObject(i).optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                content.getJSONObject(j).optString("text").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        error("OpenAI 未回傳文字")
    }

    private fun anthropic(
        config: PersonalAiConfig,
        prompt: String,
        images: List<ImagePayload>
    ): String {
        val content = JSONArray()
        images.forEach { image ->
            content.put(
                JSONObject()
                    .put("type", "image")
                    .put(
                        "source",
                        JSONObject()
                            .put("type", "base64")
                            .put("media_type", image.mimeType)
                            .put("data", image.base64())
                    )
            )
        }
        content.put(JSONObject().put("type", "text").put("text", prompt))
        val body = JSONObject()
            .put("model", config.model)
            .put("max_tokens", 4096)
            .put("system", SYSTEM_PROMPT)
            .put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", content)
            ))
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(jsonBody(body)).build()
        return JSONObject(execute(request, config.provider)).getJSONArray("content")
            .getJSONObject(0).getString("text")
    }

    private fun openRouter(
        config: PersonalAiConfig,
        prompt: String,
        images: List<ImagePayload>
    ): String {
        val userContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
        images.forEach { image ->
            userContent.put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", image.dataUrl()))
            )
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", userContent)))
            .put("temperature", 0.2)
            .put("max_tokens", 4096)
            .put("response_format", JSONObject().put("type", "json_object"))
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("X-Title", "Vocab")
            .post(jsonBody(body)).build()
        return JSONObject(execute(request, config.provider)).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }

    private fun execute(
        request: Request,
        provider: AiProvider
    ): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val detail = runCatching {
                val error = JSONObject(body).opt("error")
                when (error) {
                    is JSONObject -> error.optString("message")
                    else -> error?.toString().orEmpty()
                }
            }.getOrDefault("")
            val message = detail.ifBlank { "AI API 連線失敗（HTTP ${response.code}）" }
            if (response.code == 402 || response.code == 429) {
                setUsage(
                    provider,
                    AiUsageSnapshot("已達額度或速率限制，將切換下一個 API", true)
                )
                throw AiQuotaException(message)
            }
            error(message)
        }
        updateRateLimitUsage(provider, response.headers)
        if (body.isBlank()) {
            error("AI API 回傳空白內容，請確認所選模型可用並且帳戶仍有額度")
        }
        body
    }

    private fun updateRateLimitUsage(provider: AiProvider, headers: okhttp3.Headers) {
        val label = when (provider) {
            AiProvider.OPENAI -> {
                val requests = headers["x-ratelimit-remaining-requests"]
                val tokens = headers["x-ratelimit-remaining-tokens"]
                if (requests != null || tokens != null) {
                    "本時段剩餘：${requests ?: "—"} 次請求 · ${tokens ?: "—"} tokens"
                } else null
            }
            AiProvider.ANTHROPIC -> {
                val requests = headers["anthropic-ratelimit-requests-remaining"]
                val input = headers["anthropic-ratelimit-input-tokens-remaining"]
                val output = headers["anthropic-ratelimit-output-tokens-remaining"]
                if (requests != null || input != null || output != null) {
                    "本時段剩餘：${requests ?: "—"} 次 · 輸入 ${input ?: "—"} · 輸出 ${output ?: "—"}"
                } else null
            }
            else -> null
        }
        if (label != null) setUsage(provider, AiUsageSnapshot(label))
    }

    private fun setUsage(
        provider: AiProvider,
        snapshot: AiUsageSnapshot
    ): AiUsageSnapshot {
        _usage.value = _usage.value + (provider to snapshot)
        return snapshot
    }

    private fun jsonBody(value: JSONObject) =
        value.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    private fun ImagePayload.base64(): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun ImagePayload.dataUrl(): String =
        "data:$mimeType;base64,${base64()}"

    private fun parseJsonPayload(raw: String): Any? {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val objectStart = cleaned.indexOf('{')
        val arrayStart = cleaned.indexOf('[')
        val start = when {
            arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart) -> arrayStart
            objectStart >= 0 -> objectStart
            else -> throw IllegalArgumentException("AI 沒有回傳 JSON 格式資料")
        }
        val completeJson = findCompleteJson(cleaned, start)
            ?: throw IllegalArgumentException(
                "AI 回傳內容不完整，可能超過模型輸出上限；請縮小批次或更換模型後再試"
            )
        return try {
            if (completeJson.first() == '[') JSONArray(completeJson)
            else JSONObject(completeJson)
        } catch (_: JSONException) {
            throw IllegalArgumentException("AI 回傳的 JSON 格式錯誤，未套用任何資料")
        }
    }

    internal fun findCompleteJson(text: String, start: Int): String? {
        val opening = text.getOrNull(start) ?: return null
        val closing = if (opening == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val character = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                opening -> depth++
                closing -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun JSONArray?.strings(key: String): List<String> = buildList {
        val array = this@strings ?: return@buildList
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.optString(key)?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    private fun JSONArray?.toCandidates(): List<OcrCardCandidate> = buildList {
        val array = this@toCandidates ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val word = item.optString("word").trim()
            if (word.isBlank()) continue
            add(OcrCardCandidate(
                word = word,
                phonetic = item.optString("phonetic").trim(),
                // Missing AI data stays missing. Defaulting every unknown word to
                // noun would present guessed information as verified content.
                partOfSpeech = item.optString("partOfSpeech").trim(),
                definition = item.optString("definition").trim(),
                exampleSentence = item.optString("exampleSentence").trim(),
                exampleTranslation = item.optString("exampleTranslation").trim(),
                isSelected = true
            ))
        }
    }
}
