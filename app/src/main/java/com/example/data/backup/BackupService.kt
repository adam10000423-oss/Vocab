package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.entity.GrammarNote
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarWeakness
import com.example.data.entity.GrammarPattern
import com.example.data.entity.GrammarExample
import com.example.data.entity.GrammarWritingRecord
import com.example.data.entity.GrammarLibrary
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
        val grammarNotes = database.grammarNoteDao().getAll().first()
        val grammarQuestions = database.grammarQuestionDao().getAllQuestions().first()
        val grammarWeaknesses = database.grammarQuestionDao().getAllWeaknesses().first()
        val grammarPatterns = database.grammarContentDao().getAllPatterns().first()
        val grammarExamples = database.grammarContentDao().getAllExamples().first()
        val grammarWritingRecords = database.grammarWritingDao().getAll().first()
        val grammarLibraries = database.grammarLibraryDao().getAll().first()
        val root = JSONObject()
            .put("format", "vocab-backup")
            .put("version", 8)
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
            .put("grammarNotes", JSONArray().apply {
                grammarNotes.forEach { note ->
                    put(JSONObject()
                        .put("id", note.id).put("course", note.course).put("folder", note.folder).put("title", note.title)
                        .put("category", note.category).put("level", note.level)
                        .put("summary", note.summary).put("structure", note.structure).put("usage", note.usage)
                        .put("exampleSentence", note.exampleSentence).put("exampleTranslation", note.exampleTranslation)
                        .put("commonMistakes", note.commonMistakes).put("comparison", note.comparison)
                        .put("tags", note.tags).put("sourceType", note.sourceType).put("sourceName", note.sourceName)
                        .put("questionTemplate", note.questionTemplate).put("answer", note.answer)
                        .put("acceptedAnswers", note.acceptedAnswers).put("options", note.options)
                        .put("explanation", note.explanation).put("favorite", note.favorite)
                        .put("masteryPercent", note.masteryPercent).put("nextReviewAt", note.nextReviewAt)
                        .put("studyStep", note.studyStep).put("attemptCount", note.attemptCount)
                        .put("correctCount", note.correctCount).put("createdAt", note.createdAt)
                        .put("updatedAt", note.updatedAt).put("sortOrder", note.sortOrder))
                }
            })
            .put("grammarLibraries", JSONArray().apply {
                grammarLibraries.forEach { library -> put(JSONObject()
                    .put("course", library.course).put("name", library.name)
                    .put("description", library.description).put("colorHex", library.colorHex)
                    .put("createdAt", library.createdAt).put("sortOrder", library.sortOrder))
                }
            })
            .put("grammarPatterns", JSONArray().apply {
                grammarPatterns.forEach { pattern -> put(JSONObject()
                    .put("id", pattern.id).put("grammarNoteId", pattern.grammarNoteId)
                    .put("title", pattern.title).put("formula", pattern.formula)
                    .put("meaning", pattern.meaning).put("usage", pattern.usage)
                    .put("notes", pattern.notes).put("masteryPercent", pattern.masteryPercent)
                    .put("nextReviewAt", pattern.nextReviewAt).put("attemptCount", pattern.attemptCount)
                    .put("correctCount", pattern.correctCount).put("sortOrder", pattern.sortOrder))
                }
            })
            .put("grammarExamples", JSONArray().apply {
                grammarExamples.forEach { example -> put(JSONObject()
                    .put("id", example.id).put("grammarNoteId", example.grammarNoteId)
                    .put("grammarPatternId", example.grammarPatternId).put("sentence", example.sentence)
                    .put("translation", example.translation).put("highlightedText", example.highlightedText)
                    .put("sortOrder", example.sortOrder))
                }
            })
            .put("grammarQuestions", JSONArray().apply {
                grammarQuestions.forEach { q -> put(JSONObject()
                    .put("id", q.id).put("grammarNoteId", q.grammarNoteId).put("type", q.type)
                    .put("grammarPatternId", q.grammarPatternId)
                    .put("prompt", q.prompt).put("translation", q.translation).put("answer", q.answer)
                    .put("acceptedAnswers", q.acceptedAnswers).put("options", q.options)
                    .put("explanation", q.explanation).put("difficulty", q.difficulty)
                    .put("sourceType", q.sourceType).put("sortOrder", q.sortOrder))
                }
            })
            .put("grammarWeaknesses", JSONArray().apply {
                grammarWeaknesses.forEach { w -> put(JSONObject()
                    .put("ruleKey", w.ruleKey).put("grammarNoteId", w.grammarNoteId).put("title", w.title)
                    .put("originalText", w.originalText).put("correctedText", w.correctedText)
                    .put("explanation", w.explanation).put("occurrenceCount", w.occurrenceCount)
                    .put("lastOccurredAt", w.lastOccurredAt))
                }
            })
            .put("grammarWritingRecords", JSONArray().apply {
                grammarWritingRecords.forEach { record -> put(JSONObject()
                    .put("id", record.id).put("course", record.course).put("title", record.title)
                    .put("originalText", record.originalText).put("revisedText", record.revisedText)
                    .put("issuesJson", record.issuesJson).put("createdAt", record.createdAt)
                    .put("updatedAt", record.updatedAt))
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
        val grammarItems = root.optJSONArray("grammarNotes") ?: JSONArray()
        val grammarQuestionItems = root.optJSONArray("grammarQuestions") ?: JSONArray()
        val grammarWeaknessItems = root.optJSONArray("grammarWeaknesses") ?: JSONArray()
        val grammarPatternItems = root.optJSONArray("grammarPatterns") ?: JSONArray()
        val grammarExampleItems = root.optJSONArray("grammarExamples") ?: JSONArray()
        val grammarWritingItems = root.optJSONArray("grammarWritingRecords") ?: JSONArray()
        val grammarLibraryItems = root.optJSONArray("grammarLibraries") ?: JSONArray()
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
            val existingGrammar = database.grammarNoteDao().getAllOnce()
            val existingLibraries = database.grammarLibraryDao().getAllOnce().toMutableList()
            for (index in 0 until grammarLibraryItems.length()) {
                val item = grammarLibraryItems.optJSONObject(index) ?: continue
                val course = item.optString("course", "通用").trim().ifBlank { "通用" }
                val name = item.optString("name").trim()
                if (name.isBlank() || existingLibraries.any { it.course.equals(course, true) && it.name.equals(name, true) }) continue
                val library = GrammarLibrary(
                    course = course, name = name, description = item.optString("description"),
                    colorHex = item.optString("colorHex", "#426B63"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = item.optLong("sortOrder", index.toLong())
                )
                database.grammarLibraryDao().insert(library)
                existingLibraries.add(library)
            }
            val grammarIdMap = mutableMapOf<Long, Long>()
            for (index in 0 until grammarItems.length()) {
                val item = grammarItems.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                if (title.isBlank()) continue
                val course = item.optString("course", "通用").trim().ifBlank { "通用" }
                val existingNote = existingGrammar.firstOrNull { it.title.equals(title, true) && it.course.equals(course, true) }
                val newGrammarId = existingNote?.id ?: database.grammarNoteDao().insert(
                    GrammarNote(
                        course = course, folder = item.optString("folder", "未分類").ifBlank { "未分類" }, title = title, category = item.optString("category", "其他"),
                        level = item.optString("level", "未分級"), summary = item.optString("summary"),
                        structure = item.optString("structure"), usage = item.optString("usage"),
                        exampleSentence = item.optString("exampleSentence"),
                        exampleTranslation = item.optString("exampleTranslation"),
                        commonMistakes = item.optString("commonMistakes"), comparison = item.optString("comparison"),
                        tags = item.optString("tags"), sourceType = item.optString("sourceType", "IMPORT"),
                        sourceName = item.optString("sourceName"), questionTemplate = item.optString("questionTemplate"),
                        answer = item.optString("answer"), acceptedAnswers = item.optString("acceptedAnswers"),
                        options = item.optString("options"), explanation = item.optString("explanation"),
                        favorite = item.optBoolean("favorite"), masteryPercent = item.optInt("masteryPercent").coerceIn(0, 100),
                        nextReviewAt = item.optLong("nextReviewAt", System.currentTimeMillis()),
                        studyStep = item.optInt("studyStep").coerceAtLeast(0), attemptCount = item.optInt("attemptCount"),
                        correctCount = item.optInt("correctCount"), createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        sortOrder = item.optLong("sortOrder", item.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
                grammarIdMap[item.optLong("id")] = newGrammarId
                val folder = item.optString("folder", "未分類").ifBlank { "未分類" }
                if (database.grammarLibraryDao().getAllOnce().none { it.course.equals(course, true) && it.name.equals(folder, true) }) {
                    database.grammarLibraryDao().insert(GrammarLibrary(course = course, name = folder))
                }
                // Version 3 and older stored one question directly on the note.
                if (grammarQuestionItems.length() == 0 && item.optString("questionTemplate").isNotBlank() &&
                    database.grammarQuestionDao().getForNote(newGrammarId).isEmpty()
                ) database.grammarQuestionDao().insertQuestion(
                    GrammarQuestion(
                        grammarNoteId = newGrammarId, prompt = item.optString("questionTemplate"),
                        translation = item.optString("exampleTranslation"), answer = item.optString("answer"),
                        acceptedAnswers = item.optString("acceptedAnswers"), options = item.optString("options"),
                        explanation = item.optString("explanation"), sourceType = "IMPORT"
                    )
                )
            }
            for (index in 0 until grammarWritingItems.length()) {
                val item = grammarWritingItems.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                if (title.isBlank()) continue
                database.grammarWritingDao().insert(GrammarWritingRecord(
                    course = item.optString("course", "通用").ifBlank { "通用" },
                    title = title,
                    originalText = item.optString("originalText"),
                    revisedText = item.optString("revisedText"),
                    issuesJson = item.optString("issuesJson", "[]"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                ))
            }
            val grammarPatternIdMap = mutableMapOf<Long, Long>()
            for (index in 0 until grammarPatternItems.length()) {
                val item = grammarPatternItems.optJSONObject(index) ?: continue
                val grammarId = grammarIdMap[item.optLong("grammarNoteId")] ?: continue
                val formula = item.optString("formula").trim()
                if (formula.isBlank()) continue
                val newPatternId = database.grammarContentDao().insertPattern(GrammarPattern(
                    grammarNoteId = grammarId, title = item.optString("title", "句型"), formula = formula,
                    meaning = item.optString("meaning"), usage = item.optString("usage"),
                    notes = item.optString("notes"), masteryPercent = item.optInt("masteryPercent").coerceIn(0, 100),
                    nextReviewAt = item.optLong("nextReviewAt", System.currentTimeMillis()),
                    attemptCount = item.optInt("attemptCount").coerceAtLeast(0),
                    correctCount = item.optInt("correctCount").coerceAtLeast(0),
                    sortOrder = item.optLong("sortOrder", index.toLong())
                ))
                grammarPatternIdMap[item.optLong("id")] = newPatternId
            }
            for (index in 0 until grammarExampleItems.length()) {
                val item = grammarExampleItems.optJSONObject(index) ?: continue
                val grammarId = grammarIdMap[item.optLong("grammarNoteId")] ?: continue
                val sentence = item.optString("sentence").trim()
                if (sentence.isBlank()) continue
                database.grammarContentDao().insertExample(GrammarExample(
                    grammarNoteId = grammarId,
                    grammarPatternId = grammarPatternIdMap[item.optLong("grammarPatternId")] ?: 0,
                    sentence = sentence, translation = item.optString("translation"),
                    highlightedText = item.optString("highlightedText"),
                    sortOrder = item.optLong("sortOrder", index.toLong())
                ))
            }
            for (index in 0 until grammarQuestionItems.length()) {
                val item = grammarQuestionItems.optJSONObject(index) ?: continue
                val grammarId = grammarIdMap[item.optLong("grammarNoteId")] ?: continue
                val prompt = item.optString("prompt").trim(); val answer = item.optString("answer").trim()
                if (prompt.isBlank() || answer.isBlank()) continue
                if (database.grammarQuestionDao().getForNote(grammarId).any { it.prompt == prompt && it.answer.equals(answer, true) }) continue
                database.grammarQuestionDao().insertQuestion(GrammarQuestion(
                    grammarNoteId = grammarId,
                    grammarPatternId = grammarPatternIdMap[item.optLong("grammarPatternId")] ?: 0,
                    type = item.optString("type", "CLOZE_CHOICE"), prompt = prompt,
                    translation = item.optString("translation"), answer = answer,
                    acceptedAnswers = item.optString("acceptedAnswers"), options = item.optString("options"),
                    explanation = item.optString("explanation"), difficulty = item.optInt("difficulty", 1).coerceIn(1, 3),
                    sourceType = item.optString("sourceType", "IMPORT"), sortOrder = item.optLong("sortOrder", System.currentTimeMillis())
                ))
            }
            for (index in 0 until grammarWeaknessItems.length()) {
                val item = grammarWeaknessItems.optJSONObject(index) ?: continue
                val grammarId = grammarIdMap[item.optLong("grammarNoteId")] ?: continue
                val ruleKey = item.optString("ruleKey").trim()
                if (ruleKey.isBlank() || database.grammarQuestionDao().getWeakness(ruleKey) != null) continue
                database.grammarQuestionDao().insertWeakness(GrammarWeakness(
                    ruleKey = ruleKey, grammarNoteId = grammarId, title = item.optString("title"),
                    originalText = item.optString("originalText"), correctedText = item.optString("correctedText"),
                    explanation = item.optString("explanation"), occurrenceCount = item.optInt("occurrenceCount", 1).coerceAtLeast(1),
                    lastOccurredAt = item.optLong("lastOccurredAt", System.currentTimeMillis())
                ))
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
