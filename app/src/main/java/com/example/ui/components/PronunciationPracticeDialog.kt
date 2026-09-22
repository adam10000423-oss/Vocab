package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.entity.Flashcard
import java.io.File
import java.util.Locale

private enum class PronunciationAction { SCORE, RECORD }

@Composable
fun PronunciationPracticeDialog(
    card: Flashcard,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit = {},
    onResult: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var score by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf("按下「開始評分」並清楚念出單字。") }
    var requestedAction by remember { mutableStateOf<PronunciationAction?>(null) }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun finishRecognition(results: Bundle?) {
        listening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
        val heard = matches.firstOrNull().orEmpty().trim()
        recognizedText = heard
        val calculated = pronunciationScore(card.word, heard, confidence)
        score = calculated
        feedback = pronunciationFeedback(card, heard, calculated)
        onResult(calculated)
    }

    DisposableEffect(speechRecognizer, card.id) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; feedback = "正在聆聽…" }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { feedback = "正在判斷發音…" }
            override fun onError(error: Int) {
                listening = false
                feedback = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "沒有辨識到單字，請靠近麥克風再試一次。"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "沒有聽到聲音，請再試一次。"
                    else -> "語音辨識暫時失敗，請確認網路與系統語音服務。"
                }
            }
            override fun onResults(results: Bundle?) = finishRecognition(results)
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose {
            runCatching { speechRecognizer?.cancel() }
            runCatching { speechRecognizer?.destroy() }
            runCatching { recorder?.release() }
            runCatching { player?.release() }
            recordingFile?.delete()
        }
    }

    fun startScoring() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            feedback = "此裝置沒有可用的系統語音辨識服務。"
            return
        }
        recognizedText = ""
        score = null
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    fun startRecording() {
        runCatching {
            player?.release()
            player = null
            val file = File.createTempFile("pronunciation_", ".m4a", context.cacheDir)
            @Suppress("DEPRECATION")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else MediaRecorder()
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            recordingFile?.delete()
            recordingFile = file
            recording = true
            feedback = "正在錄下你的聲音…"
        }.onFailure {
            feedback = "無法開始錄音，請確認麥克風權限。"
        }
    }

    fun stopRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        recording = false
        feedback = "錄音完成，可輪流播放示範與自己的錄音比較。"
    }

    fun playRecording() {
        val file = recordingFile?.takeIf(File::isFile) ?: return
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completed -> completed.release(); if (player === completed) player = null }
                prepare()
                start()
            }
        }.onFailure { feedback = "無法播放這次錄音。" }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) feedback = "需要麥克風權限才能評分或錄音。"
        else permissionVersion += 1
    }

    fun request(action: PronunciationAction) {
        requestedAction = action
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(requestedAction, permissionVersion) {
        val action = requestedAction ?: return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return@LaunchedEffect
        }
        requestedAction = null
        when (action) {
            PronunciationAction.SCORE -> startScoring()
            PronunciationAction.RECORD -> startRecording()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (recording) stopRecording()
            onStopSpeaking()
            onDismiss()
        },
        title = { Text("發音練習", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(card.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (card.phonetic.isNotBlank()) Text(card.phonetic, color = MaterialTheme.colorScheme.primary)
                pronunciationStressHint(card.phonetic)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                score?.let {
                    Text(
                        "$it 分",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (it >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (recognizedText.isNotBlank()) {
                    Text("系統辨識為：$recognizedText", style = MaterialTheme.typography.bodyMedium)
                }
                Text(feedback, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSpeak(card.word) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Text("示範")
                    }
                    Button(
                        onClick = { if (listening) speechRecognizer?.cancel() else request(PronunciationAction.SCORE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                        Text(if (listening) "停止" else "開始評分")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (recording) stopRecording() else request(PronunciationAction.RECORD) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                        Text(if (recording) "停止錄音" else "錄下自己")
                    }
                    OutlinedButton(
                        onClick = ::playRecording,
                        enabled = recordingFile?.isFile == true && !recording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("播放我的")
                    }
                }
                Text(
                    "分數依 Android 語音辨識結果與信心值估算，適合練習比較，不等同專業音標診斷。低於 80 分會加入發音弱點，達 90 分可移除。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onStopSpeaking(); onDismiss() }) { Text("完成") }
        }
    )
}

internal fun pronunciationScore(target: String, heard: String, confidence: Float?): Int {
    val expected = target.lowercase().filter(Char::isLetter)
    val actualWords = heard.lowercase().split(Regex("[^a-z]+"))
        .map { it.filter(Char::isLetter) }
        .filter(String::isNotBlank)
    if (expected.isBlank() || actualWords.isEmpty()) return 0
    val similarity = actualWords.maxOf { word ->
        val distance = levenshteinDistance(expected, word)
        1f - distance.toFloat() / maxOf(expected.length, word.length, 1)
    }.coerceIn(0f, 1f)
    val confidenceFactor = confidence?.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: similarity
    return (similarity * 80f + confidenceFactor * 20f).toInt().coerceIn(0, 100)
}

private fun pronunciationFeedback(card: Flashcard, heard: String, score: Int): String {
    if (score >= 90) return "發音辨識非常接近目標，可以繼續下一個單字。"
    if (score >= 80) return "整體正確；再聽一次示範，注意重音與尾音會更自然。"
    val target = card.word.lowercase().filter(Char::isLetter)
    val actual = heard.lowercase().filter(Char::isLetter)
    val mismatchStart = target.indices.firstOrNull { index -> actual.getOrNull(index) != target[index] } ?: 0
    val focus = target.drop(mismatchStart).take(4).ifBlank { target.takeLast(3) }
    return "系統沒有完整辨識成 ${card.word}。建議放慢速度，特別加強「$focus」附近的音，並比較示範錄音。"
}

private fun pronunciationStressHint(phonetic: String): String? = when {
    'ˈ' in phonetic -> "重音提示：主要重音在 ˈ 後方的音節。"
    'ˌ' in phonetic -> "重音提示：ˌ 表示次重音，請再參考示範語音。"
    phonetic.isNotBlank() -> "音標未標示明確重音，請以示範語音為準。"
    else -> null
}

private fun levenshteinDistance(first: String, second: String): Int {
    if (first.isEmpty()) return second.length
    if (second.isEmpty()) return first.length
    var previous = IntArray(second.length + 1) { it }
    first.forEachIndexed { firstIndex, firstChar ->
        val current = IntArray(second.length + 1)
        current[0] = firstIndex + 1
        second.forEachIndexed { secondIndex, secondChar ->
            current[secondIndex + 1] = minOf(
                current[secondIndex] + 1,
                previous[secondIndex + 1] + 1,
                previous[secondIndex] + if (firstChar == secondChar) 0 else 1
            )
        }
        previous = current
    }
    return previous.last()
}
