package com.example.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class ImportCapability {
    DIRECT_TEXT,
    ANKI_PACKAGE,
    PUBLIC_SHARE_PAGE,
    EXPORT_AND_PASTE,
    OFFICIAL_SEARCH_ONLY
}

data class ExternalVocabularySource(
    val id: String,
    val name: String,
    val hosts: List<String>,
    val searchUrl: String,
    val capability: ImportCapability,
    val guidance: String
) {
    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        return searchUrl.replace("{query}", encoded)
    }
}

data class ExternalCardCandidate(
    val word: String,
    val definition: String,
    val exampleSentence: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val exampleTranslation: String = "",
    val sourceName: String = "",
    val selected: Boolean = true,
    val aiFormatted: Boolean = false
)

data class ParsedExternalDeck(
    val title: String,
    val sourceName: String,
    val cards: List<ExternalCardCandidate>
)

data class ExternalSearchResult(
    val title: String,
    val url: String,
    val description: String = "",
    val sourceName: String
)

data class AiExternalFormattingResult(
    val cards: List<ExternalCardCandidate>,
    val formattedCount: Int,
    val fallbackCount: Int,
    val message: String = ""
)

object ExternalDeckImporter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val sources = listOf(
        ExternalVocabularySource(
            "anki", "Anki / AnkiWeb", listOf("ankiweb.net"),
            "https://ankiweb.net/shared/decks?search={query}",
            ImportCapability.ANKI_PACKAGE,
            "可搜尋公開牌組；從官方頁面下載 .apkg 後可直接匯入。AnkiWeb 下載需要瀏覽器簽章，分享頁不能由第三方 App 偷抓。"
        ),
        ExternalVocabularySource(
            "quizlet", "Quizlet", listOf("quizlet.com"),
            "https://quizlet.com/search?query={query}&type=sets",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可貼上公開學習集網址嘗試讀取。若 Quizlet 要求人機驗證，App 會如實顯示，請改用平台匯出文字。"
        ),
        ExternalVocabularySource(
            "kahoot", "Kahoot!", listOf("kahoot.com", "create.kahoot.it", "kahoot.it"),
            "https://create.kahoot.it/discover?query={query}",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可搜尋公開 Kahoot；只有公開頁實際提供題目與正確答案時才會匯入。"
        ),
        source("wordup", "WORD UP", listOf("wordup.com.tw"), "https://www.google.com/search?q=site%3Awordup.com.tw+{query}"),
        source("lingvist", "Lingvist", listOf("lingvist.com", "lingvist.io"), "https://lingvist.com/search/?q={query}"),
        source("vocabulazy", "Vocabulazy", listOf("vocabulazy.com"), "https://www.google.com/search?q=site%3Avocabulazy.com+{query}"),
        source("super_word_king", "超級單字王", emptyList(), "https://www.google.com/search?q=%E8%B6%85%E7%B4%9A%E5%96%AE%E5%AD%97%E7%8E%8B+{query}"),
        source("e4f", "E4F 字根字首字尾字典", listOf("e4f.com.tw"), "https://www.google.com/search?q=site%3Ae4f.com.tw+{query}"),
        source("english_word_king", "英文單字王", emptyList(), "https://www.google.com/search?q=%E8%8B%B1%E6%96%87%E5%96%AE%E5%AD%97%E7%8E%8B+{query}"),
        source("toast_toeic", "英文知識王－吐司多益英文", emptyList(), "https://www.google.com/search?q=%E5%90%90%E5%8F%B8%E5%A4%9A%E7%9B%8A%E8%8B%B1%E6%96%87+{query}"),
        source("word_chakra", "單字查克拉", emptyList(), "https://www.google.com/search?q=%E5%96%AE%E5%AD%97%E6%9F%A5%E5%85%8B%E6%8B%89+{query}"),
        source("cool_words", "英文單字酷", emptyList(), "https://www.google.com/search?q=%E8%8B%B1%E6%96%87%E5%96%AE%E5%AD%97%E9%85%B7+{query}"),
        source("word_tree", "單詞樹", emptyList(), "https://www.google.com/search?q=%E5%96%AE%E8%A9%9E%E6%A8%B9+{query}"),
        source("duolingo", "Duolingo 多鄰國", listOf("duolingo.com"), "https://www.google.com/search?q=site%3Aduolingo.com+{query}"),
        source("voicetube", "VoiceTube", listOf("voicetube.com"), "https://tw.voicetube.com/search?query={query}"),
        source("death_words", "死神單字", emptyList(), "https://www.google.com/search?q=%E6%AD%BB%E7%A5%9E%E5%96%AE%E5%AD%97+{query}"),
        source("chacha", "查查單字", emptyList(), "https://www.google.com/search?q=%E6%9F%A5%E6%9F%A5%E5%96%AE%E5%AD%97+{query}"),
        ExternalVocabularySource(
            "brainscape", "Brainscape", listOf("brainscape.com"),
            "https://www.brainscape.com/subjects/{query}",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可貼上公開 Flashcards 卡片頁網址，從網頁公開的題目與答案資料匯入。"
        ),
        ExternalVocabularySource(
            "knowt", "Knowt", listOf("knowt.com"),
            "https://knowt.com/search?search={query}",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可貼上公開 Flashcards 分享網址，從頁面公開資料讀取單字與解釋。"
        ),
        ExternalVocabularySource(
            "cram", "Cram", listOf("cram.com"),
            "https://www.cram.com/search?query={query}",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可貼上公開 Flashcards 分享網址，從頁面實際顯示的 Front／Back 匯入。"
        ),
        ExternalVocabularySource(
            "studysmarter", "StudySmarter", listOf("studysmarter.co.uk", "studysmarter.de"),
            "https://www.studysmarter.co.uk/search/?query={query}",
            ImportCapability.PUBLIC_SHARE_PAGE,
            "可搜尋並讀取公開 Flashcards 頁；需要登入或沒有公開正反面的內容不會匯入。"
        ),
        source("memrise", "Memrise", listOf("memrise.com"), "https://www.google.com/search?q=site%3Amemrise.com+{query}"),
        source("remnote", "RemNote", listOf("remnote.com"), "https://www.google.com/search?q=site%3Aremnote.com+{query}"),
        source("mochi", "Mochi Cards", listOf("mochi.cards"), "https://www.google.com/search?q=site%3Amochi.cards+{query}")
    )

    private fun source(id: String, name: String, hosts: List<String>, searchUrl: String) =
        ExternalVocabularySource(
            id, name, hosts, searchUrl, ImportCapability.OFFICIAL_SEARCH_ONLY,
            "此平台目前沒有公開且穩定的第三方牌組 API。可開啟官方搜尋，若平台提供 CSV／文字匯出，再回到本頁匯入。"
        )

    fun detectSource(url: String): ExternalVocabularySource? {
        val host = runCatching { Uri.parse(url.trim()).host?.lowercase().orEmpty() }.getOrDefault("")
        return sources.firstOrNull { source ->
            source.hosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }
        }
    }

    fun isDirectTextUrl(url: String): Boolean {
        val clean = url.substringBefore('?').lowercase()
        return clean.endsWith(".csv") || clean.endsWith(".tsv") ||
            clean.endsWith(".txt") || clean.endsWith(".json")
    }

    fun isDirectAnkiPackageUrl(url: String): Boolean =
        url.substringBefore('?').lowercase().endsWith(".apkg")

    suspend fun fetchDirectText(url: String): String = withContext(Dispatchers.IO) {
        val uri = requireSafeHttpsUrl(url)
        val request = Request.Builder()
            .url(uri.toString())
            .header("User-Agent", "Vocab/1.1")
            .build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "下載失敗（HTTP ${response.code}）" }
            val body = response.body ?: error("下載內容為空")
            require((body.contentLength().takeIf { it >= 0 } ?: 0) <= 5_000_000) { "檔案超過 5 MB" }
            body.string().take(5_000_000)
        }
    }

    suspend fun fetchSharedDeck(context: Context, url: String): ParsedExternalDeck {
        requireSafeHttpsUrl(url)
        val detected = detectSource(url)
        if (detected?.id == "quizlet") {
            return QuizletWebViewImporter.fetch(context, url)
        }
        if (detected != null && detected.hosts.isNotEmpty()) {
            return PublicDeckWebViewImporter.fetch(context, detected, url)
        }
        return withContext(Dispatchers.IO) {
        val endpoint = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/api/v1/import/url"
        val payload = JSONObject().put("url", url.trim())
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(responseText).optString("error")
                }.getOrNull().orEmpty().ifBlank {
                    "分享頁讀取失敗（HTTP ${response.code}）"
                }
                error(message)
            }
            val data = JSONObject(responseText).optJSONObject("data")
                ?: error("分享頁服務沒有回傳資料")
            val sourceName = data.optString("sourceName").trim().ifBlank { "公開分享頁" }
            val title = data.optString("title").trim().ifBlank { sourceName }
            val items = data.optJSONArray("cards") ?: JSONArray()
            val cards = buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val word = cleanCell(item.optString("word"))
                    val definition = cleanCell(item.optString("definition"))
                    val example = cleanCell(item.optString("exampleSentence"))
                    if (word.isNotBlank() && definition.isNotBlank()) {
                        add(
                            ExternalCardCandidate(
                                word = word,
                                definition = definition,
                                exampleSentence = example,
                                sourceName = sourceName
                            )
                        )
                    }
                }
            }.distinctBy { "${it.word.lowercase()}\u0000${it.definition.lowercase()}" }
            require(cards.isNotEmpty()) { "分享頁裡沒有可匯入的單字資料" }
            ParsedExternalDeck(title, sourceName, cards.take(5_000))
        }
    }
    }

    suspend fun searchPublicDecks(
        context: Context,
        source: ExternalVocabularySource,
        query: String
    ): List<ExternalSearchResult> {
        require(query.isNotBlank()) { "請輸入搜尋關鍵字" }
        return PublicDeckWebViewImporter.search(context, source, query.trim())
    }

    suspend fun parseUri(context: Context, uri: Uri): ParsedExternalDeck = withContext(Dispatchers.IO) {
        val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            .orEmpty()
        if (displayName.lowercase().endsWith(".apkg")) {
            return@withContext AnkiPackageParser.parse(context, uri)
        }
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("無法讀取檔案")
        parseText(text, displayName.ifBlank { "匯入檔案" })
    }

    fun parseText(text: String, title: String = "貼上的單字集"): ParsedExternalDeck {
        val trimmed = text.trim().removePrefix("\uFEFF")
        require(trimmed.isNotBlank()) { "沒有可匯入的內容" }
        val cards = if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            parseJson(trimmed)
        } else {
            parseDelimited(trimmed)
        }
        require(cards.isNotEmpty()) { "找不到「單字＋解釋」資料，請確認每行至少有兩欄" }
        return ParsedExternalDeck(title, "文字／CSV", cards.take(5_000))
    }

    private fun parseJson(text: String): List<ExternalCardCandidate> {
        val root = if (text.startsWith("[")) JSONArray(text) else {
            val objectRoot = JSONObject(text)
            objectRoot.optJSONArray("cards")
                ?: objectRoot.optJSONArray("terms")
                ?: objectRoot.optJSONArray("items")
                ?: JSONArray().put(objectRoot)
        }
        return buildList {
            for (index in 0 until root.length()) {
                val item = root.optJSONObject(index) ?: continue
                val word = firstValue(item, "word", "term", "front", "question")
                val definition = firstValue(item, "definition", "meaning", "back", "answer")
                val example = firstValue(item, "exampleSentence", "example", "sentence")
                if (word.isNotBlank() && definition.isNotBlank()) {
                    add(ExternalCardCandidate(cleanCell(word), cleanCell(definition), cleanCell(example)))
                }
            }
        }
    }

    private fun firstValue(json: JSONObject, vararg keys: String): String =
        keys.firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }.orEmpty()

    private fun parseDelimited(text: String): List<ExternalCardCandidate> {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return emptyList()
        val delimiter = listOf('\t', ';', ',')
            .maxByOrNull { delimiter -> lines.take(10).count { it.contains(delimiter) } }
            ?.takeIf { candidate -> lines.take(10).any { it.contains(candidate) } }
        return lines.mapNotNull { line ->
            val cells = if (delimiter == null) {
                line.split(Regex("\\s{2,}|\\s+-\\s+"), limit = 3)
            } else {
                splitCsvLine(line, delimiter)
            }.map(::cleanCell)
            if (cells.size < 2 || cells[0].isBlank() || cells[1].isBlank()) null
            else ExternalCardCandidate(
                word = cells[0],
                definition = cells[1],
                exampleSentence = cells.getOrNull(2).orEmpty()
            )
        }.dropHeaderIfNeeded()
    }

    private fun List<ExternalCardCandidate>.dropHeaderIfNeeded(): List<ExternalCardCandidate> {
        val first = firstOrNull() ?: return this
        val headerWords = setOf("word", "term", "front", "question", "單字", "題目")
        return if (first.word.trim().lowercase() in headerWords) drop(1) else this
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val values = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> {
                    values += cell.toString()
                    cell.clear()
                }
                else -> cell.append(char)
            }
            index++
        }
        values += cell.toString()
        return values
    }

    internal fun cleanCell(value: String): String = value
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('"')

    private fun requireSafeHttpsUrl(value: String): Uri {
        val uri = Uri.parse(value.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) { "只允許 HTTPS 網址" }
        val host = uri.host?.lowercase().orEmpty()
        require(host.isNotBlank()) { "網址格式錯誤" }
        require(
            host != "localhost" &&
                host != "127.0.0.1" &&
                host != "::1" &&
                !host.endsWith(".local") &&
                !Regex("""^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)""").containsMatchIn(host)
        ) { "不允許連線到本機或私人網路網址" }
        return uri
    }
}
