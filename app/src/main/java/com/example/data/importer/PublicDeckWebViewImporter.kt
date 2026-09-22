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

/** Reads only content already exposed in a provider's public web page. */
internal object PublicDeckWebViewImporter {
    private const val MAX_ATTEMPTS = 14
    private const val RETRY_DELAY_MS = 700L
    private const val TIMEOUT_MS = 30_000L

    suspend fun search(
        context: Context,
        source: ExternalVocabularySource,
        query: String
    ): List<ExternalSearchResult> = withContext(Dispatchers.Main.immediate) {
        val searchUrl = source.buildSearchUrl(query)
        val sourceName = JSONObject.quote(source.name)
        val allowedHosts = JSONArray(source.hosts).toString()
        val script = """
            (() => {
              const sourceName = $sourceName;
              const allowedHosts = $allowedHosts;
              const clean = value => String(value ?? "").replace(/\s+/g, " ").trim();
              const unwrap = raw => {
                try {
                  const parsed = new URL(raw, location.href);
                  if (parsed.hostname.includes("google.") && parsed.pathname === "/url") {
                    return parsed.searchParams.get("q") || parsed.searchParams.get("url") || "";
                  }
                  return parsed.href;
                } catch (_) { return ""; }
              };
              const isAllowed = raw => {
                try {
                  const url = new URL(raw);
                  if (url.protocol !== "https:") return false;
                  if (!allowedHosts.length) return !url.hostname.includes("google.");
                  return allowedHosts.some(host => url.hostname === host || url.hostname.endsWith("." + host));
                } catch (_) { return false; }
              };
              const looksLikeContent = href => {
                const value = href.toLowerCase();
                const patterns = {
                  quizlet: /\/\d+\/[^/]+(?:flash-cards|flashcards)/,
                  anki: /\/shared\/info\/\d+/,
                  kahoot: /\/(?:details|kahoot)\//,
                  brainscape: /\/(?:packs|decks)\//,
                  knowt: /\/(?:flashcards|study)\//,
                  cram: /\/flashcards\//,
                  studysmarter: /\/explanations\/flashcards\//
                };
                const id = ${JSONObject.quote(source.id)};
                return patterns[id] ? patterns[id].test(value) :
                  !/\/(?:login|signup|search|help|privacy|terms)(?:[/?#]|$)/.test(value);
              };
              const results = [];
              const seen = new Set();
              document.querySelectorAll("a[href]").forEach(anchor => {
                const href = unwrap(anchor.href || anchor.getAttribute("href"));
                const title = clean(anchor.innerText || anchor.textContent || anchor.getAttribute("aria-label"));
                if (!href || !title || title.length < 2 || title.length > 240 || !isAllowed(href) || !looksLikeContent(href)) return;
                const normalized = href.split("#")[0];
                if (seen.has(normalized) || results.length >= 50) return;
                seen.add(normalized);
                const parent = anchor.closest("article, li, [role='listitem'], div");
                const description = clean(parent?.innerText || "").replace(title, "").slice(0, 280);
                results.push({title, url: normalized, description, sourceName});
              });
              return JSON.stringify({results});
            })()
        """.trimIndent()
        val raw = evaluatePublicPage(context, searchUrl, source, script) { value ->
            runCatching {
                val decoded = decodeJavascriptString(value)
                (JSONObject(decoded).optJSONArray("results")?.length() ?: 0) > 0
            }.getOrDefault(false)
        }
        val items = JSONObject(decodeJavascriptString(raw)).optJSONArray("results") ?: JSONArray()
        buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val url = item.optString("url").trim()
                if (title.isNotBlank() && url.startsWith("https://")) {
                    add(
                        ExternalSearchResult(
                            title = title,
                            url = url,
                            description = item.optString("description").trim(),
                            sourceName = source.name
                        )
                    )
                }
            }
        }.distinctBy { it.url }.take(50)
    }

    suspend fun fetch(
        context: Context,
        source: ExternalVocabularySource,
        url: String
    ): ParsedExternalDeck = withContext(Dispatchers.Main.immediate) {
        val sourceName = JSONObject.quote(source.name)
        val script = """
            (() => {
              const sourceName = $sourceName;
              const cards = [];
              const ranked = [];
              const seen = new Set();
              const rankedSeen = new Set();
              const visited = new Set();
              const clean = value => String(value ?? "")
                .replace(/<br\s*\/?>/gi, "\n").replace(/<[^>]*>/g, " ")
                .replace(/&nbsp;/gi, " ").replace(/&amp;/gi, "&")
                .replace(/[ \t]+/g, " ").replace(/\n[ \t]+/g, "\n").trim();
              const textOf = value => {
                if (typeof value === "string" || typeof value === "number") return clean(value);
                if (!value || typeof value !== "object") return "";
                for (const key of ["text", "plainText", "value", "content", "answer", "name"]) {
                  if (typeof value[key] === "string" || typeof value[key] === "number") return clean(value[key]);
                }
                return "";
              };
              const first = (object, keys) => {
                for (const key of keys) { const value = textOf(object?.[key]); if (value) return value; }
                return "";
              };
              const valid = (word, definition) => word && definition && word !== definition &&
                word.length <= 500 && definition.length <= 10000 &&
                !["詞語\u0000定義", "正面\u0000背面", "word\u0000definition", "front\u0000back"]
                  .includes(word.toLocaleLowerCase() + "\u0000" + definition.toLocaleLowerCase());
              const add = (word, definition, exampleSentence = "") => {
                word = clean(word); definition = clean(definition); exampleSentence = clean(exampleSentence);
                if (!valid(word, definition)) return;
                const key = word.toLocaleLowerCase();
                if (seen.has(key) || cards.length >= 5000) return;
                seen.add(key); cards.push({word, definition, exampleSentence});
              };
              const addRanked = (word, definition, rank) => {
                word = clean(word); definition = clean(definition);
                if (!valid(word, definition)) return;
                const key = word.toLocaleLowerCase();
                if (rankedSeen.has(key)) return;
                rankedSeen.add(key); ranked.push({word, definition, exampleSentence: "", rank: Number.isFinite(rank) ? rank : ranked.length});
              };
              const walk = (value, depth = 0) => {
                if (depth > 45 || value == null || cards.length >= 5000) return;
                if (typeof value === "string") {
                  const trimmed = value.trim();
                  if (trimmed.length >= 2 && trimmed.length <= 3000000 &&
                      ((trimmed[0] === "{" && trimmed.endsWith("}")) || (trimmed[0] === "[" && trimmed.endsWith("]")))) {
                    try { walk(JSON.parse(trimmed), depth + 1); } catch (_) {}
                  }
                  return;
                }
                if (typeof value !== "object" || visited.has(value)) return;
                visited.add(value);
                if (!Array.isArray(value)) {
                  if (Array.isArray(value.cardSides)) {
                    const side = label => {
                      const found = value.cardSides.find(item => item?.label === label);
                      return Array.isArray(found?.media) ? found.media.map(textOf).filter(Boolean).join("\n") : "";
                    };
                    addRanked(side("word"), side("definition"), Number(value.rank));
                  }
                  if (value["@type"] === "Question" && value.acceptedAnswer) {
                    add(first(value, ["text", "name"]), first(value.acceptedAnswer, ["text", "answer", "name"]));
                  }
                  add(
                    first(value, ["word", "term", "front", "question", "prompt"]),
                    first(value, ["definition", "meaning", "back", "answer", "response", "description"]),
                    first(value, ["exampleSentence", "example", "sentence"])
                  );
                  const question = first(value, ["question", "questionText", "title"]);
                  const choices = value.choices || value.answers;
                  if (question && Array.isArray(choices)) {
                    const correct = choices.find(choice => choice?.correct === true || choice?.isCorrect === true);
                    add(question, first(correct, ["answer", "text", "title"]));
                  }
                }
                Object.values(value).forEach(child => walk(child, depth + 1));
              };
              document.querySelectorAll('script[type="application/json"], script[type="application/ld+json"], #__NEXT_DATA__')
                .forEach(node => { try { walk(JSON.parse(node.textContent || "")); } catch (_) {} });
              for (const state of [window.__INITIAL_STATE__, window.__NEXT_DATA__, window.__APOLLO_STATE__]) {
                try { if (state) walk(state); } catch (_) {}
              }
              const pairs = [
                ["[class*='front_text']", "[class*='back_text']"],
                ["[class*='card-question']", "[class*='card-answer']"],
                ["[data-testid*='term']", "[data-testid*='definition']"],
                ["[class*='flashcard-front']", "[class*='flashcard-back']"]
              ];
              pairs.forEach(([leftSelector, rightSelector]) => {
                const left = [...document.querySelectorAll(leftSelector)];
                const right = [...document.querySelectorAll(rightSelector)];
                left.slice(0, Math.min(left.length, right.length)).forEach((item, index) => add(item.innerText, right[index].innerText));
              });
              const title = clean(document.querySelector("h1")?.innerText ||
                document.querySelector('meta[property="og:title"]')?.content || document.title) || sourceName;
              const output = ranked.length ? ranked.sort((a, b) => a.rank - b.rank).map(({rank, ...card}) => card) : cards;
              return JSON.stringify({title, sourceName, cards: output});
            })()
        """.trimIndent()
        val raw = evaluatePublicPage(context, url, source, script) { value ->
            runCatching {
                val decoded = decodeJavascriptString(value)
                (JSONObject(decoded).optJSONArray("cards")?.length() ?: 0) > 0
            }.getOrDefault(false)
        }
        val root = JSONObject(decodeJavascriptString(raw))
        val items = root.optJSONArray("cards") ?: JSONArray()
        val cards = buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val word = item.optString("word").trim()
                val definition = item.optString("definition").trim()
                if (word.isNotBlank() && definition.isNotBlank()) {
                    add(
                        ExternalCardCandidate(
                            word = word,
                            definition = definition,
                            exampleSentence = item.optString("exampleSentence").trim(),
                            sourceName = source.name
                        )
                    )
                }
            }
        }.distinctBy { it.word.lowercase() }
        require(cards.isNotEmpty()) { "${source.name} 公開頁沒有可驗證的正面／背面資料" }
        ParsedExternalDeck(root.optString("title").ifBlank { source.name }, source.name, cards)
    }

    private suspend fun evaluatePublicPage(
        context: Context,
        url: String,
        source: ExternalVocabularySource,
        script: String,
        accepts: (String) -> Boolean
    ): String = suspendCancellableCoroutine { continuation ->
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
        fun finish(result: Result<String>) {
            if (!completed.compareAndSet(false, true)) return
            cleanup()
            result.fold(continuation::resume, continuation::resumeWithException)
        }
        fun extract() {
            if (completed.get()) return
            attempts += 1
            webView.evaluateJavascript(script) { raw ->
                if (completed.get()) return@evaluateJavascript
                if (accepts(raw)) finish(Result.success(raw))
                else if (attempts < MAX_ATTEMPTS) handler.postDelayed(::extract, RETRY_DELAY_MS)
                else finish(Result.failure(IllegalStateException("${source.name} 頁面沒有公開可匯入的資料，或網站要求登入／驗證")))
            }
        }

        val initialHost = runCatching { android.net.Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")
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
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host?.lowercase().orEmpty()
                return host != initialHost && !host.endsWith(".$initialHost") &&
                    source.hosts.none { allowed -> host == allowed || host.endsWith(".$allowed") }
            }

            override fun onPageFinished(view: WebView, loadedUrl: String) {
                handler.postDelayed(::extract, RETRY_DELAY_MS)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) finish(Result.failure(IllegalStateException("${source.name} 頁面載入失敗：${error.description}")))
            }
        }
        dialog.setContentView(webView, ViewGroup.LayoutParams(1, 1))
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0f)
        }
        dialog.setCancelable(false)
        dialog.show()
        dialog.window?.setLayout(1, 1)
        handler.postDelayed({
            finish(Result.failure(IllegalStateException("${source.name} 讀取逾時，請確認網路或改用公開分享網址")))
        }, TIMEOUT_MS)
        continuation.invokeOnCancellation {
            if (completed.compareAndSet(false, true)) handler.post(::cleanup)
        }
        webView.loadUrl(url)
    }

    private fun decodeJavascriptString(rawValue: String): String =
        JSONArray("[$rawValue]").optString(0)
}
