import http from "node:http";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";
import { importExternalDeck } from "./external-import.mjs";

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff"
};
const MAX_BODY_BYTES = 100_000;
const WORD_PATTERN = /^[A-Za-z][A-Za-z '-]{0,99}$/;

function sendJson(response, status, payload, extraHeaders = {}) {
  response.writeHead(status, { ...JSON_HEADERS, ...extraHeaders });
  response.end(JSON.stringify(payload));
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) {
      throw Object.assign(new Error("請求內容過大"), { status: 413 });
    }
    chunks.push(chunk);
  }
  if (chunks.length === 0) return {};
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    throw Object.assign(new Error("JSON 格式錯誤"), { status: 400 });
  }
}

function extractJson(text) {
  const normalized = String(text ?? "")
    .trim()
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/, "");
  return JSON.parse(normalized);
}

function vocabularyPrompt(words, customInstructions = "") {
  const instructions = String(customInstructions ?? "").trim().slice(0, 4_000);
  return `${instructions ? `${instructions}\n\n` : ""}Provide lexicographical flashcard data in Traditional Chinese for these English words:
${words.join(", ")}

Return only a JSON array. Each object must contain:
word, phonetic, partOfSpeech, definition, exampleSentence, exampleTranslation.`;
}

function validateVocabularyItems(value, requestedWords = []) {
  const items = Array.isArray(value) ? value : [value];
  const allowed = new Set(requestedWords.map((word) => word.toLowerCase()));
  const normalized = items.map((item) => {
    const word = String(item?.word ?? "").trim();
    const definition = String(item?.definition ?? "").trim();
    const exampleSentence = String(item?.exampleSentence ?? "").trim();
    const partOfSpeech = String(item?.partOfSpeech ?? "").trim();
    const exampleTranslation = String(item?.exampleTranslation ?? "").trim();
    if (
      !WORD_PATTERN.test(word) ||
      !definition ||
      !partOfSpeech ||
      !exampleSentence ||
      !exampleTranslation
    ) {
      throw Object.assign(new Error("AI 回傳內容未通過品質驗證"), { status: 502 });
    }
    if (allowed.size > 0 && !allowed.has(word.toLowerCase())) {
      throw Object.assign(new Error("AI 回傳了未要求的單字"), { status: 502 });
    }
    return {
      word,
      phonetic: String(item?.phonetic ?? "").trim(),
      partOfSpeech,
      definition,
      exampleSentence,
      exampleTranslation
    };
  });
  return Array.isArray(value) ? normalized : normalized[0];
}

async function generateWithGemini(prompt, env) {
  const apiKey = env.GEMINI_API_KEY?.trim();
  if (!apiKey) {
    throw Object.assign(new Error("AI 服務尚未設定"), { status: 503 });
  }
  const model = env.GEMINI_MODEL?.trim() || "gemini-3.6-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-goog-api-key": apiKey
    },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        temperature: 0.2,
        responseMimeType: "application/json",
        maxOutputTokens: 4096
      }
    }),
    signal: AbortSignal.timeout(40_000)
  });
  if (!response.ok) {
    throw Object.assign(new Error("AI 上游服務暫時無法使用"), { status: 502 });
  }
  const payload = await response.json();
  const text = payload?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    throw Object.assign(new Error("AI 回傳內容不完整"), { status: 502 });
  }
  return extractJson(text);
}

function createRateLimiter(env) {
  const buckets = new Map();
  const max = Math.max(1, Number(env.RATE_LIMIT_MAX) || 60);
  const windowMs = Math.max(10_000, Number(env.RATE_LIMIT_WINDOW_MS) || 600_000);
  return (address) => {
    const now = Date.now();
    const current = buckets.get(address);
    if (!current || now >= current.resetAt) {
      buckets.set(address, { count: 1, resetAt: now + windowMs });
      return true;
    }
    current.count += 1;
    return current.count <= max;
  };
}

