package com.example.data.api

enum class AiProvider(
    val displayName: String,
    val defaultModel: String,
    val keyUrl: String,
    val recommendedModels: List<String>
) {
    GEMINI(
        "Google Gemini",
        "gemini-3.6-flash",
        "https://aistudio.google.com/app/apikey",
        listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-flash-latest"
        )
    ),
    OPENAI(
        "OpenAI",
        "gpt-5.6-luna",
        "https://platform.openai.com/api-keys",
        listOf("gpt-5.6-luna", "gpt-5.6-terra", "gpt-5.6-sol", "gpt-5.6")
    ),
    ANTHROPIC(
        "Anthropic Claude",
        "claude-sonnet-4-6",
        "https://console.anthropic.com/settings/keys",
        listOf("claude-haiku-4-5-20251001", "claude-sonnet-4-6", "claude-opus-4-6")
    ),
    OPENROUTER(
        "OpenRouter",
        "openai/gpt-5.6-luna",
        "https://openrouter.ai/settings/keys",
        listOf(
            "openai/gpt-5.6-luna",
            "google/gemini-3.6-flash",
            "anthropic/claude-sonnet-4.6"
        )
    );

    companion object {
        fun from(value: String): AiProvider =
            entries.firstOrNull { it.name == value } ?: GEMINI
    }
}

data class PersonalAiConfig(
    val credentialId: String,
    val provider: AiProvider,
    val apiKey: String,
    val model: String
)

data class AiApiProfile(
    val credentialId: String,
    val provider: AiProvider,
    val model: String,
    val maskedKey: String
)
