package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.learning.StudyCheckInStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class BackupService(
    private val context: Context,
    private val database: AppDatabase
) {
    private val studyCheckInStore = StudyCheckInStore(context)

    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val decks = database.deckDao().getAllDecks().first()
        val cards = database.flashcardDao().getAllCards().first()
        val logs = database.studyLogDao().getAllLogs().first()
        val root = JSONObject()
            .put("format", "vocab-backup")
            .put("version", 2)
            .put("exportedAt", System.currentTimeMillis())
            .put("decks", JSONArray().apply {
                decks.forEach { deck ->
                    put(JSONObject()
                        .put("id", deck.id)
                        .put("name", deck.name)
                        .put("description", deck.description)
                        .put("category", deck.category)
                        .put("colorHex", deck.colorHex)
                        .put("createdAt", deck.createdAt)
                        .put("sortOrder", deck.sortOrder))
                }
            })
            .put("cards", JSONArray().apply {
                cards.forEach { card -> put(card.toJson()) }
            })
            .put("studyLogs", JSONArray().apply {
                logs.forEach { log ->
                    put(JSONObject().put("cardId", log.cardId).put("rating", log.rating).put("reviewedAt", log.reviewedAt))
                }
            })
            .put("makeUpCheckIns", JSONArray(studyCheckInStore.currentDates().map(LocalDate::toString)))
        context.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter().use { it.write(root.toString(2)) }
        cards.size
    }

    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val decks = database.deckDao().getAllDecks().first().associateBy { it.id }
        val cards = database.flashcardDao().getAllCards().first()
        context.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter().use { writer ->
            writer.appendLine("deck,category,word,phonetic,partOfSpeech,definition,exampleSentence,exampleTranslation,notes")
            cards.forEach { card ->
                val deck = decks[card.deckId]
                writer.appendLine(listOf(
                    deck?.name.orEmpty(), deck?.category.orEmpty(), card.word, card.phonetic,
                    card.partOfSpeech, card.definition, card.exampleSentence,
                    card.exampleTranslation, card.notes
                ).joinToString(",") { csv(it) })
            }
        }
        cards.size
    }

    suspend fun importJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        require(root.optString("format") in setOf("vocab-backup", "vocabpulse-backup")) {
            "不是 Vocab 備份檔"
        }
        val deckItems = root.optJSONArray("decks") ?: JSONArray()
        val cardItems = root.optJSONArray("cards") ?: JSONArray()
        val logItems = root.optJSONArray("studyLogs") ?: JSONArray()
        val checkInItems = root.optJSONArray("makeUpCheckIns") ?: JSONArray()
        var imported = 0
        database.withTransaction {
            val existingDecks = database.deckDao().getAllDecks().first().toMutableList()
            val deckIdMap = mutableMapOf<Long, Long>()
            val cardIdMap = mutableMapOf<Long, Long>()
            for (index in 0 until deckItems.length()) {
                val item = deckItems.getJSONObject(index)
                val oldId = item.optLong("id")
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                val category = item.optString("category", "通用")
                val existing = existingDecks.firstOrNull {
                    it.name.equals(name, true) && it.category.equals(category, true)
                }
                val newId = existing?.id ?: database.deckDao().insertDeck(
                    Deck(
                        name = name,
                        description = item.optString("description"),
                        category = category,
                        colorHex = item.optString("colorHex", "#426B63"),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        sortOrder = item.optLong(
                            "sortOrder",
                            item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                )
                deckIdMap[oldId] = newId
            }
            for (index in 0 until cardItems.length()) {
                val item = cardItems.getJSONObject(index)
                val deckId = deckIdMap[item.optLong("deckId")] ?: continue
                val word = item.optString("word").trim()
                if (word.isBlank()) continue
                val incoming = item.toCard(deckId, word)
                val normalizedWord = word.lowercase()
                val existing = database.flashcardDao().getCardByWord(deckId, normalizedWord)
                if (existing == null) {
                    val newId = database.flashcardDao().insertCard(
                        incoming.copy(normalizedWord = normalizedWord)
                    )
                    cardIdMap[item.optLong("id")] = newId
                    imported++
                } else {
                    cardIdMap[item.optLong("id")] = existing.id
                    database.flashcardDao().updateCard(existing.copy(
                        phonetic = existing.phonetic.ifBlank { incoming.phonetic },
                        partOfSpeech = existing.partOfSpeech.ifBlank { incoming.partOfSpeech },
                        definition = existing.definition.ifBlank { incoming.definition },
                        exampleSentence = existing.exampleSentence.ifBlank { incoming.exampleSentence },
                        exampleTranslation = existing.exampleTranslation.ifBlank { incoming.exampleTranslation },
                        notes = existing.notes.ifBlank { incoming.notes }
                    ))
                }
            }
            for (index in 0 until logItems.length()) {
                val item = logItems.getJSONObject(index)
                val cardId = cardIdMap[item.optLong("cardId")] ?: continue
                val rating = item.optInt("rating")
                if (rating !in 1..4) continue
                database.studyLogDao().insertLog(
                    StudyLog(cardId = cardId, rating = rating, reviewedAt = item.optLong("reviewedAt"))
                )
            }
        }
        studyCheckInStore.merge(
            (0 until checkInItems.length()).mapNotNull { index ->
                runCatching { LocalDate.parse(checkInItems.optString(index)) }.getOrNull()
            }.toSet()
        )
        imported
    }

    private fun Flashcard.toJson() = JSONObject()
        .put("id", id).put("deckId", deckId).put("word", word).put("phonetic", phonetic)
        .put("partOfSpeech", partOfSpeech).put("definition", definition)
        .put("exampleSentence", exampleSentence).put("exampleTranslation", exampleTranslation)
        .put("notes", notes).put("isMastered", isMastered).put("isFavorite", isFavorite)
        .put("isSuspended", isSuspended).put("mistakeCount", mistakeCount).put("intervalDays", intervalDays)
        .put("easeFactor", easeFactor.toDouble()).put("repetitionCount", repetitionCount)
        .put("nextReviewTimestamp", nextReviewTimestamp).put("lastReviewedTimestamp", lastReviewedTimestamp)
        .put("createdAt", createdAt).put("sortOrder", sortOrder)

    private fun JSONObject.toCard(deckId: Long, word: String) = Flashcard(
        deckId = deckId,
        word = word,
        phonetic = optString("phonetic"),
        partOfSpeech = optString("partOfSpeech"),
        definition = optString("definition"),
        exampleSentence = optString("exampleSentence"),
        exampleTranslation = optString("exampleTranslation"),
        notes = optString("notes"),
        isMastered = optBoolean("isMastered"),
        isFavorite = optBoolean("isFavorite"),
        isSuspended = optBoolean("isSuspended"),
        mistakeCount = optInt("mistakeCount"),
        intervalDays = optInt("intervalDays"),
        easeFactor = optDouble("easeFactor", 2.5).toFloat(),
        repetitionCount = optInt("repetitionCount"),
        nextReviewTimestamp = optLong("nextReviewTimestamp", System.currentTimeMillis()),
        lastReviewedTimestamp = if (isNull("lastReviewedTimestamp")) null else optLong("lastReviewedTimestamp"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        sortOrder = optLong("sortOrder", optLong("createdAt", System.currentTimeMillis()))
    )

    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
