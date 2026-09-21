import dns from "node:dns/promises";
import net from "node:net";

const MAX_RESPONSE_BYTES = 6_000_000;
const MAX_CARDS = 5_000;
const MAX_REDIRECTS = 5;
const USER_AGENT =
  "Mozilla/5.0 (compatible; VocabPulsePublicDeckImporter/1.0; +https://github.com/)";

const PROVIDERS = [
  { id: "quizlet", name: "Quizlet", hosts: ["quizlet.com"] },
  { id: "knowt", name: "Knowt", hosts: ["knowt.com"] },
  { id: "anki", name: "AnkiWeb", hosts: ["ankiweb.net"] },
  { id: "kahoot", name: "Kahoot!", hosts: ["kahoot.com", "kahoot.it", "create.kahoot.it"] },
  { id: "brainscape", name: "Brainscape", hosts: ["brainscape.com"] },
  { id: "cram", name: "Cram", hosts: ["cram.com"] },
  { id: "studysmarter", name: "StudySmarter", hosts: ["studysmarter.co.uk", "studysmarter.de"] },
  { id: "wordup", name: "WORD UP", hosts: ["wordup.com.tw"] },
  { id: "lingvist", name: "Lingvist", hosts: ["lingvist.com", "lingvist.io"] },
  { id: "vocabulazy", name: "Vocabulazy", hosts: ["vocabulazy.com"] },
  { id: "e4f", name: "E4F", hosts: ["e4f.com.tw"] },
  { id: "duolingo", name: "Duolingo", hosts: ["duolingo.com"] },
  { id: "voicetube", name: "VoiceTube", hosts: ["voicetube.com"] },
  { id: "memrise", name: "Memrise", hosts: ["memrise.com"] },
  { id: "remnote", name: "RemNote", hosts: ["remnote.com"] },
  { id: "mochi", name: "Mochi Cards", hosts: ["mochi.cards"] }
];

function httpError(status, message, code) {
  return Object.assign(new Error(message), { status, code });
}

function isPrivateIpv4(ip) {
  const parts = ip.split(".").map(Number);
  if (parts.length !== 4 || parts.some((part) => !Number.isInteger(part))) return true;
  const [a, b] = parts;
  return (
    a === 0 || a === 10 || a === 127 ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 0) ||
    (a === 192 && b === 168) ||
    (a === 198 && (b === 18 || b === 19)) ||
    a >= 224
  );
}

function isPrivateAddress(address) {
  if (net.isIPv4(address)) return isPrivateIpv4(address);
  if (!net.isIPv6(address)) return true;
  const value = address.toLowerCase();
  if (value.startsWith("::ffff:")) return isPrivateIpv4(value.slice(7));
  return (
    value === "::" || value === "::1" ||
    value.startsWith("fc") || value.startsWith("fd") ||
    /^fe[89ab]/.test(value) || value.startsWith("2001:db8:")
  );
}

async function validatePublicUrl(value) {
  let url;
  try {
    url = new URL(String(value ?? "").trim());
  } catch {
    throw httpError(400, "分享網址格式不正確", "INVALID_URL");
  }
  if (url.protocol !== "https:") {
    throw httpError(400, "分享網址只允許 HTTPS", "HTTPS_REQUIRED");
  }
  if (url.username || url.password || !url.hostname) {
    throw httpError(400, "分享網址格式不正確", "INVALID_URL");
  }
  if (url.hostname === "localhost" || url.hostname.endsWith(".local")) {
    throw httpError(400, "不允許讀取本機或私人網路網址", "PRIVATE_NETWORK");
  }
  const addresses = net.isIP(url.hostname)
    ? [{ address: url.hostname }]
    : await dns.lookup(url.hostname, { all: true }).catch(() => {
        throw httpError(422, "找不到分享網址的網站", "HOST_NOT_FOUND");
      });
  if (!addresses.length || addresses.some(({ address }) => isPrivateAddress(address))) {
    throw httpError(400, "不允許讀取本機或私人網路網址", "PRIVATE_NETWORK");
  }
  return url;
}

