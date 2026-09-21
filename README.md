# Vocab

Vocab 是一套以 Kotlin、Jetpack Compose 與 Room 製作的開源 Android 單字學習 App，並附有選用的 AI Gateway。

## 主要功能

- 課程、資料夾與單字卡管理，支援排序、搬移、備份與還原
- 卡牌學習、SRS、測驗、配對、聽力與多種題型
- 相片、拍攝、掃描與文件匯入；可使用多模態 AI 擷取內容
- AI 補齊、AI 聊天與互動測驗；使用者可自行設定供應商、模型及 API Key
- TTS 發音、每日目標、通知、學習統計與 Android 桌面小工具
- GitHub Releases 版本檢查、APK 下載與系統安裝引導
- 全新安裝不含示範課程或示範單字

## 安裝 APK

1. 到 GitHub 專案右側的 **Releases** 開啟最新版本。
2. 下載 `Vocab-版本號.apk`。
3. 在 Android 開啟檔案，依系統提示允許該來源並確認安裝。

App 內可在「設定 → 更新」檢查 GitHub 最新版本。下載完成後會開啟 Android 系統安裝器；基於 Android 安全限制，安裝確認不能被 App 自動略過。若新舊 APK 使用相同簽章，系統會保留資料並覆蓋更新。安裝完成後可在系統安裝頁按「開啟」。

## 從原始碼執行 Android 版

需求：Android Studio、JDK 21、Android SDK 36。

1. 複製專案：`git clone https://github.com/adam10000423-oss/Vocab.git`
2. 用 Android Studio 開啟專案根目錄。
3. 等待 Gradle Sync 完成，選擇模擬器或 Android 手機。
4. 執行 `app`。

命令列也可執行：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Debug APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。

AI Gateway 預設為模擬器上的 `http://10.0.2.2:8787`，可在建置時覆寫：

```powershell
.\gradlew.bat assembleDebug -PBACKEND_BASE_URL=https://your-api.example.com
```

## Release 簽章與自動發布

Release 必須永遠使用同一把私密簽章金鑰，否則 Android 不允許覆蓋安裝。GitHub Actions 需要以下 Repository Secrets：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

本機建置則使用 `KEYSTORE_PATH`、`STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 環境變數。私鑰、密碼、API Key 與 APK 都已排除於 Git 之外。

先更新 `app/build.gradle.kts` 的 `versionCode` 與 `versionName`，再推送相同版本的標籤，例如 `v1.3.0`。工作流程會測試並建立 GitHub Release 與簽章 APK。

## 執行 AI Gateway

Gateway 使用 Node.js 內建模組，不需要第三方執行期套件：

```powershell
cd server
Copy-Item .env.example .env
# 編輯 .env 並加入自己的供應商金鑰
npm test
npm start
```

伺服器包含請求大小限制、逾時、公開網址檢查、重新導向限制及基本限流。請勿提交 `.env`。

## 隱私與安全

- Android 個人 AI Key 使用 Android Keystore 加密，不會包含在 JSON/CSV 備份。
- Android 系統雲端備份已停用；資料由使用者透過 App 內備份功能主動匯出。
- URL 匯入只接受公開 HTTPS 位址，不會繞過登入、CAPTCHA 或網站存取控制。
- GitHub 更新只讀取此專案的公開 Release，安裝仍由 Android 系統驗證與確認。

發現弱點時請依 [SECURITY.md](SECURITY.md) 私下回報，不要先建立公開 Issue。

## 授權

[MIT License](LICENSE)
