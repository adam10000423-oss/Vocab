package com.example.data.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/** Stable, key-free fallback backed by dictionaryapi.dev. */
object FreeDictionaryLookupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(rawWord: String): DictionaryEntry = withContext(Dispatchers.IO) {
        val word = rawWord.trim().lowercase()
        require(word.matches(Regex("[a-z][a-z' -]{0,79}"))) { "請輸入英文單字或短語" }
        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/${java.net.URLEncoder.encode(word, "UTF-8")}".toHttpUrl()
        val request = Request.Builder().url(url).header("User-Agent", "Vocab/2.8.0 Android").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                if (response.code == 404) "免費英文字典找不到這個單字" else "免費英文字典暫時無法查詢（${response.code}）"
            }
            parse(word, JSONArray(response.body?.string().orEmpty()))
        }
    }

    internal fun parse(requestedWord: String, root: JSONArray): DictionaryEntry {
        val entry = root.optJSONObject(0) ?: error("免費英文字典沒有回傳結果")
        val phonetics = entry.optJSONArray("phonetics")
        val phonetic = entry.optString("phonetic").ifBlank {
            (0 until (phonetics?.length() ?: 0)).firstNotNullOfOrNull { index ->
                phonetics?.optJSONObject(index)?.optString("text")?.takeIf(String::isNotBlank)
            }.orEmpty()
        }
        val meanings = entry.optJSONArray("meanings")
        val parts = mutableListOf<String>()
        val definitions = mutableListOf<String>()
        var example = ""
        for (index in 0 until (meanings?.length() ?: 0)) {
            val meaning = meanings?.optJSONObject(index) ?: continue
            meaning.optString("partOfSpeech").takeIf(String::isNotBlank)?.let(parts::add)
            val rows = meaning.optJSONArray("definitions")
            for (rowIndex in 0 until minOf(rows?.length() ?: 0, 3)) {
                val row = rows?.optJSONObject(rowIndex) ?: continue
                row.optString("definition").takeIf(String::isNotBlank)?.let(definitions::add)
                if (example.isBlank()) example = row.optString("example")
            }
        }
        check(definitions.isNotEmpty()) { "免費英文字典沒有可顯示的解釋" }
        return DictionaryEntry(
            word = entry.optString("word").ifBlank { requestedWord },
            phonetic = phonetic.takeIf(String::isNotBlank)?.let { if (it.startsWith('/')) it else "/$it/" }.orEmpty(),
            partOfSpeech = parts.distinct().joinToString("/"),
            definition = definitions.distinct().take(6).joinToString("；"),
            exampleSentence = example,
            exampleTranslation = "",
            category = "Free Dictionary"
        )
    }
}