async function readLimitedBody(response) {
  const declared = Number(response.headers.get("content-length"));
  if (declared > MAX_RESPONSE_BYTES) {
    throw httpError(413, "分享頁超過 6 MB，無法安全解析", "PAGE_TOO_LARGE");
  }
  if (!response.body) return "";
  const chunks = [];
  let size = 0;
  for await (const chunk of response.body) {
    size += chunk.length;
    if (size > MAX_RESPONSE_BYTES) {
      throw httpError(413, "分享頁超過 6 MB，無法安全解析", "PAGE_TOO_LARGE");
    }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

async function fetchPublicPage(initialUrl, fetchImpl = fetch) {
  let url = await validatePublicUrl(initialUrl);
  for (let redirect = 0; redirect <= MAX_REDIRECTS; redirect += 1) {
    const response = await fetchImpl(url, {
      redirect: "manual",
      headers: {
        "user-agent": USER_AGENT,
        accept: "text/html,application/json,text/plain;q=0.9,*/*;q=0.1",
        "accept-language": "zh-TW,zh;q=0.9,en;q=0.8"
      },
      signal: AbortSignal.timeout(20_000)
    });
    if ([301, 302, 303, 307, 308].includes(response.status)) {
      const location = response.headers.get("location");
      if (!location) throw httpError(502, "網站回傳無效的重新導向", "BAD_REDIRECT");
      url = await validatePublicUrl(new URL(location, url).href);
      continue;
    }
    const contentType = response.headers.get("content-type")?.toLowerCase() ?? "";
    const body = await readLimitedBody(response);
    return { response, body, contentType, finalUrl: url.href };
  }
  throw httpError(502, "分享網址重新導向次數過多", "TOO_MANY_REDIRECTS");
}

function providerFor(url) {
  const host = new URL(url).hostname.toLowerCase();
  return PROVIDERS.find((provider) =>
    provider.hosts.some((allowed) => host === allowed || host.endsWith(`.${allowed}`))
  ) ?? { id: "generic", name: host, hosts: [host] };
}

function decodeHtml(value) {
  const named = {
    amp: "&", lt: "<", gt: ">", quot: "\"", apos: "'", nbsp: " "
  };
  return String(value ?? "")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(?:p|div|li|h[1-6])>/gi, " ")
    .replace(/<[^>]*>/g, " ")
    .replace(/&(#x[0-9a-f]+|#\d+|amp|lt|gt|quot|apos|nbsp);/gi, (_, entity) => {
      if (entity[0] === "#") {
        const hex = entity[1].toLowerCase() === "x";
        return String.fromCodePoint(Number.parseInt(entity.slice(hex ? 2 : 1), hex ? 16 : 10));
      }
      return named[entity.toLowerCase()] ?? _;
    })
    .replace(/\s+/g, " ")
    .trim();
}

function clean(value) {
  return decodeHtml(value).slice(0, 10_000);
}

function titleFromHtml(html, fallback) {
  const og = html.match(/<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']/i)
    ?? html.match(/<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:title["']/i);
  const title = og?.[1] ?? html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1];
  return clean(title || fallback).replace(/\s+(?:Flashcards.*?)?\|\s+\S+$/i, "").trim() || fallback;
}

function normalizeCard(word, definition, exampleSentence = "") {
  const normalized = {
    word: clean(word),
    definition: clean(definition),
    exampleSentence: clean(exampleSentence)
  };
  if (!normalized.word || !normalized.definition || normalized.word === normalized.definition) return null;
  const placeholderPair = `${normalized.word.toLocaleLowerCase()}\u0000${normalized.definition.toLocaleLowerCase()}`;
  if (new Set([
    "詞語\u0000定義",
    "正面\u0000背面",
    "word\u0000definition",
    "front\u0000back"
  ]).has(placeholderPair)) return null;
  if (normalized.word.length > 500 || normalized.definition.length > 10_000) return null;
  return normalized;
}

function deduplicateCards(cards) {
  const seen = new Set();
  return cards.filter(Boolean).filter((card) => {
    const key = `${card.word.toLocaleLowerCase()}\u0000${card.definition.toLocaleLowerCase()}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  }).slice(0, MAX_CARDS);
}

function deduplicateCardsByWord(cards) {
  const seen = new Set();
  return cards.filter(Boolean).filter((card) => {
    const key = card.word.toLocaleLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  }).slice(0, MAX_CARDS);
}

const WORD_KEYS = ["word", "term", "front", "question", "prompt"];
const DEFINITION_KEYS = ["definition", "meaning", "back", "answer", "response", "description"];
const EXAMPLE_KEYS = ["exampleSentence", "example", "sentence"];

function firstString(object, keys) {
  for (const key of keys) {
    const value = object?.[key];
    if (typeof value === "string" || typeof value === "number") return String(value);
    if (value && typeof value === "object") {
      for (const nestedKey of ["text", "plainText", "value", "content"]) {
        if (typeof value[nestedKey] === "string") return value[nestedKey];
      }
    }
  }
  return "";
}

function cardsFromJson(root) {
  const cards = [];
  const rankedSideCards = [];
  const queue = [root];
  const visited = new Set();
  let inspected = 0;
  while (queue.length && inspected < 50_000 && cards.length < MAX_CARDS) {
    const current = queue.shift();
    if (!current) continue;
    if (typeof current === "string") {
      const trimmed = current.trim();
      if (
        trimmed.length >= 2 &&
        trimmed.length <= 3_000_000 &&
        (
          (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
          (trimmed.startsWith("[") && trimmed.endsWith("]"))
        )
      ) {
        try {
          queue.push(JSON.parse(trimmed));
        } catch {
          // Modern Quizlet sometimes embeds serialized JSON in another JSON object.
        }
      }
      continue;
    }
    if (typeof current !== "object" || visited.has(current)) continue;
    visited.add(current);
    inspected += 1;
    if (!Array.isArray(current)) {
      if (Array.isArray(current.cardSides)) {
        const sideText = (label) => {
          const side = current.cardSides.find((candidate) => candidate?.label === label);
          if (!Array.isArray(side?.media)) return "";
          return side.media
            .map((media) => firstString(media, ["plainText", "text", "value", "content"]))
            .filter(Boolean)
            .join("\n");
        };
        const quizletCard = normalizeCard(sideText("word"), sideText("definition"));
        if (quizletCard) {
          rankedSideCards.push({
            ...quizletCard,
            rank: Number.isFinite(Number(current.rank)) ? Number(current.rank) : rankedSideCards.length
          });
        }
      }
      if (current["@type"] === "Question" && current.acceptedAnswer) {
        const schemaCard = normalizeCard(
          firstString(current, ["text", "name"]),
          firstString(current.acceptedAnswer, ["text", "answer", "name"])
        );
        if (schemaCard) cards.push(schemaCard);
      }
      const word = firstString(current, WORD_KEYS);
      const definition = firstString(current, DEFINITION_KEYS);
      const example = firstString(current, EXAMPLE_KEYS);
      const card = normalizeCard(word, definition, example);
      if (card) cards.push(card);

      // Kahoot-style question: use the correct public answer as the back.
      const question = firstString(current, ["question", "questionText", "title"]);
      const choices = current.choices ?? current.answers;
      if (question && Array.isArray(choices)) {
        const correct = choices.find((choice) => choice?.correct === true || choice?.isCorrect === true);
        const answer = firstString(correct, ["answer", "text", "title"]);
        const kahootCard = normalizeCard(question, answer);
        if (kahootCard) cards.push(kahootCard);
      }
    }
    for (const value of Object.values(current)) {
      if (
        value &&
        (
          typeof value === "object" ||
          (
            typeof value === "string" &&
            value.length >= 2 &&
            (
              value.trimStart().startsWith("{") ||
              value.trimStart().startsWith("[")
            )
          )
        )
      ) {
        queue.push(value);
      }
    }
  }
  if (rankedSideCards.length) {
    return deduplicateCardsByWord(
      rankedSideCards
        .sort((left, right) => left.rank - right.rank)
        .map(({ rank: _rank, ...card }) => card)
    );
  }
  return deduplicateCards(cards);
}

function jsonBlocks(html) {
  const values = [];
  const scriptPattern = /<script\b[^>]*type=["'](?:application\/json|application\/ld\+json)["'][^>]*>([\s\S]*?)<\/script>/gi;
  for (const match of html.matchAll(scriptPattern)) {
    try {
      values.push(JSON.parse(match[1]));
    } catch {
      // Invalid analytics or escaped payload; other parsers still run.
    }
  }
  return values;
}

function pairedClassText(html, leftClass, rightClass) {
  const left = [...html.matchAll(new RegExp(
    `<[^>]+class=["'][^"']*${leftClass}[^"']*["'][^>]*>([\\s\\S]*?)<\\/[^>]+>`,
    "gi"
  ))].map((match) => match[1]);
  const right = [...html.matchAll(new RegExp(
    `<[^>]+class=["'][^"']*${rightClass}[^"']*["'][^>]*>([\\s\\S]*?)<\\/[^>]+>`,
    "gi"
  ))].map((match) => match[1]);
  return left.slice(0, Math.min(left.length, right.length)).map((word, index) =>
    normalizeCard(word, right[index])
  );
}

function cardsFromHtml(html, providerId) {
  const cards = [];

  if (providerId === "knowt") {
    const prose = [...html.matchAll(/<div class=["']ProseMirror["'][^>]*>([\s\S]*?)<\/div>/gi)]
      .map((match) => match[1]);
    for (let index = 0; index + 1 < prose.length; index += 2) {
      cards.push(normalizeCard(prose[index], prose[index + 1]));
    }
  }

  cards.push(...pairedClassText(html, "SetPageTerm-wordText", "SetPageTerm-definitionText"));
  cards.push(...pairedClassText(html, "card-question", "card-answer"));
  cards.push(...pairedClassText(html, "front_text", "back_text"));

  for (const value of jsonBlocks(html)) cards.push(...cardsFromJson(value));

  // Common embedded application-state objects, including modern SSR frameworks.
  const statePatterns = [
    /<script[^>]+id=["']__NEXT_DATA__["'][^>]*>([\s\S]*?)<\/script>/i,
    /window\.__INITIAL_STATE__\s*=\s*(\{[\s\S]*?\})\s*;<\/script>/i
  ];
  for (const pattern of statePatterns) {
    const match = html.match(pattern);
    if (!match) continue;
    try {
      cards.push(...cardsFromJson(JSON.parse(match[1])));
    } catch {
      // Page-state format changed; provider-specific HTML parsing may still work.
    }
  }
  return deduplicateCards(cards);
}

function blockedReason(status, body) {
  const sample = body.slice(0, 250_000).toLowerCase();
  if (
    status === 403 || status === 429 ||
    /captcha challenge|verify you are human|cf-chl-|access denied|unusual traffic|request blocked|403 error|request could not be satisfied/.test(sample)
  ) {
    return httpError(
      409,
      "網站要求人機驗證或封鎖伺服器讀取；請在瀏覽器開啟後使用平台的匯出功能",
      "SITE_VERIFICATION_REQUIRED"
    );
  }
  if (
    status === 401 ||
    /<title[^>]*>\s*(?:log in|sign in)|請先登入|需要登入/.test(sample)
  ) {
    return httpError(
      409,
      "這個分享頁需要登入，無法當作公開牌組匯入",
      "LOGIN_REQUIRED"
    );
  }
  return null;
}

export async function importExternalDeck(url, { fetchImpl = fetch } = {}) {
  const fetched = await fetchPublicPage(url, fetchImpl);
  const provider = providerFor(fetched.finalUrl);
  const blocked = blockedReason(fetched.response.status, fetched.body);
  if (blocked) throw blocked;
  if (!fetched.response.ok) {
    throw httpError(
      422,
      `網站無法讀取這個分享頁（HTTP ${fetched.response.status}）`,
      "REMOTE_HTTP_ERROR"
    );
  }
  if (
    fetched.contentType.includes("application/octet-stream") ||
    fetched.finalUrl.toLowerCase().split("?")[0].endsWith(".apkg")
  ) {
    throw httpError(
      422,
      "這是 Anki 套件檔；請下載 .apkg 後使用 App 的「匯入檔案」",
      "ANKI_PACKAGE_DOWNLOAD"
    );
  }

  let cards = [];
  let method = "public_html";
  if (fetched.contentType.includes("json") || /^[\s\uFEFF]*[\[{]/.test(fetched.body)) {
    try {
      cards = cardsFromJson(JSON.parse(fetched.body.replace(/^\uFEFF/, "")));
      method = "public_json";
    } catch {
      throw httpError(422, "網站回傳的 JSON 格式無法解析", "INVALID_REMOTE_JSON");
    }
  } else {
    cards = cardsFromHtml(fetched.body, provider.id);
  }
  if (!cards.length) {
    const message = provider.id === "anki"
      ? "AnkiWeb 分享頁不公開卡片內容；請從官方頁面下載 .apkg，再用「匯入檔案」"
      : "已讀取公開頁面，但頁面裡找不到可驗證的「單字＋解釋」資料；網站可能已改版或未公開內容";
    throw httpError(422, message, "NO_PUBLIC_CARDS");
  }

  return {
    title: titleFromHtml(fetched.body, provider.name),
    sourceName: provider.name,
    sourceUrl: fetched.finalUrl,
    method,
    cards
  };
}

export const __test = {
  cardsFromHtml,
  cardsFromJson,
  decodeHtml,
  providerFor
};
