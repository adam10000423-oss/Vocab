package com.example.data.settings

import android.content.Context
import com.example.data.api.AiPromptDefaults
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("vocab_pulse_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val dataInitialized = booleanPreferencesKey("data_initialized")
        val dailyGoalCards = intPreferencesKey("daily_goal_cards")
        val themeMode = stringPreferencesKey("theme_mode")
        val themeColorPreset = stringPreferencesKey("theme_color_preset")
        val customPrimaryColor = stringPreferencesKey("custom_primary_color")
        val customSecondaryColor = stringPreferencesKey("custom_secondary_color")
        val backgroundBrightness = floatPreferencesKey("background_brightness")
        val backgroundOpacity = floatPreferencesKey("background_opacity")
        val gradientEnabled = booleanPreferencesKey("gradient_enabled")
        val gradientStartColor = stringPreferencesKey("gradient_start_color")
        val gradientEndColor = stringPreferencesKey("gradient_end_color")
        val fontFamily = stringPreferencesKey("font_family")
        val englishFontFamily = stringPreferencesKey("english_font_family")
        val fontScale = floatPreferencesKey("font_scale")
        val customTextColorEnabled = booleanPreferencesKey("custom_text_color_enabled")
        val customTextColor = stringPreferencesKey("custom_text_color")
        val advancedSrs = booleanPreferencesKey("advanced_srs")
        val dailyNewCardLimit = intPreferencesKey("daily_new_card_limit")
        val dailyReviewLimit = intPreferencesKey("daily_review_limit")
        val aiEnabled = booleanPreferencesKey("ai_enabled")
        val aiRequiresConfirmation = booleanPreferencesKey("ai_requires_confirmation")
        val usePersonalAiApi = booleanPreferencesKey("use_personal_ai_api")
        val aiProvider = stringPreferencesKey("ai_provider")
        val aiModel = stringPreferencesKey("ai_model")
        val aiChatStyle = stringPreferencesKey("ai_chat_style")
        val aiWordPrompt = stringPreferencesKey("ai_word_prompt")
        val aiImagePrompt = stringPreferencesKey("ai_image_prompt")
        val ocrPreviewBeforeImport = booleanPreferencesKey("ocr_preview_before_import")
        val pdfPageLimit = intPreferencesKey("pdf_page_limit")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val speechRate = floatPreferencesKey("speech_rate")
        val ttsVoiceStyle = stringPreferencesKey("tts_voice_style")
        val ttsVoiceName = stringPreferencesKey("tts_voice_name")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val reminderTimes = stringPreferencesKey("reminder_times")
        val gameMistakesToReview = booleanPreferencesKey("game_mistakes_to_review")
        val autoCheckUpdates = booleanPreferencesKey("auto_check_updates")
        val wifiOnlyUpdates = booleanPreferencesKey("wifi_only_updates")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { value ->
        val legacyReminder = ReminderTime(
            hour = (value[Keys.reminderHour] ?: 20).coerceIn(0, 23),
            minute = (value[Keys.reminderMinute] ?: 0).coerceIn(0, 59)
        )
        val reminderTimes = value[Keys.reminderTimes]
            ?.split(',')
            ?.mapNotNull(::parseReminderTime)
            ?.distinct()
            ?.sorted()
            ?.take(12)
            ?.ifEmpty { null }
            ?: listOf(legacyReminder)
        AppSettings(
            onboardingCompleted = value[Keys.onboardingCompleted] ?: false,
            dataInitialized = value[Keys.dataInitialized] ?: false,
            dailyGoalCards = (value[Keys.dailyGoalCards] ?: 20).coerceIn(5, 100),
            themeMode = value[Keys.themeMode] ?: "SYSTEM",
            themeColorPreset = value[Keys.themeColorPreset] ?: "GREEN",
            customPrimaryColor = value[Keys.customPrimaryColor] ?: "#39796D",
            customSecondaryColor = value[Keys.customSecondaryColor] ?: "#5B7482",
            backgroundBrightness = (value[Keys.backgroundBrightness] ?: 1f).coerceIn(0.55f, 1.35f),
            backgroundOpacity = (value[Keys.backgroundOpacity] ?: 1f).coerceIn(0.55f, 1f),
            gradientEnabled = value[Keys.gradientEnabled] ?: false,
            gradientStartColor = value[Keys.gradientStartColor] ?: "#DFF5EC",
            gradientEndColor = value[Keys.gradientEndColor] ?: "#DCEBFA",
            fontFamily = value[Keys.fontFamily] ?: "DEFAULT",
            englishFontFamily = value[Keys.englishFontFamily] ?: "DEFAULT",
            fontScale = (value[Keys.fontScale] ?: 1f).coerceIn(0.85f, 1.3f),
            customTextColorEnabled = value[Keys.customTextColorEnabled] ?: false,
            customTextColor = value[Keys.customTextColor] ?: "#202522",
            advancedSrs = value[Keys.advancedSrs] ?: false,
            dailyNewCardLimit = value[Keys.dailyNewCardLimit] ?: 20,
            dailyReviewLimit = value[Keys.dailyReviewLimit] ?: 100,
            aiEnabled = value[Keys.aiEnabled] ?: true,
            aiRequiresConfirmation = value[Keys.aiRequiresConfirmation] ?: true,
            usePersonalAiApi = value[Keys.usePersonalAiApi] ?: false,
            aiProvider = value[Keys.aiProvider] ?: "GEMINI",
            aiModel = value[Keys.aiModel] ?: "gemini-3.6-flash",
            aiChatStyle = value[Keys.aiChatStyle]
                ?.takeIf { it in setOf("NORMAL", "RELAXED", "STRICT") }
                ?: "NORMAL",
            aiWordPrompt = value[Keys.aiWordPrompt] ?: AiPromptDefaults.WORD_DETAILS,
            aiImagePrompt = value[Keys.aiImagePrompt] ?: AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION,
            ocrPreviewBeforeImport = value[Keys.ocrPreviewBeforeImport] ?: true,
            pdfPageLimit = (value[Keys.pdfPageLimit] ?: 20).coerceIn(1, 50),
            autoSpeak = value[Keys.autoSpeak] ?: false,
            speechRate = (value[Keys.speechRate] ?: 1f).coerceIn(0.5f, 1.5f),
            ttsVoiceStyle = value[Keys.ttsVoiceStyle] ?: "NATURAL",
            ttsVoiceName = value[Keys.ttsVoiceName] ?: "",
            remindersEnabled = value[Keys.remindersEnabled] ?: false,
            reminderHour = (value[Keys.reminderHour] ?: 20).coerceIn(0, 23),
            reminderMinute = (value[Keys.reminderMinute] ?: 0).coerceIn(0, 59),
            reminderTimes = reminderTimes,
            gameMistakesToReview = value[Keys.gameMistakesToReview] ?: true,
            autoCheckUpdates = value[Keys.autoCheckUpdates] ?: true,
            wifiOnlyUpdates = value[Keys.wifiOnlyUpdates] ?: true,
            reduceMotion = value[Keys.reduceMotion] ?: false
        )
    }

    suspend fun setBoolean(name: String, enabled: Boolean) = context.settingsDataStore.edit {
        val key = when (name) {
            "onboardingCompleted" -> Keys.onboardingCompleted
            "dataInitialized" -> Keys.dataInitialized
            "advancedSrs" -> Keys.advancedSrs
            "aiEnabled" -> Keys.aiEnabled
            "aiRequiresConfirmation" -> Keys.aiRequiresConfirmation
            "usePersonalAiApi" -> Keys.usePersonalAiApi
            "ocrPreviewBeforeImport" -> Keys.ocrPreviewBeforeImport
            "autoSpeak" -> Keys.autoSpeak
            "remindersEnabled" -> Keys.remindersEnabled
            "gameMistakesToReview" -> Keys.gameMistakesToReview
            "autoCheckUpdates" -> Keys.autoCheckUpdates
            "wifiOnlyUpdates" -> Keys.wifiOnlyUpdates
            "reduceMotion" -> Keys.reduceMotion
            "gradientEnabled" -> Keys.gradientEnabled
            "customTextColorEnabled" -> Keys.customTextColorEnabled
            else -> error("Unknown boolean setting: $name")
        }
        it[key] = enabled
    }

    suspend fun setThemeMode(mode: String) = context.settingsDataStore.edit {
        it[Keys.themeMode] = mode.takeIf { value -> value in setOf("SYSTEM", "LIGHT", "DARK") } ?: "SYSTEM"
    }

    suspend fun setThemeColorPreset(preset: String) = context.settingsDataStore.edit {
        it[Keys.themeColorPreset] = preset.take(20)
    }

    suspend fun setCustomThemeColors(primary: String, secondary: String) =
        context.settingsDataStore.edit {
            it[Keys.customPrimaryColor] = primary.take(9)
            it[Keys.customSecondaryColor] = secondary.take(9)
        }

    suspend fun setGradientColors(start: String, end: String) =
        context.settingsDataStore.edit {
            it[Keys.gradientStartColor] = start.take(9)
            it[Keys.gradientEndColor] = end.take(9)
        }

    suspend fun setCustomTextColor(color: String) = context.settingsDataStore.edit {
        it[Keys.customTextColor] = color.take(9)
    }

    suspend fun setBackgroundAppearance(brightness: Float, opacity: Float) =
        context.settingsDataStore.edit {
            it[Keys.backgroundBrightness] = brightness.coerceIn(0.55f, 1.35f)
            it[Keys.backgroundOpacity] = opacity.coerceIn(0.55f, 1f)
        }

    suspend fun setFontAppearance(chineseFamily: String, englishFamily: String, scale: Float) =
        context.settingsDataStore.edit {
            it[Keys.fontFamily] = chineseFamily.takeIf { value ->
                value in setOf("DEFAULT", "ROUNDED", "SANS_SERIF", "SERIF", "CURSIVE", "MONOSPACE")
            } ?: "DEFAULT"
            it[Keys.englishFontFamily] = englishFamily.takeIf { value ->
                value in setOf("DEFAULT", "INTER", "NUNITO", "PLAYFAIR", "CAVEAT", "JETBRAINS_MONO")
            } ?: "DEFAULT"
            it[Keys.fontScale] = scale.coerceIn(0.85f, 1.3f)
        }

    suspend fun setAiProvider(provider: String, defaultModel: String) =
        context.settingsDataStore.edit {
            it[Keys.aiProvider] = provider
            it[Keys.aiModel] = defaultModel.take(120)
        }

    suspend fun setAiModel(model: String) = context.settingsDataStore.edit {
        it[Keys.aiModel] = model.trim().take(120)
    }

    suspend fun setAiChatStyle(style: String) = context.settingsDataStore.edit {
        it[Keys.aiChatStyle] = style.takeIf {
            value -> value in setOf("NORMAL", "RELAXED", "STRICT")
        } ?: "NORMAL"
    }

    suspend fun setAiWordPrompt(prompt: String) = context.settingsDataStore.edit {
        it[Keys.aiWordPrompt] = prompt.trim().take(4_000).ifBlank { AiPromptDefaults.WORD_DETAILS }
    }

    suspend fun setAiImagePrompt(prompt: String) = context.settingsDataStore.edit {
        it[Keys.aiImagePrompt] = prompt.trim().take(4_000)
            .ifBlank { AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION }
    }

    suspend fun setSpeechRate(rate: Float) = context.settingsDataStore.edit {
        it[Keys.speechRate] = rate.coerceIn(0.5f, 1.5f)
    }

    suspend fun setTtsVoiceStyle(style: String) = context.settingsDataStore.edit {
        it[Keys.ttsVoiceStyle] = style.takeIf { value ->
            value in setOf("NATURAL", "FEMALE", "MALE")
        } ?: "NATURAL"
    }

    suspend fun setTtsVoiceName(name: String) = context.settingsDataStore.edit {
        it[Keys.ttsVoiceName] = name.trim().take(200)
    }

    suspend fun setReminderTime(hour: Int, minute: Int) = context.settingsDataStore.edit {
        it[Keys.reminderHour] = hour.coerceIn(0, 23)
        it[Keys.reminderMinute] = minute.coerceIn(0, 59)
        it[Keys.reminderTimes] = serializeReminderTimes(listOf(ReminderTime(hour, minute)))
    }

    suspend fun setReminderTimes(times: List<ReminderTime>) = context.settingsDataStore.edit {
        val normalized = times
            .map { ReminderTime(it.hour.coerceIn(0, 23), it.minute.coerceIn(0, 59)) }
            .distinct()
            .sorted()
            .take(12)
            .ifEmpty { listOf(ReminderTime(20, 0)) }
        it[Keys.reminderTimes] = serializeReminderTimes(normalized)
        it[Keys.reminderHour] = normalized.first().hour
        it[Keys.reminderMinute] = normalized.first().minute
    }

    suspend fun setInt(name: String, number: Int) = context.settingsDataStore.edit {
        when (name) {
            "dailyGoalCards" -> it[Keys.dailyGoalCards] = number.coerceIn(5, 100)
            "dailyNewCardLimit" -> it[Keys.dailyNewCardLimit] = number.coerceIn(1, 100)
            "dailyReviewLimit" -> it[Keys.dailyReviewLimit] = number.coerceIn(10, 500)
            "pdfPageLimit" -> it[Keys.pdfPageLimit] = number.coerceIn(1, 50)
            "reminderHour" -> it[Keys.reminderHour] = number.coerceIn(0, 23)
            "reminderMinute" -> it[Keys.reminderMinute] = number.coerceIn(0, 59)
            else -> error("Unknown integer setting: $name")
        }
    }

    suspend fun completeOnboarding(dailyGoalCards: Int, remindersEnabled: Boolean, reminderHour: Int) =
        context.settingsDataStore.edit {
            it[Keys.onboardingCompleted] = true
            it[Keys.dailyGoalCards] = dailyGoalCards.coerceIn(5, 100)
            it[Keys.remindersEnabled] = remindersEnabled
            it[Keys.reminderHour] = reminderHour.coerceIn(0, 23)
            it[Keys.reminderMinute] = 0
            it[Keys.reminderTimes] = serializeReminderTimes(listOf(ReminderTime(reminderHour, 0)))
        }

    suspend fun markDataInitialized() = context.settingsDataStore.edit {
        it[Keys.dataInitialized] = true
    }

    suspend fun resetAfterDataClear() = context.settingsDataStore.edit {
        it.asMap().keys.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            it.remove(key as Preferences.Key<Any>)
        }
        it[Keys.dataInitialized] = true
        it[Keys.onboardingCompleted] = false
    }

    private fun parseReminderTime(value: String): ReminderTime? {
        val parts = value.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return ReminderTime(hour, minute)
    }

    private fun serializeReminderTimes(times: List<ReminderTime>): String =
        times.joinToString(",") { "%02d:%02d".format(it.hour, it.minute) }
}
