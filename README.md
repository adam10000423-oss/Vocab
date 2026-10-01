<div align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_launcher_generated.png" width="132" alt="Vocab App 圖示">

  <h1>Vocab</h1>

  <p><strong>把單字、文法、文章閱讀與英文寫作整合在同一個 Android 學習空間。</strong></p>
  <p>本機優先、可自行設定 AI，適合建立自己的英文教材與複習流程。</p>

  <p>
    <a href="https://github.com/adam10000423-oss/Vocab/releases/latest"><img alt="最新版本" src="https://img.shields.io/github/v/release/adam10000423-oss/Vocab?display_name=tag&style=flat-square&label=Release&color=2196F3"></a>
    <a href="https://github.com/adam10000423-oss/Vocab/releases"><img alt="下載次數" src="https://img.shields.io/github/downloads/adam10000423-oss/Vocab/total?style=flat-square&label=Downloads&color=43A047"></a>
    <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/github/license/adam10000423-oss/Vocab?style=flat-square&label=License&color=FF7043"></a>
    <a href="https://github.com/adam10000423-oss/Vocab/actions/workflows/verify.yml"><img alt="建置狀態" src="https://img.shields.io/github/actions/workflow/status/adam10000423-oss/Vocab/verify.yml?branch=main&style=flat-square&label=Build"></a>
    <img alt="Android 7.0+" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white">
  </p>

  <p>
    <a href="https://github.com/adam10000423-oss/Vocab/releases/latest"><img alt="前往 Releases 下載最新版 APK" src="https://img.shields.io/badge/%E4%B8%8B%E8%BC%89%E6%9C%80%E6%96%B0%E7%89%88_APK-GitHub_Releases-1976D2?style=for-the-badge&logo=github&logoColor=white"></a>
  </p>

  <p>
    <a href="#download">下載與安裝</a> ·
    <a href="#features">功能</a> ·
    <a href="#getting-started">開始使用</a> ·
    <a href="#ai">AI 設定</a> ·
    <a href="#development">開發</a> ·
    <a href="CHANGELOG.md">更新日誌</a>
  </p>
</div>

---

Vocab 是使用 Kotlin、Jetpack Compose 與 Room 製作的開源 Android 英文學習 App。它不是預先塞滿固定教材的課程平台，而是一個讓使用者整理自己的單字、文法、文章與作文紀錄，再透過學習、測驗和 AI 工具持續複習的個人學習系統。

主要學習資料保存在裝置本機。AI 是選用功能：不設定 API Key，仍可使用單字卡、文法筆記、學習、測驗、統計、備份等本機功能。

### 3.0 介面與操作

- 首頁左上角的「單字／文法」膠囊可切換學習區域；其他頁面顯示目前頁面的名稱。
- 底部五個入口使用透明懸浮工作列，點擊或左右滑動可切換頁面。內容能延伸到工作列後方，清單尾端仍可捲動至完整顯示。
- 「設定」主頁列出各功能分類，點擊後進入專屬設定頁，左上角返回鍵可回到設定主頁。
- 「設定 → 外觀」只提供跟隨系統、亮色、暗色。背景、字體顏色與主色由 App 統一配置。
- 「設定 → 發音與提醒」可啟用背景朗讀與浮動視窗；視窗採用黑白配色，拖曳可移動，翻面與下一張卡會保留位置，點擊可回到 App。

<a id="download"></a>
## 下載與安裝

### 直接安裝 APK

