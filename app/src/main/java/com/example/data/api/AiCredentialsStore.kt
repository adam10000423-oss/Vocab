package com.example.data.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.UUID

/**
 * Stores multiple encrypted keys per AI provider. Keys never enter DataStore,
 * backups, logs, or source code.
 */
class AiCredentialsStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasApiKey(): Boolean =
        AiProvider.entries.any(::hasApiKey) || preferences.contains(LEGACY_CIPHERTEXT)

    fun hasApiKey(provider: AiProvider): Boolean =
        credentialIds(provider).isNotEmpty() || preferences.contains(ciphertextKey(provider))

    fun saveApiKey(provider: AiProvider, apiKey: String, model: String): String {
        val normalized = apiKey.trim()
        require(normalized.isNotBlank()) { "API Key 不可空白" }
        migrateProviderKey(provider, model)
        val existing = readProviderCredentials(provider)
        existing.firstOrNull { it.second == normalized }?.let { return it.first }
        val credentialId = UUID.randomUUID().toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val ids = credentialIds(provider) + credentialId
        val order = savedProviderOrder().toMutableList().apply {
            if (provider !in this) add(provider)
        }
        preferences.edit()
            .putString(ivKey(provider, credentialId), Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(
                ciphertextKey(provider, credentialId),
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            )
            .putString(credentialsKey(provider), ids.joinToString(","))
            .putString(modelKey(provider), model.trim().ifBlank { provider.defaultModel })
            .putString(PROVIDER_ORDER, order.joinToString(",") { it.name })
            .apply()
        return credentialId
    }

    private fun readApiKey(provider: AiProvider, credentialId: String): String? = runCatching {
        val iv = Base64.decode(preferences.getString(ivKey(provider, credentialId), null), Base64.NO_WRAP)
        val encrypted = Base64.decode(
            preferences.getString(ciphertextKey(provider, credentialId), null),
            Base64.NO_WRAP
        )
        decrypt(iv, encrypted)
    }.getOrNull()

    fun readConfigs(
        preferredProvider: AiProvider,
        preferredModel: String
    ): List<PersonalAiConfig> {
        migrateLegacyKey(preferredProvider, preferredModel)
        AiProvider.entries.forEach { migrateProviderKey(it, savedModel(it)) }
        val providers = buildList {
            if (hasApiKey(preferredProvider)) add(preferredProvider)
            savedProviderOrder().forEach { if (it !in this && hasApiKey(it)) add(it) }
            AiProvider.entries.forEach { if (it !in this && hasApiKey(it)) add(it) }
        }
        return providers.flatMap { provider ->
            readProviderCredentials(provider).map { (credentialId, key) ->
                PersonalAiConfig(
                    credentialId = credentialId,
                    provider = provider,
                    apiKey = key,
                    model = if (provider == preferredProvider) {
                        preferredModel.ifBlank { savedModel(provider) }
                    } else {
                        savedModel(provider)
                    }
                )
            }
        }
    }

    fun savedModel(provider: AiProvider): String =
        preferences.getString(modelKey(provider), null)
            ?.takeIf(String::isNotBlank)
            ?: provider.defaultModel

    fun updateModel(provider: AiProvider, model: String) {
        if (!hasApiKey(provider)) return
        preferences.edit()
            .putString(modelKey(provider), model.trim().ifBlank { provider.defaultModel })
            .apply()
    }

    fun clear(provider: AiProvider) {
        val order = savedProviderOrder().filterNot { it == provider }
        val editor = preferences.edit()
        credentialIds(provider).forEach { id ->
            editor.remove(ivKey(provider, id)).remove(ciphertextKey(provider, id))
        }
        editor
            .remove(ivKey(provider))
            .remove(ciphertextKey(provider))
            .remove(credentialsKey(provider))
            .remove(modelKey(provider))
            .putString(PROVIDER_ORDER, order.joinToString(",") { it.name })
            .apply()
    }

    fun clearCredential(credentialId: String) {
        AiProvider.entries.forEach { provider ->
            val ids = credentialIds(provider)
            if (credentialId !in ids) return@forEach
            val remaining = ids.filterNot { it == credentialId }
            preferences.edit()
                .remove(ivKey(provider, credentialId))
                .remove(ciphertextKey(provider, credentialId))
                .putString(credentialsKey(provider), remaining.joinToString(","))
                .apply()
            if (remaining.isEmpty()) clear(provider)
            return
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun savedProviderOrder(): List<AiProvider> =
        preferences.getString(PROVIDER_ORDER, null)
            .orEmpty()
            .split(',')
            .mapNotNull { name -> AiProvider.entries.firstOrNull { it.name == name } }
            .distinct()

    private fun migrateLegacyKey(provider: AiProvider, model: String) {
        if (!preferences.contains(LEGACY_CIPHERTEXT) || hasApiKey(provider)) return
        val legacyKey = runCatching {
            val iv = Base64.decode(
                preferences.getString(LEGACY_IV, null),
                Base64.NO_WRAP
            )
            val encrypted = Base64.decode(
                preferences.getString(LEGACY_CIPHERTEXT, null),
                Base64.NO_WRAP
            )
            decrypt(iv, encrypted)
        }.getOrNull()
        if (!legacyKey.isNullOrBlank()) saveApiKey(provider, legacyKey, model)
        preferences.edit().remove(LEGACY_IV).remove(LEGACY_CIPHERTEXT).apply()
    }

    private fun migrateProviderKey(provider: AiProvider, model: String) {
        if (!preferences.contains(ciphertextKey(provider))) return
        val oldKey = runCatching {
            val iv = Base64.decode(preferences.getString(ivKey(provider), null), Base64.NO_WRAP)
            val encrypted = Base64.decode(
                preferences.getString(ciphertextKey(provider), null),
                Base64.NO_WRAP
            )
            decrypt(iv, encrypted)
        }.getOrNull()
        preferences.edit().remove(ivKey(provider)).remove(ciphertextKey(provider)).apply()
        if (!oldKey.isNullOrBlank()) saveApiKey(provider, oldKey, model)
    }

    private fun readProviderCredentials(provider: AiProvider): List<Pair<String, String>> =
        credentialIds(provider).mapNotNull { id ->
            readApiKey(provider, id)?.takeIf(String::isNotBlank)?.let { id to it }
        }

    private fun credentialIds(provider: AiProvider): List<String> =
        preferences.getString(credentialsKey(provider), null)
            .orEmpty().split(',').filter(String::isNotBlank).distinct()

    private fun decrypt(iv: ByteArray, encrypted: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, iv)
        )
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun ivKey(provider: AiProvider) = "iv_${provider.name}"
    private fun ciphertextKey(provider: AiProvider) = "ciphertext_${provider.name}"
    private fun ivKey(provider: AiProvider, credentialId: String) = "iv_${provider.name}_$credentialId"
    private fun ciphertextKey(provider: AiProvider, credentialId: String) =
        "ciphertext_${provider.name}_$credentialId"
    private fun credentialsKey(provider: AiProvider) = "credentials_${provider.name}"
    private fun modelKey(provider: AiProvider) = "model_${provider.name}"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    companion object {
        const val PREFERENCES_NAME = "ai_credentials"
        private const val KEY_ALIAS = "vocabpulse_personal_ai_key"
        private const val LEGACY_IV = "iv"
        private const val LEGACY_CIPHERTEXT = "ciphertext"
        private const val PROVIDER_ORDER = "provider_order"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
