package com.example.data.importer

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Quizlet blocks many server/datacenter requests with a CAPTCHA even when the set is public.
 * Loading the public share page in Android's WebView uses the same normal browser path as the
 * user, then reads only the public JSON already embedded in that page.
 */
internal object QuizletWebViewImporter {
    private const val MAX_ATTEMPTS = 12
    private const val RETRY_DELAY_MS = 750L
    private const val TIMEOUT_MS = 25_000L

    suspend fun fetch(context: Context, url: String): ParsedExternalDeck =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                val handler = Handler(Looper.getMainLooper())
                val webView = WebView(context)
                val dialog = Dialog(context)
                var attempts = 0

                fun cleanup() {
                    handler.removeCallbacksAndMessages(null)
                    webView.stopLoading()
                    if (dialog.isShowing) dialog.dismiss()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                }

                fun finish(result: Result<ParsedExternalDeck>) {
                    if (!completed.compareAndSet(false, true)) return
                    cleanup()
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) }
                    )
                }

                val extractionScript = """
                    (() => {
                      const cards = [];
                      const seenCards = new Set();
                      const rankedCards = [];
                      const seenRankedCards = new Set();
                      const visited = new Set();
                      const clean = value => String(value ?? "")
                        .replace(/<br\s*\/?>/gi, "\n")
                        .replace(/<[^>]*>/g, " ")
                        .replace(/&nbsp;/gi, " ")
                        .replace(/&amp;/gi, "&")
                        .replace(/[ \t]+/g, " ")
                        .replace(/\n[ \t]+/g, "\n")
                        .trim();
                      const textOf = value => {
                        if (typeof value === "string" || typeof value === "number") return clean(value);
                        if (!value || typeof value !== "object") return "";
                        for (const key of ["text", "plainText", "value", "content", "answer"]) {
                          if (typeof value[key] === "string" || typeof value[key] === "number") {
                            return clean(value[key]);
                          }
                        }
                        return "";
                      };
                      const first = (object, keys) => {
                        for (const key of keys) {
                          const value = textOf(object?.[key]);
                          if (value) return value;
                        }
                        return "";
                      };
                      const add = (word, definition, exampleSentence = "") => {
                        word = clean(word);
                        definition = clean(definition);
                        exampleSentence = clean(exampleSentence);
                        if (!word || !definition || word === definition || word.length > 500) return;
                        const placeholder = word.toLocaleLowerCase() + "\u0000" + definition.toLocaleLowerCase();
                        if (["詞語\u0000定義", "正面\u0000背面", "word\u0000definition", "front\u0000back"].includes(placeholder)) return;
                        const key = word.toLocaleLowerCase();
                        if (seenCards.has(key) || cards.length >= 5000) return;
                        seenCards.add(key);
                        cards.push({word, definition, exampleSentence});
                      };
                      const addRanked = (word, definition, rank) => {
                        word = clean(word);
                        definition = clean(definition);
                        if (!word || !definition || word === definition) return;
                        const key = word.toLocaleLowerCase();
                        if (seenRankedCards.has(key)) return;
                        seenRankedCards.add(key);
                        rankedCards.push({
                          word,
                          definition,
                          exampleSentence: "",
                          rank: Number.isFinite(rank) ? rank : rankedCards.length
                        });
                      };
                      const walk = (value, depth = 0) => {
                        if (depth > 45 || value == null || cards.length >= 5000) return;
                        if (typeof value === "string") {
                          const trimmed = value.trim();
                          if (trimmed.length >= 2 && trimmed.length <= 3000000 &&
                              ((trimmed[0] === "{" && trimmed.endsWith("}")) ||
                               (trimmed[0] === "[" && trimmed.endsWith("]")))) {
                            try { walk(JSON.parse(trimmed), depth + 1); } catch (_) {}
                          }
                          return;
                        }
                        if (typeof value !== "object" || visited.has(value)) return;
                        visited.add(value);
                        if (!Array.isArray(value)) {
                          if (Array.isArray(value.cardSides)) {
                            const sideText = label => {
                              const side = value.cardSides.find(item => item?.label === label);
                              if (!side || !Array.isArray(side.media)) return "";
                              return side.media.map(item => textOf(item)).filter(Boolean).join("\n");
                            };
                            addRanked(sideText("word"), sideText("definition"), Number(value.rank));
                          }
                          if (value["@type"] === "Question" && value.acceptedAnswer) {
                            add(first(value, ["text", "name"]), first(value.acceptedAnswer, ["text", "answer", "name"]));
                          }
                          add(
                            first(value, ["word", "term", "front", "question", "prompt"]),
                            first(value, ["definition", "meaning", "back", "answer", "response", "description"]),
                            first(value, ["exampleSentence", "example", "sentence"])
                          );
                        }
                        Object.values(value).forEach(child => walk(child, depth + 1));
                      };
                      document.querySelectorAll('script[type="application/ld+json"], #__NEXT_DATA__')
                        .forEach(script => {
                          try { walk(JSON.parse(script.textContent || "")); } catch (_) {}
                        });
                      const title = clean(
                        document.querySelector("h1")?.textContent ||
                        document.querySelector('meta[property="og:title"]')?.content ||
                        document.title.replace(/\s*[|｜]\s*Quizlet.*$/i, "")
                      ) || "Quizlet";
                      const outputCards = rankedCards.length
                        ? rankedCards.sort((left, right) => left.rank - right.rank)
                            .map(({rank, ...card}) => card)
                        : cards;
                      return JSON.stringify({title, sourceName: "Quizlet", cards: outputCards});
                    })()
                """.trimIndent()

                fun extract() {
                    if (completed.get()) return
                    attempts += 1
                    webView.evaluateJavascript(extractionScript) { rawValue ->
                        if (completed.get()) return@evaluateJavascript
                        val parsed = runCatching {
                            // evaluateJavascript JSON-encodes a returned string, so unwrap it once.
                            val decoded = JSONArray("[$rawValue]").optString(0)
                            val root = JSONObject(decoded)
                            val items = root.optJSONArray("cards") ?: JSONArray()
                            val cards = buildList {
                                for (index in 0 until items.length()) {
                                    val item = items.optJSONObject(index) ?: continue
                                    val word = item.optString("word").trim()
                                    val definition = item.optString("definition").trim()
                                    val example = item.optString("exampleSentence").trim()
                                    if (word.isNotBlank() && definition.isNotBlank()) {
                                        add(
                                            ExternalCardCandidate(
                                                word = word,
                                                definition = definition,
                                                exampleSentence = example,
                                                sourceName = "Quizlet"
                                            )
                                        )
                                    }
                                }
                            }.distinctBy { "${it.word.lowercase()}\u0000${it.definition.lowercase()}" }
                            require(cards.isNotEmpty()) { "Quizlet 頁面尚未提供可匯入的公開卡片" }
                            ParsedExternalDeck(
                                title = root.optString("title").ifBlank { "Quizlet" },
                                sourceName = "Quizlet",
                                cards = cards
                            )
                        }
                        if (parsed.isSuccess) {
                            finish(parsed)
                        } else if (attempts < MAX_ATTEMPTS) {
                            handler.postDelayed(::extract, RETRY_DELAY_MS)
                        } else {
                            finish(
                                Result.failure(
                                    IllegalStateException(
                                        "Quizlet 沒有回傳公開牌組資料或要求人機驗證，請稍後重試或改用平台匯出文字"
                                    )
                                )
                            )
                        }
                    }
                }

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportMultipleWindows(false)
                }
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val host = request.url.host?.lowercase().orEmpty()
                        return host != "quizlet.com" && !host.endsWith(".quizlet.com")
                    }

                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                        handler.postDelayed(::extract, RETRY_DELAY_MS)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            finish(Result.failure(IllegalStateException("Quizlet 頁面載入失敗：${error.description}")))
                        }
                    }
                }

                dialog.setContentView(
                    webView,
                    ViewGroup.LayoutParams(1, 1)
                )
                dialog.window?.apply {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setDimAmount(0f)
                }
                dialog.setCancelable(false)
                dialog.show()
                dialog.window?.setLayout(1, 1)

                val timeout = Runnable {
                    finish(Result.failure(IllegalStateException("Quizlet 讀取逾時，請確認網路後重試")))
                }
                handler.postDelayed(timeout, TIMEOUT_MS)
                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true)) {
                        handler.post(::cleanup)
                    }
                }
                webView.loadUrl(url)
            }
        }
}