1. 開啟 **[最新版本下載頁](https://github.com/adam10000423-oss/Vocab/releases/latest)**。
2. 在 Assets 下載 `Vocab-v版本號.apk`。
3. 在 Android 手機開啟 APK。
4. 若系統詢問，允許目前使用的瀏覽器或檔案管理器「安裝未知應用程式」。
5. 回到安裝畫面並確認安裝。

需求與注意事項：

- 最低支援 Android 7.0（API 24）。
- 文件掃描使用 Google Play 服務提供的 ML Kit 掃描器；部分沒有 Google Play 服務的裝置可能無法使用該入口，但仍可從相簿或檔案匯入。
- 更新時必須安裝使用相同簽章的正式 APK，Android 才能保留既有資料並覆蓋更新。
- Android 的安裝確認畫面屬於系統安全機制，App 無法替使用者自動按下「安裝」。

### App 內更新

進入「設定 → 更新」即可：

- 檢查 GitHub Releases 的最新版本。
- 選擇是否自動檢查，以及是否只在 Wi-Fi 下更新。
- 下載 APK 並開啟 Android 系統安裝器。
- 查看目前安裝的版本號與更新日誌。

<a id="features"></a>
## 功能總覽

### 單字庫與課程管理

- 使用「課程 → 資料夾 → 單字卡」整理教材。
- 建立、重新命名、搬移、刪除及拖曳排序資料夾與卡片。
- 單字卡可保存英文、中文解釋、詞性、音標、中英文例句與熟練狀態。
- 依課程或資料夾瀏覽，並搜尋自己已保存的單字。
- 從搜尋結果直接跳到單字所在位置；編輯時可保留目前瀏覽位置。
- 內建資料品質檢查，用來發現重複單字、缺漏欄位、格式不一致與可能的拼字問題。

### 單字查詢與匯入

- 支援輸入英文查中文，也可輸入中文尋找英文結果。
- 可選擇自動模式、Google 翻譯、Cambridge、Wiktionary 與 Free Dictionary 等來源；來源是否可用仍受對方網站服務與連線限制影響。
- 查詢結果可先用 AI 補齊音標、詞性、解釋與例句，再加入指定資料夾。
- 支援拍照、相簿、多頁文件掃描與檔案匯入。
- 支援貼上公開網址、文字，以及可辨識的 PDF、DOCX、TXT、CSV、TSV、JSON 等內容。
- 匯入前可預覽、編輯、AI 補齊、取消個別項目，再一次存入指定課程與資料夾。

### 單字學習與測驗

- 翻卡學習與間隔複習（SRS）。
- 依「記得／不熟」更新學習狀態，並可在結束後繼續練習錯題或不熟單字。
- 多種測驗方式，包含選擇、輸入、配對、聽力及口說練習。
- 口說可分為念單字與念例句，先播放示範，再錄音辨識與評分。
- 學習卡正面、背面可個別設定朗讀欄位、拼字與重複次數。
- 自動播放會等待目前卡片的語音完整結束才換頁；手動切換時會停止上一張卡片的播放。

### 文法學習系統

- 文法資料使用獨立的「課程 → 文法庫 → 文法筆記」結構，不會混用單字資料夾。
- 一篇筆記可包含核心概念、多組句型、使用時機、注意事項、多個中英文例句、補充資料與多題練習。
- 編輯器以「基本、句型、題目、補充」分頁整理長內容。
- 文法庫支援搜尋、拖曳排序、學習進度與錯題複習。
- 文法測驗支援概念題、選擇題、填空題，以及介系詞、動詞形式等句型挖空練習。
- 可從文字、圖片、拍照、掃描、PDF、文件、AI 主題或 `.vocabshare` 建立文法資料。
- 多模態 AI 會優先保留教材內已有的句型與例句，辨識不清的部分會標示或留空，儲存前仍由使用者預覽確認。

### 互動文章閱讀

- 可依指定單字資料夾生成文章，也能直接輸入自訂主題、程度與文章類型。
- 支援貼上文字、拍照、文件掃描、相簿與檔案匯入文章。
- 文章建立後自動保存在本機，可從文章清單繼續閱讀或確認後刪除。
- 點擊文章中的英文單字會開啟翻卡，顯示發音、詞性、中文解釋與中英文例句。
- 尚未存在單字庫的單字可即時查詢完整資料，並選擇要加入的資料夾。
- 可朗讀文章，並從文章內容建立理解測驗。

### 英文作文與寫作檢查

- 可自由輸入作文，或從拍照、掃描、相簿與文件辨識原稿。
- AI 檢查以逐項問題呈現原句、建議修正、規則與繁體中文說明。
- 保留原始作文、修改後全文、批改項目、課程與標題。
- 每篇作文有獨立詳情頁，可切換批改總覽、原始作文、修改後作文及逐項批改。
- 使用者可選擇套用哪些修改；接受的問題可整理成「寫作弱點」文法筆記，供後續學習與測驗。

### AI 助手與自動整理

- AI 聊天可讀取使用者選擇的多個資料夾與文法內容，協助解釋、出題或整理學習資料。
- AI 寫入動作會先顯示待確認內容，使用者確認後才修改本機資料。
- 可批次補齊單字、生成文法筆記、產生文章、檢查作文與分析資料品質。
- 可建立多組 API 設定，個別測試不同供應商與模型。
- 支援 Google Gemini、OpenRouter、OpenAI 與 Anthropic Claude；實際可用模型依供應商帳號與 API 規則而定。

### 學習紀錄與裝置整合

- 每日學習目標、待複習數、連續天數與學習日曆。
- 學習進度、錯題及熟練度會保存於本機資料庫。
- 可設定學習提醒。
- 提供今日進度、每日單字、待複習與連續學習等 Android 桌面小工具。
- 介面支援手機直式與橫式安全區，避免被系統導覽列遮住。

### 備份、匯出與分享資料

- 建立完整備份並在其他安裝中還原。
- 匯出 CSV 供試算表或其他工具使用。
- `.vocabshare` 用於攜帶可分享的學習內容，不包含 API Key、聊天紀錄與私人學習紀錄。
- 刪除重要資料前會要求再次確認。

<a id="getting-started"></a>
## 建議的首次使用流程

1. **建立課程**：例如「高中英文第一冊」或「TOEIC」。
2. **建立資料夾／文法庫**：依課次、主題或來源分類。
3. **加入內容**：手動輸入、查詢、貼上文字，或使用拍照與掃描匯入。
4. **檢查預覽**：確認 AI 或辨識結果，修正不確定的文字後再儲存。
5. **開始學習**：先使用翻卡建立印象，再進入測驗。
6. **回顧錯題**：完成測驗後繼續練習錯題，或從學習日曆查看累積狀況。
7. **定期備份**：在「設定 → 資料」建立完整備份並保存在安全位置。

<a id="ai"></a>
## AI 連線設定

AI 功能需要使用者自行提供支援供應商的 API Key，或自行部署選用的 Vocab Gateway。專案不附贈 API 額度，也不要求所有使用者共用作者帳號。

### 使用個人 API Key

1. 開啟「設定 → AI」，找到「AI 連線」。
2. 選擇供應商。
3. 依 App 內教學前往供應商官方網站建立 API Key。
4. 貼上 Key、選擇模型並儲存。
5. 在該組設定按「測試這組」，確認連線成功。
6. 若有多組 Key，可分別測試並設定優先順序。

安全原則：

- 個人 API Key 會使用 Android Keystore 保護並保存在目前裝置。
- API Key 不會寫入完整備份、CSV、`.vocabshare`、聊天內容或 Git 儲存庫。
- 不要把 Key 貼進 Issue、聊天訊息、螢幕截圖或公開文件。
- API 的費用、速率限制、可用區域與內容政策由各供應商決定。

### 自行部署 Vocab Gateway

Gateway 使用 Node.js 內建模組，不需要第三方執行期套件。Node.js 需為 20 以上版本。

```powershell
cd server
Copy-Item .env.example .env
# 編輯 .env，填入要由伺服器使用的供應商設定
npm test
npm start
```

Android Debug 版預設連到模擬器主機上的 `http://10.0.2.2:8787`。可在建置時替換：

```powershell
.\gradlew.bat assembleDebug -PBACKEND_BASE_URL=https://your-api.example.com
```

若公開部署 Gateway，請自行設定 HTTPS、來源限制、秘密管理、流量控管與監控。伺服器已包含基本的請求大小限制、逾時、公開網址驗證、重新導向限制及限流，但這些不能取代正式部署環境的安全措施。

## 隱私與安全

- 學習資料預設保存在裝置上的 Room 資料庫。
- Android 系統雲端備份已停用；備份由使用者在 App 內主動建立。
- AI 功能只在使用者操作時，把完成該工作所需的文字或圖片送往所選供應商。
- URL 匯入只接受公開 HTTPS 位址，不會繞過登入、CAPTCHA 或網站存取控制。
- App 內更新只讀取本專案公開的 GitHub Release；安裝仍由 Android 系統驗證與確認。
- 發現安全問題時，請依 [SECURITY.md](SECURITY.md) 私下回報，不要先建立公開 Issue。

<a id="development"></a>
## 從原始碼建置

### 環境需求

- Android Studio（支援目前 Android Gradle Plugin）
- JDK 21
- Android SDK 36
- Git
- Node.js 20+（只有執行 Gateway 或 Gateway 測試時需要）

### 取得專案

```powershell
git clone https://github.com/adam10000423-oss/Vocab.git
cd Vocab
```

使用 Android Studio 開啟專案根目錄，等待 Gradle Sync 完成，選擇 Android 裝置後執行 `app`。

命令列驗證：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Debug APK 位於：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Gateway 測試：

```powershell
cd server
npm test
```

### 主要技術

| 類別 | 技術 |
| --- | --- |
| 語言 | Kotlin |
| UI | Jetpack Compose、Material 3 |
| 架構元件 | ViewModel、Kotlin Coroutines、Flow |
| 導覽 | Navigation Compose |
| 本機資料 | Room、DataStore |
| 網路與序列化 | OkHttp、Retrofit、Moshi |
| 文件掃描 | Google Play services ML Kit Document Scanner |
| 測試 | JUnit、Robolectric、Roborazzi |
| 選用後端 | Node.js Vocab Gateway |
| CI/CD | GitHub Actions |

### 專案結構

```text
app/
├─ src/main/java/com/example/
│  ├─ data/          # Room、Repository、AI、辭典、匯入與備份
│  ├─ ui/            # Compose 元件、畫面、主題與導覽
│  ├─ util/          # 文件解析、更新與共用工具
│  ├─ viewmodel/     # 畫面狀態與主要應用流程
│  └─ widgets/       # Android 桌面小工具
├─ src/main/res/     # 圖示、字型、字串與 Android 資源
└─ src/test/         # 單元與畫面測試

server/
├─ src/              # 選用 AI Gateway
└─ test/             # Gateway 測試

.github/workflows/   # 驗證與正式發布流程
```

## 正式版本與簽章

Android 只允許使用相同 application ID 與相同簽章的 APK 覆蓋更新。正式發布前需更新 `app/build.gradle.kts` 中的 `versionCode` 與 `versionName`。

GitHub Actions 的 Release 工作流程需要以下 Repository Secrets：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

本機正式建置使用：

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

推送與版本相同的標籤（例如 `v2.10.2`）後，工作流程會執行測試、建立簽章 APK，並把 APK 上傳到 GitHub Releases。私鑰、密碼、API Key 與建置產物不得提交進 Git。

## 疑難排解

### 文件掃描顯示不支援或無法開啟

- 更新 Google Play 服務。
- 確認裝置有可用的 Google Play 服務與足夠儲存空間。
- 若裝置無法使用 ML Kit 掃描器，改用「拍照」、「相片」或「檔案」匯入。

### AI 測試失敗

- 確認 API Key 沒有多餘空白且仍有效。
- 確認選擇的模型存在，並且帳號有權使用。
- 檢查供應商的額度、速率限制、地區限制與服務狀態。
- 分別測試每組 API 設定，以找出失效的 Key 或模型。

### 更新後無法覆蓋安裝

- 確認 APK 來自本專案的正式 Releases。
- 若曾安裝其他簽章的自行編譯版，Android 會拒絕覆蓋；請先匯出備份，再移除舊版並安裝正式版。
- 請勿在未備份資料前直接清除 App 資料或解除安裝。

## 更新日誌與回報

- [完整更新日誌](CHANGELOG.md)
- [最新 GitHub Release](https://github.com/adam10000423-oss/Vocab/releases/latest)
- [問題回報](https://github.com/adam10000423-oss/Vocab/issues)
- [安全性回報方式](SECURITY.md)

## 授權

本專案採用 [MIT License](LICENSE)。
