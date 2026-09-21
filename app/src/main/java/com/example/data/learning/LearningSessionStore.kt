package com.example.data.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LearningRound(
    val remainingCardIds: List<Long>,
    val unfamiliarCardIds: Set<Long>,
    val totalCardCount: Int = remainingCardIds.size,
    val history: List<LearningHistoryEntry> = emptyList()
)

data class LearningHistoryEntry(
    val cardId: Long,
    val wasUnfamiliarBefore: Boolean
)

data class LearningUndo(
    val round: LearningRound,
    val cardId: Long
)

data class LearningAdvance(
    val nextRound: LearningRound?,
    val roundFinished: Boolean,
    val allRemembered: Boolean,
    val nextRoundCardCount: Int
)

/**
 * Persists the unfinished swipe queue for each deck. Only ids are stored, so card
 * edits stay visible and deleted cards disappear automatically on the next load.
 */
class LearningSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(
        scopeId: Long,
        availableCardIds: List<Long>,
        shuffleNewRound: Boolean = true
    ): LearningRound {
        return loadSaved(scopeId, availableCardIds)
            ?: startNew(scopeId, availableCardIds, shuffleNewRound)
    }

    /** Returns only an unfinished saved session and never creates or reshuffles one. */
    fun loadSaved(scopeId: Long, availableCardIds: List<Long>): LearningRound? {
        val available = availableCardIds.toSet()
        val saved = preferences.getString(key(scopeId), null)?.let(::decode) ?: return null
        val remaining = saved.remainingCardIds.filter { it in available }
        if (remaining.isEmpty()) return null

        val unfamiliar = saved.unfamiliarCardIds.filterTo(linkedSetOf()) { it in available }
        val history = saved.history.filter { it.cardId in available }
        return saved.copy(
            remainingCardIds = remaining,
            unfamiliarCardIds = unfamiliar,
            totalCardCount = saved.totalCardCount.coerceAtLeast(remaining.size),
            history = history
        ).also { persist(scopeId, it) }
    }

    /** Starts from every available card and intentionally replaces any old progress. */
    fun startNew(
        scopeId: Long,
        availableCardIds: List<Long>,
        shuffleNewRound: Boolean
    ): LearningRound {
        val cardIds = if (shuffleNewRound) availableCardIds.shuffled() else availableCardIds
        return LearningRound(cardIds, emptySet()).also { persist(scopeId, it) }
    }

    fun answer(
        scopeId: Long,
        current: LearningRound,
        cardId: Long,
        remembered: Boolean
    ): LearningAdvance {
        val remaining = current.remainingCardIds.drop(1)
        val history = current.history + LearningHistoryEntry(
            cardId = cardId,
            wasUnfamiliarBefore = cardId in current.unfamiliarCardIds
        )
        val unfamiliar = current.unfamiliarCardIds.toMutableSet().apply {
            if (!remembered) add(cardId)
        }

        if (remaining.isNotEmpty()) {
            val next = LearningRound(remaining, unfamiliar, current.totalCardCount, history)
            persist(scopeId, next)
            return LearningAdvance(next, false, false, 0)
        }

        if (unfamiliar.isEmpty()) {
            preferences.edit().remove(key(scopeId)).apply()
            return LearningAdvance(null, true, true, 0)
        }

        val nextVisit = LearningRound(unfamiliar.shuffled(), emptySet())
        persist(scopeId, nextVisit)
        return LearningAdvance(null, true, false, nextVisit.remainingCardIds.size)
    }

    /** Restores the last answered card, including after leaving and reopening learning. */
    fun undo(scopeId: Long, current: LearningRound): LearningUndo? {
        val last = current.history.lastOrNull() ?: return null
        val unfamiliar = current.unfamiliarCardIds.toMutableSet().apply {
            if (last.wasUnfamiliarBefore) add(last.cardId) else remove(last.cardId)
        }
        val restored = current.copy(
            remainingCardIds = listOf(last.cardId) + current.remainingCardIds,
            unfamiliarCardIds = unfamiliar,
            history = current.history.dropLast(1)
        )
        persist(scopeId, restored)
        return LearningUndo(restored, last.cardId)
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    fun save(scopeId: Long, round: LearningRound) {
        persist(scopeId, round)
    }

    private fun persist(scopeId: Long, round: LearningRound) {
        preferences.edit().putString(key(scopeId), encode(round)).apply()
    }

    private fun encode(round: LearningRound): String = JSONObject()
        .put("remaining", JSONArray(round.remainingCardIds))
        .put("unfamiliar", JSONArray(round.unfamiliarCardIds.toList()))
        .put("total", round.totalCardCount)
        .put(
            "history",
            JSONArray().apply {
                round.history.forEach { entry ->
                    put(
                        JSONObject()
                            .put("cardId", entry.cardId)
                            .put("wasUnfamiliarBefore", entry.wasUnfamiliarBefore)
                    )
                }
            }
        )
        .toString()

    private fun decode(value: String): LearningRound? = runCatching {
        val json = JSONObject(value)
        LearningRound(
            remainingCardIds = json.getJSONArray("remaining").toLongList(),
            unfamiliarCardIds = json.getJSONArray("unfamiliar").toLongList().toSet(),
            totalCardCount = json.optInt("total", json.getJSONArray("remaining").length()),
            history = json.optJSONArray("history")?.toHistoryList().orEmpty()
        )
    }.getOrNull()

    private fun JSONArray.toLongList(): List<Long> =
        List(length()) { index -> getLong(index) }

    private fun JSONArray.toHistoryList(): List<LearningHistoryEntry> =
        List(length()) { index ->
            getJSONObject(index).let { item ->
                LearningHistoryEntry(
                    cardId = item.getLong("cardId"),
                    wasUnfamiliarBefore = item.optBoolean("wasUnfamiliarBefore", false)
                )
            }
        }

    private fun key(scopeId: Long) = "learning_scope_$scopeId"

    private companion object {
        const val PREFERENCES_NAME = "learning_sessions"
    }
}
