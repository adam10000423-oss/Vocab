package com.example.data.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Key-free Chinese-to-English fallback used when the primary translation endpoint is throttled. */
object MyMemoryTranslateLookupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun lookupChinese(rawText: String): DictionaryEntry = lookup(rawText, "zh-TW|en", true)

    suspend fun lookupEnglish(rawText: String): DictionaryEntry = lookup(rawText, "en|zh-TW", false)

    private suspend fun lookup(rawText: String, languagePair: String, chineseToEnglish: Boolean): DictionaryEntry = withContext(Dispatchers.IO) {
        val text = rawText.trim()
        require(text.isNotBlank() && text.length <= 80) { "請輸入要查詢的內容" }
        val url = "https://api.mymemory.translated.net/get".toHttpUrl().newBuilder()
            .addQueryParameter("q", text)
            .addQueryParameter("langpair", languagePair)
            .build()
        val request = Request.Builder().url(url).header("User-Agent", "Vocab/2.7.1 Android").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "備用翻譯暫時無法查詢（${response.code}）" }
            val root = JSONObject(response.body?.string().orEmpty())
            val english = root.optJSONObject("responseData")?.optString("translatedText").orEmpty().trim()
            check(english.isNotBlank() && !english.equals("null", true)) { "備用翻譯找不到對應英文" }
            DictionaryEntry(
                word = if (chineseToEnglish) english else text,
                phonetic = "",
                partOfSpeech = "",
                definition = if (chineseToEnglish) text else english,
                exampleSentence = "",
                exampleTranslation = "",
                category = "備用翻譯"
            )
        }
    }
}
