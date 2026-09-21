import assert from "node:assert/strict";
import test from "node:test";
import { createAppServer } from "../src/server.mjs";

async function withServer(generate, action, importDeck) {
  const server = createAppServer({
    env: { RATE_LIMIT_MAX: "100" },
    generate,
    ...(importDeck ? { importDeck } : {})
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  try {
    await action(`http://127.0.0.1:${address.port}`);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
}

test("health endpoint is available", async () => {
  await withServer(async () => [], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/health`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: "ok" });
  });
});

test("details validates the requested word", async () => {
  await withServer(async () => [], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/vocabulary/details`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ word: "123" })
    });
    assert.equal(response.status, 400);
  });
});

test("details returns normalized gateway response", async () => {
  const generated = [{
    word: "resilient",
    phonetic: "/rɪˈzɪl.i.ənt/",
    partOfSpeech: "adj.",
    definition: "有韌性的",
    exampleSentence: "She remained resilient.",
    exampleTranslation: "她保持堅韌。"
  }];
  await withServer(async () => generated, async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/vocabulary/details`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ word: "resilient" })
    });
    assert.equal(response.status, 200);
    assert.deepEqual((await response.json()).data, generated[0]);
  });
});

test("details rejects a model response for a different word", async () => {
  await withServer(async () => [{
    word: "banana",
    definition: "香蕉",
    exampleSentence: "I ate a banana."
  }], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/vocabulary/details`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ word: "apple" })
    });
    assert.equal(response.status, 502);
  });
});

test("version endpoint returns update metadata", async () => {
  await withServer(async () => [], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/app/version`);
    const payload = await response.json();
    assert.equal(response.status, 200);
    assert.equal(payload.data.versionCode, 2);
    assert.equal(payload.data.versionName, "1.1.0");
  });
});

test("public share import returns only parser-produced cards and provenance", async () => {
  const imported = {
    title: "Public deck",
    sourceName: "Example",
    sourceUrl: "https://example.com/deck",
    method: "public_html",
    cards: [{ word: "apple", definition: "蘋果", exampleSentence: "" }]
  };
  await withServer(async () => [], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/import/url`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ url: imported.sourceUrl })
    });
    const payload = await response.json();
    assert.equal(response.status, 200);
    assert.deepEqual(payload.data.cards, imported.cards);
    assert.equal(payload.meta.source, "public_share_page");
    assert.equal(payload.meta.parser, "public_html");
    assert.equal(payload.meta.cardCount, 1);
  }, async () => imported);
});

test("public share import keeps explicit crawler error codes", async () => {
  await withServer(async () => [], async (baseUrl) => {
    const response = await fetch(`${baseUrl}/api/v1/import/url`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ url: "https://example.com/private" })
    });
    assert.equal(response.status, 409);
    assert.equal((await response.json()).code, "LOGIN_REQUIRED");
  }, async () => {
    throw Object.assign(new Error("這個分享頁需要登入"), {
      status: 409,
      code: "LOGIN_REQUIRED"
    });
  });
});
