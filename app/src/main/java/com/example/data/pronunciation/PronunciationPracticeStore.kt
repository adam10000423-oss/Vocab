package com.example.data.pronunciation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PronunciationPracticeStore(context: Context) {
    private val preferences = context.getSharedPreferences("vocab_pronunciation", Context.MODE_PRIVATE)
    private val _weakCardIds = MutableStateFlow(
        preferences.getStringSet(KEY_WEAK_IDS, emptySet()).orEmpty()
            .mapNotNull(String::toLongOrNull)
            .toSet()
    )
    val weakCardIds: StateFlow<Set<Long>> = _weakCardIds.asStateFlow()

    fun recordResult(cardId: Long, score: Int) {
        if (cardId <= 0L) return
        val updated = when {
            score < 80 -> _weakCardIds.value + cardId
            score >= 90 -> _weakCardIds.value - cardId
            else -> _weakCardIds.value
        }
        if (updated == _weakCardIds.value) return
        _weakCardIds.value = updated
        preferences.edit()
            .putStringSet(KEY_WEAK_IDS, updated.map(Long::toString).toSet())
            .apply()
    }

    fun clearMissingCards(existingIds: Set<Long>) {
        val updated = _weakCardIds.value.intersect(existingIds)
        if (updated == _weakCardIds.value) return
        _weakCardIds.value = updated
        preferences.edit()
            .putStringSet(KEY_WEAK_IDS, updated.map(Long::toString).toSet())
            .apply()
    }

    fun clear() {
        _weakCardIds.value = emptySet()
        preferences.edit().remove(KEY_WEAK_IDS).apply()
    }

    private companion object {
        const val KEY_WEAK_IDS = "weak_card_ids"
    }
}
