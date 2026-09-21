import assert from "node:assert/strict";
import test from "node:test";
import { __test } from "../src/external-import.mjs";

test("parses modern Quizlet JSON nested inside Next.js state", () => {
  const educationQAndAData = {
    "@type": "Quiz",
    about: { name: "小魚U5" },
    hasPart: [
      {
        "@type": "Question",
        text: "active",
        acceptedAnswer: {
          "@type": "Answer",
          text: "adj./n. 活躍的；活潑的；積極的／積極分子；主動語態"
        }
      },
      {
        "@type": "Question",
        text: "advertise",
        acceptedAnswer: {
          "@type": "Answer",
          text: "vt./vi. 為……宣傳；公布"
        }
      }
    ]
  };
  const nextData = {
    props: {
      pageProps: {
        dehydratedReduxStateKey: JSON.stringify({
          educationQAndAData
        })
      }
    }
  };
  const html = `<script id="__NEXT_DATA__" type="application/json">${JSON.stringify(nextData)}</script>`;
  const cards = __test.cardsFromHtml(html, "quizlet");

  assert.equal(cards.length, 2);
  assert.equal(cards[0].word, "active");
  assert.match(cards[0].definition, /活躍的/);
  assert.equal(cards[1].word, "advertise");
});

test("parses Quizlet studiableItems cardSides and ignores UI placeholder pairs", () => {
  const nextData = {
    props: {
      pageProps: {
        state: JSON.stringify({
          misleadingUiLabels: { word: "詞語", definition: "定義" },
          studiableItems: [
            {
              id: 1,
              rank: 1,
              cardSides: [
                { label: "word", media: [{ plainText: "active" }] },
                { label: "definition", media: [{ plainText: "adj. 活躍的" }] }
              ]
            },
            {
              id: 2,
              rank: 0,
              cardSides: [
                { label: "word", media: [{ plainText: "advertise" }] },
                { label: "definition", media: [{ plainText: "vt. 宣傳" }] }
              ]
            },
            {
              id: 3,
              rank: 2,
              cardSides: [
                { label: "word", media: [{ plainText: "active" }] },
                { label: "definition", media: [{ plainText: "這個重複項目不應覆蓋第一張" }] }
              ]
            }
          ]
        })
      }
    }
  };
  const html = `<script id="__NEXT_DATA__" type="application/json">${JSON.stringify(nextData)}</script>`;
  const cards = __test.cardsFromHtml(html, "quizlet");

  assert.deepEqual(cards.map(({ word }) => word), ["advertise", "active"]);
  assert.equal(cards[1].definition, "adj. 活躍的");
});
