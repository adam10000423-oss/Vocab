package com.example.data.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Key-free Wikimedia fallback using the official page-summary endpoint. */
object WiktionaryLookupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(18, TimeUnit.SECONDS).build()

    suspend fun lookup(rawWord: String): DictionaryEntry = withContext(Dispatchers.IO) {
        val word = rawWord.trim().lowercase()
        require(word.matches(Regex("[a-z][a-z' -]{0,79}"))) { "維基詞典僅支援英文單字或短語" }
        val encoded = URLEncoder.encode(word, "UTF-8").replace("+", "%20")
        val request = Request.Builder()
            .url("https://en.wiktionary.org/api/rest_v1/page/definition/$encoded")
            .header("User-Agent", "Vocab/2.8.0 (https://github.com/adam10000423-oss/Vocab)")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                if (response.code == 404) "維基詞典找不到這個單字" else "維基詞典暫時無法查詢（${response.code}）"
            }
            val json = JSONObject(response.body?.string().orEmpty())
            val entries = json.optJSONArray("en") ?: error("維基詞典沒有英文詞條")
            val parts = mutableListOf<String>()
            val definitions = mutableListOf<String>()
            var example = ""
            for (entryIndex in 0 until entries.length()) {
                val entry = entries.optJSONObject(entryIndex) ?: continue
                entry.optString("partOfSpeech").takeIf(String::isNotBlank)?.let(parts::add)
                val rows = entry.optJSONArray("definitions") ?: continue
                for (rowIndex in 0 until rows.length()) {
                    val row = rows.optJSONObject(rowIndex) ?: continue
                    row.optString("definition").replace(Regex("<[^>]+>"), " ")
                        .replace(Regex("\\s+"), " ").trim().takeIf(String::isNotBlank)?.let(definitions::add)
                    if (example.isBlank()) {
                        val examples = row.optJSONArray("parsedExamples")
                        example = examples?.optString(0).orEmpty().replace(Regex("<[^>]+>"), " ").trim()
                    }
                }
            }
            check(definitions.isNotEmpty()) { "維基詞典沒有可顯示的解釋" }
            DictionaryEntry(
                word = word,
                phonetic = "",
                partOfSpeech = parts.distinct().joinToString("/"),
                definition = definitions.distinct().take(6).joinToString("；").take(1600),
                exampleSentence = example,
                exampleTranslation = "",
                category = "Wiktionary"
            )
        }
    }
}
