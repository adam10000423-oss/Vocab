package com.example.data.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Reads the structured response used by the public Google Translate web client.
 * This is intentionally isolated because it is not a documented Google API and
 * can change independently of the app.
 */
object GoogleTranslateLookupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(rawWord: String): DictionaryEntry = withContext(Dispatchers.IO) {
        val word = rawWord.trim().lowercase()
        require(word.matches(Regex("[a-z][a-z' -]{0,79}"))) { "請輸入英文單字或短語" }
        val root = request(word, includeDictionary = true)
        parse(word, root)
    }

    internal fun parse(requestedWord: String, root: JSONArray): DictionaryEntry {
        val translationRows = root.optJSONArray(0) ?: error("Google 翻譯沒有回傳翻譯結果")
        val firstTranslation = translationRows.optJSONArray(0)
        val translated = firstTranslation?.optString(0).orEmpty().trim()
        val returnedWord = firstTranslation?.optString(1).orEmpty().trim().ifBlank { requestedWord }
        val pronunciation = translationRows.optJSONArray(1)?.optString(3).orEmpty().trim()

        val parts = mutableListOf<String>()
        val definitions = mutableListOf<String>()
        root.optJSONArray(1)?.let { dictionary ->
            for (index in 0 until dictionary.length()) {
                val row = dictionary.optJSONArray(index) ?: continue
                val meanings = row.optJSONArray(1)?.strings().orEmpty()
                    .map(String::trim).filter(String::isNotBlank).distinct().take(6)
                if (meanings.isEmpty()) continue
                parts += normalizePartOfSpeech(row.optString(0))
                definitions += meanings.joinToString("；")
            }
        }
        val example = findFirstExample(root)?.replace(Regex("<[^>]+>"), "")?.trim().orEmpty()
        return DictionaryEntry(
            word = returnedWord,
            phonetic = pronunciation.takeIf(String::isNotBlank)?.let { "/$it/" }.orEmpty(),
            partOfSpeech = parts.filter(String::isNotBlank).distinct().joinToString("/"),
            definition = definitions.joinToString("／").ifBlank { translated },
            exampleSentence = example,
            exampleTranslation = "",
            category = "Google 翻譯"
        )
    }

    suspend fun translateExample(entry: DictionaryEntry): DictionaryEntry = withContext(Dispatchers.IO) {
        if (entry.exampleSentence.isBlank()) return@withContext entry
        val root = request(entry.exampleSentence, includeDictionary = false)
        val rows = root.optJSONArray(0)
        val translation = buildString {
            if (rows != null) for (index in 0 until rows.length()) {
                append(rows.optJSONArray(index)?.optString(0).orEmpty())
            }
        }.trim()
        entry.copy(exampleTranslation = translation)
    }

    private fun request(text: String, includeDictionary: Boolean): JSONArray {
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", "en")
            .addQueryParameter("tl", "zh-TW")
            .addQueryParameter("dt", "t")
            .addQueryParameter("dt", "rm")
            .apply {
                if (includeDictionary) {
                    addQueryParameter("dt", "bd")
                    addQueryParameter("dt", "ex")
                }
            }
            .addQueryParameter("q", text)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) Vocab/2")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Google 翻譯暫時無法查詢（${response.code}）" }
            return JSONArray(response.body?.string().orEmpty())
        }
    }

    private fun normalizePartOfSpeech(value: String): String = when (value.lowercase()) {
        "noun" -> "n."
        "verb" -> "v."
        "adjective" -> "adj."
        "adverb" -> "adv."
        "pronoun" -> "pron."
        "preposition" -> "prep."
        "conjunction" -> "conj."
        "interjection" -> "interj."
        else -> value.trim()
    }

    private fun JSONArray.strings(): List<String> =
        (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private fun findFirstExample(value: Any?): String? = when (value) {
        is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { findFirstExample(value.opt(it)) }
        is String -> value.takeIf { it.contains("<b>", ignoreCase = true) && it.contains("</b>", ignoreCase = true) }
        else -> null
    }
}
