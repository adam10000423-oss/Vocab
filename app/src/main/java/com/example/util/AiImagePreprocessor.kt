package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.example.data.api.DirectAiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object AiImagePreprocessor {
    private const val MAX_DIMENSION = 2_048
    private const val MAX_ENCODED_BYTES = 8 * 1024 * 1024

    suspend fun prepare(
        context: Context,
        uri: Uri
    ): DirectAiService.ImagePayload = withContext(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val scale = (MAX_DIMENSION.toFloat() / max(width, height)).coerceAtMost(1f)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            decodeLegacyBitmap(context, uri)
        }

        try {
            var quality = 90
            var bytes: ByteArray
            do {
                bytes = ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                        "無法壓縮圖片"
                    }
                    output.toByteArray()
                }
                quality -= 10
            } while (bytes.size > MAX_ENCODED_BYTES && quality >= 50)

            require(bytes.size <= MAX_ENCODED_BYTES) {
                "圖片處理後仍超過 8 MB，請裁切後再試"
            }
            DirectAiService.ImagePayload(bytes = bytes, mimeType = "image/jpeg")
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeLegacyBitmap(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "圖片格式無法讀取" }

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_DIMENSION * 2 ||
            bounds.outHeight / sampleSize > MAX_DIMENSION * 2
        ) {
            sampleSize *= 2
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } ?: error("圖片內容無法讀取")

        val scale = (MAX_DIMENSION.toFloat() / max(decoded.width, decoded.height))
            .coerceAtMost(1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val rotation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                when (ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        if (rotation == 0f) return scaled

        return Bitmap.createBitmap(
            scaled,
            0,
            0,
            scaled.width,
            scaled.height,
            Matrix().apply { postRotate(rotation) },
            true
        ).also { if (it !== scaled) scaled.recycle() }
    }
}