export function createAppServer({
  env = process.env,
  generate = generateWithGemini,
  importDeck = importExternalDeck
} = {}) {
  const allowRequest = createRateLimiter(env);

  return http.createServer(async (request, response) => {
    const requestUrl = new URL(request.url ?? "/", "http://localhost");
    const corsHeaders = env.ALLOWED_ORIGIN
      ? { "access-control-allow-origin": env.ALLOWED_ORIGIN, vary: "origin" }
      : {};

    if (request.method === "GET" && requestUrl.pathname === "/health") {
      return sendJson(response, 200, { status: "ok" }, corsHeaders);
    }
    if (request.method === "GET" && requestUrl.pathname === "/api/v1/app/version") {
      return sendJson(response, 200, {
        data: {
          versionCode: Math.max(1, Number(env.APP_VERSION_CODE) || 2),
          versionName: env.APP_VERSION_NAME || "1.1.0",
          minimumVersionCode: Math.max(1, Number(env.APP_MINIMUM_VERSION_CODE) || 1),
          releaseNotes: env.APP_RELEASE_NOTES || "目前已是最新版本",
          storeUrl: env.APP_STORE_URL || ""
        }
      }, corsHeaders);
    }
    if (request.method !== "POST") {
      return sendJson(response, 404, { error: "找不到服務端點" }, corsHeaders);
    }
    if (!allowRequest(request.socket.remoteAddress ?? "unknown")) {
      return sendJson(response, 429, { error: "請求過於頻繁，請稍後再試" }, corsHeaders);
    }

    try {
      const body = await readJson(request);
      let data;

      if (requestUrl.pathname === "/api/v1/import/url") {
        const url = String(body.url ?? "").trim();
        if (!url || url.length > 2_000) {
          throw Object.assign(new Error("請提供有效的分享網址"), {
            status: 400,
            code: "INVALID_URL"
          });
        }
        const imported = await importDeck(url);
        data = {
          title: imported.title,
          sourceName: imported.sourceName,
          sourceUrl: imported.sourceUrl,
          cards: imported.cards
        };
        return sendJson(response, 200, {
          data,
          meta: {
            source: "public_share_page",
            parser: imported.method,
            cardCount: imported.cards.length,
            requestId: randomUUID(),
            fetchedAt: new Date().toISOString()
          }
        }, corsHeaders);
      } else if (requestUrl.pathname === "/api/v1/vocabulary/details") {
        const word = String(body.word ?? "").trim();
        if (!WORD_PATTERN.test(word)) {
          throw Object.assign(new Error("請輸入有效的英文單字或片語"), { status: 400 });
        }
        const result = await generate(vocabularyPrompt([word], body.instructions), env);
        data = validateVocabularyItems(Array.isArray(result) ? result[0] : result, [word]);
      } else if (requestUrl.pathname === "/api/v1/vocabulary/analyze") {
        const text = String(body.text ?? "").trim();
        if (!text || text.length > 20_000) {
          throw Object.assign(new Error("文件文字必須介於 1 到 20,000 字元"), { status: 400 });
        }
        const result = await generate(`Extract useful English vocabulary from the text below.
Remove headings, page numbers, instructions, copyright and other noise.
For every vocabulary item return word, phonetic, partOfSpeech, Traditional Chinese definition,
an English exampleSentence and Traditional Chinese exampleTranslation.
Return only a JSON array and return at most 80 items.

TEXT:
${text}`, env);
        data = validateVocabularyItems(result);
      } else if (requestUrl.pathname === "/api/v1/vocabulary/enrich") {
        const words = Array.isArray(body.words)
          ? [...new Set(body.words.map((word) => String(word).trim()).filter((word) => WORD_PATTERN.test(word)))].slice(0, 80)
          : [];
        if (words.length === 0) {
          throw Object.assign(new Error("請提供有效的英文單字"), { status: 400 });
        }
        data = validateVocabularyItems(await generate(vocabularyPrompt(words), env), words);
      } else {
        return sendJson(response, 404, { error: "找不到服務端點" }, corsHeaders);
      }

      return sendJson(response, 200, {
        data,
        meta: {
          source: "gemini",
          model: env.GEMINI_MODEL?.trim() || "gemini-3.6-flash",
          requestId: randomUUID(),
          generatedAt: new Date().toISOString()
        }
      }, corsHeaders);
    } catch (error) {
      const status = Number(error?.status) || 500;
      const safeMessage = status >= 500 && !error?.status ? "伺服器暫時無法處理請求" : error.message;
      return sendJson(response, status, {
        error: safeMessage,
        ...(error?.code ? { code: error.code } : {})
      }, corsHeaders);
    }
  });
}

const isMainModule = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMainModule) {
  const port = Math.max(1, Number(process.env.PORT) || 8787);
  createAppServer().listen(port, "0.0.0.0", () => {
    console.log(`VocabPulse AI gateway listening on http://0.0.0.0:${port}`);
  });
}
