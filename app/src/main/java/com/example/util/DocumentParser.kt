package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.data.api.DirectAiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object DocumentParser {
    private const val TAG = "DocumentParser"
    private const val MAX_INPUT_BYTES = 25L * 1024L * 1024L

    /**
     * Extracts text from PDF, DOCX, and plain-text/CSV/JSON files.
     * Image URIs are intentionally rejected and must use the multimodal AI path.
     */
    suspend fun extractTextFromUri(context: Context, uri: Uri, maxPdfPages: Int = 20): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val uriString = uri.toString().lowercase()

        val isPdf = mimeType.contains("pdf", ignoreCase = true) || uriString.endsWith(".pdf")
        val isDocx = mimeType.contains("wordprocessingml", ignoreCase = true) || uriString.endsWith(".docx")
        val isLegacyDoc = mimeType.equals("application/msword", ignoreCase = true) ||
            uriString.endsWith(".doc")
        val isImage = mimeType.startsWith("image/", ignoreCase = true) ||
            listOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif").any(uriString::endsWith)

        val inputLength = runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        require(inputLength <= MAX_INPUT_BYTES || inputLength < 0L) {
            "檔案超過 25 MB，請先壓縮或分成較小檔案"
        }
        require(!isLegacyDoc) {
            "舊版 DOC 無法可靠解析，請另存為 DOCX 或 PDF 後再匯入"
        }
        require(!isImage) {
            "圖片必須使用設定中選擇的多模態 AI 直接辨識"
        }

        if (isPdf) {
            Log.d(TAG, "Extracting text from PDF document...")
            val pdfText = extractTextFromPdf(context, uri, maxPdfPages)
            if (pdfText.isNotBlank()) return@withContext pdfText
            return@withContext ""
        }

        if (isDocx) {
            val docxText = extractTextFromDocx(context, uri)
            if (docxText.isNotBlank()) return@withContext docxText
            return@withContext ""
        }

        // Try reading as plain text file (TXT, CSV, JSON, MD, etc.)
        val rawText = try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val text = String(bytes, Charsets.UTF_8)
                if (isReadableText(text)) text else ""
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading raw text from URI", e)
            ""
        }

        if (rawText.isNotBlank()) {
            return@withContext rawText
        }

        throw IllegalArgumentException("不支援此檔案格式，請使用 PDF、DOCX 或文字檔")
    }

    /**
     * Renders PDF pages for a multimodal model. This is used by AI chat so scanned
     * documents are sent as page images instead of silently becoming empty text.
     */
    suspend fun renderPdfPagesForAi(
        context: Context,
        uri: Uri,
        maxPages: Int = 5
    ): List<DirectAiService.ImagePayload> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("ai_chat_pdf_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use(input::copyTo)
            } ?: error("無法開啟 PDF")

            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = minOf(renderer.pageCount, maxPages.coerceIn(1, 5))
                    buildList {
                        for (index in 0 until pageCount) {
                            renderer.openPage(index).use { page ->
                                val scale = minOf(
                                    2f,
                                    2048f / page.width.toFloat(),
                                    2048f / page.height.toFloat()
                                )
                                val bitmap = Bitmap.createBitmap(
                                    (page.width * scale).toInt().coerceAtLeast(1),
                                    (page.height * scale).toInt().coerceAtLeast(1),
                                    Bitmap.Config.ARGB_8888
                                )
                                try {
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val bytes = ByteArrayOutputStream().use { output ->
                                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                                            "無法處理 PDF 第 ${index + 1} 頁"
                                        }
                                        output.toByteArray()
                                    }
                                    add(DirectAiService.ImagePayload(bytes, "image/jpeg"))
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            tempFile?.delete()
        }
    }

    private fun extractTextFromDocx(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use zipUse@{ zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return@zipUse xml
                            .replace(Regex("</w:p>"), "\n")
                            .replace(Regex("</w:tab>"), "\t")
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .trim()
                    }
                    zip.closeEntry()
                }
                ""
            }
        }.orEmpty()
    }.getOrElse {
        Log.e(TAG, "Failed reading DOCX", it)
        ""
    }

    private fun isReadableText(text: String): Boolean {
        if (text.isBlank()) return false
        val printableCount = text.count { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?'\"()-=+|/\t\n\r" }
        return (printableCount.toDouble() / text.length.toDouble()) > 0.65
    }

    private fun extractTextFromPdf(context: Context, uri: Uri, maxPdfPages: Int): String {
        val fullTextBuilder = StringBuilder()
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("pdf_import_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    try {
                        val maxPages = minOf(renderer.pageCount, maxPdfPages.coerceIn(1, 50))
                        for (i in 0 until maxPages) {
                            renderer.openPage(i).use { page ->
                                val scale = minOf(
                                    2f,
                                    2048f / page.width.toFloat(),
                                    2048f / page.height.toFloat()
                                )
                                val width = (page.width * scale).toInt().coerceAtLeast(1)
                                val height = (page.height * scale).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                try {
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val visionText = Tasks.await(
                                        recognizer.process(InputImage.fromBitmap(bitmap, 0))
                                    )
                                    if (visionText.text.isNotBlank()) {
                                        fullTextBuilder.append(visionText.text).append("\n\n")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error OCR on PDF page $i", e)
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    } finally {
                        recognizer.close()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed rendering PDF", e)
        } finally {
            tempFile?.delete()
        }
        return fullTextBuilder.toString()
    }

    /**
     * Extracts candidates locally and immediately. AI enrichment is an explicit
     * user action in the preview screen, so an unavailable API never blocks
     * camera or document parsing.
     */
    suspend fun parseDocumentToCards(
        context: Context,
        uri: Uri,
        maxPdfPages: Int = 20
    ): Pair<String, List<OcrCardCandidate>> = withContext(Dispatchers.IO) {
        val rawText = extractTextFromUri(context, uri, maxPdfPages)
        require(rawText.isNotBlank()) {
            "沒有讀取到文字；請確認影像清晰、方向正確且文字沒有被裁切"
        }

        val localCandidates = OcrWordParser.parseTextToCards(rawText, 0L)
        val finalCandidates = OcrWordParser.enrichCandidatesWithDictionary(localCandidates)
        return@withContext Pair(rawText, finalCandidates)
    }
}
