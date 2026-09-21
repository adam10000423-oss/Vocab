package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsVoiceOption(
    val name: String,
    val label: String,
    val localeTag: String,
    val requiresNetwork: Boolean,
    val quality: Int
)

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private data class PendingSpeech(
        val text: String,
        val rate: Float,
        val voiceName: String,
        val voiceStyle: String
    )

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingSpeech: PendingSpeech? = null
    private val _voices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val voices: StateFlow<List<TtsVoiceOption>> = _voices.asStateFlow()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        val result = engine.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        isInitialized = true
        val englishVoices = runCatching {
            engine.voices.orEmpty()
                .filter { it.locale.language.equals("en", ignoreCase = true) }
                .sortedWith(
                    compareByDescending<Voice> { it.locale == Locale.US }
                        .thenByDescending { it.quality }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenBy { it.name }
                )
        }.getOrDefault(emptyList())
        _voices.value = englishVoices.map { voice ->
            TtsVoiceOption(
                name = voice.name,
                label = buildString {
                    append(voice.locale.displayName)
                    append(if (voice.isNetworkConnectionRequired) "・線上高品質" else "・裝置內建")
                    if (voice.quality >= Voice.QUALITY_HIGH) append("・高品質")
                },
                localeTag = voice.locale.toLanguageTag(),
                requiresNetwork = voice.isNetworkConnectionRequired,
                quality = voice.quality
            )
        }
        chooseBestVoice(engine, englishVoices)
        pendingSpeech?.let { pending ->
            pendingSpeech = null
            speak(pending.text, pending.rate, pending.voiceName, pending.voiceStyle)
        }
    }

    fun speak(
        text: String,
        rate: Float = 0.9f,
        voiceName: String = "",
        voiceStyle: String = "NATURAL"
    ) {
        val normalized = text.trim()
        if (
            normalized.isBlank() ||
            normalized.none { it in 'A'..'Z' || it in 'a'..'z' } ||
            normalized.any {
                it.isLetter() &&
                    Character.UnicodeScript.of(it.code) != Character.UnicodeScript.LATIN
            }
        ) return
        if (!isInitialized) {
            pendingSpeech = PendingSpeech(normalized, rate, voiceName, voiceStyle)
            return
        }
        val engine = tts ?: return
        selectVoice(engine, voiceName)
        engine.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
        engine.setPitch(
            when (voiceStyle) {
                "MALE" -> 0.84f
                "FEMALE" -> 1.10f
                else -> 1.0f
            }
        )
        engine.speak(normalized, TextToSpeech.QUEUE_FLUSH, null, "vocab_tts_id")
    }

    private fun selectVoice(engine: TextToSpeech, voiceName: String) {
        val available = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
        val selected = available.firstOrNull { it.name == voiceName }
            ?: available
                .filter { it.locale.language == "en" }
                .maxWithOrNull(
                    compareBy<Voice> { it.locale == Locale.US }
                        .thenBy { it.quality }
                        .thenBy { !it.isNetworkConnectionRequired }
                )
        selected?.let { engine.voice = it }
    }

    private fun chooseBestVoice(engine: TextToSpeech, voices: List<Voice>) {
        voices.firstOrNull()?.let { engine.voice = it }
        engine.setSpeechRate(0.9f)
        engine.setPitch(1f)
    }

    fun stop() {
        pendingSpeech = null
        tts?.stop()
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isInitialized = false
        pendingSpeech = null
        _voices.value = emptyList()
    }
}
