package com.example.data.settings

import com.example.data.api.AiPromptDefaults

data class ReminderTime(
    val hour: Int,
    val minute: Int
) : Comparable<ReminderTime> {
    override fun compareTo(other: ReminderTime): Int =
        (hour * 60 + minute).compareTo(other.hour * 60 + other.minute)

    fun displayText(): String = "%02d:%02d".format(hour, minute)
}

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val dataInitialized: Boolean = false,
    val dailyGoalCards: Int = 20,
    val themeMode: String = "SYSTEM",
    val themeColorPreset: String = "GREEN",
    val customPrimaryColor: String = "#39796D",
    val customSecondaryColor: String = "#5B7482",
    val backgroundBrightness: Float = 1f,
    val backgroundOpacity: Float = 1f,
    val gradientEnabled: Boolean = false,
    val gradientStartColor: String = "#DFF5EC",
    val gradientEndColor: String = "#DCEBFA",
    val fontFamily: String = "DEFAULT",
    val fontScale: Float = 1f,
    val customTextColorEnabled: Boolean = false,
    val customTextColor: String = "#202522",
    val advancedSrs: Boolean = false,
    val dailyNewCardLimit: Int = 20,
    val dailyReviewLimit: Int = 100,
    val aiEnabled: Boolean = true,
    val aiRequiresConfirmation: Boolean = true,
    val usePersonalAiApi: Boolean = false,
    val aiProvider: String = "GEMINI",
    val aiModel: String = "gemini-3.6-flash",
    val aiChatStyle: String = "NORMAL",
    val aiWordPrompt: String = AiPromptDefaults.WORD_DETAILS,
    val aiImagePrompt: String = AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION,
    val ocrPreviewBeforeImport: Boolean = true,
    val pdfPageLimit: Int = 20,
    val autoSpeak: Boolean = false,
    val speechRate: Float = 1f,
    val ttsVoiceStyle: String = "NATURAL",
    val ttsVoiceName: String = "",
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderTimes: List<ReminderTime> = listOf(ReminderTime(20, 0)),
    val gameMistakesToReview: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val wifiOnlyUpdates: Boolean = true,
    val reduceMotion: Boolean = false
)
