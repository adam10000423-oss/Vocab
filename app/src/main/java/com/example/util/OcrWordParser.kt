package com.example.util

import com.example.data.dictionary.DictionaryEngine
import com.example.data.entity.Flashcard

data class OcrCardCandidate(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val exampleSentence: String,
    val phonetic: String = "",
    val exampleTranslation: String = "",
    val isSelected: Boolean = true
)

object OcrWordParser {

    private val IGNORED_KEYWORDS = setOf(
        "unit", "chapter", "lesson", "page", "index", "vocabulary", "grammar",
        "exercise", "part", "section", "copyright", "author", "date", "table",
        "content", "contents", "book", "edition", "test", "quiz", "review",
        "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "of", "with",
        "is", "are", "was", "were", "be", "been", "being"
    )

    /**
     * Cleans up raw Chinese definition text so it strictly contains only concise definitions.
     */
    fun cleanDefinition(raw: String): String {
        return raw.replace(Regex("【AI建議解釋】|【AI建議】|【AI】|待補充中文解釋|待補充解釋|待補充|暫無中文解釋"), "")
            .trim()
    }

    /**
     * Checks if a candidate string is noise or document structural text rather than a real vocabulary word.
     */
    fun isNoiseWord(word: String): Boolean {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.length > 30) return true
        if (clean in IGNORED_KEYWORDS) return true
        if (clean.matches(Regex("^[0-9]+$"))) return true
        if (clean.matches(Regex("^[0-9\\W_]+$"))) return true
        return false
    }

    /**
     * Parses raw extracted text from photo or OCR into candidate flashcard items with noise filtering.
     */
    fun parseTextToCards(rawText: String, deckId: Long): List<OcrCardCandidate> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<OcrCardCandidate>()
        val consumedLineIndexes = mutableSetOf<Int>()

        for ((lineIndex, line) in lines.withIndex()) {
            if (lineIndex in consumedLineIndexes) continue
            // Check for delimiter formats like "|", "-", ":", "\t", or "="
            val parts = when {
                line.contains("|") -> line.split("|")
                line.contains("\t") -> line.split("\t")
                line.contains(" - ") -> line.split(" - ")
                line.contains("：") -> line.split("：")
                line.contains(":") -> line.split(":")
                line.contains("=") -> line.split("=")
                else -> emptyList()
            }

            if (parts.size >= 2) {
                val rawWord = parts[0].trim().replace(Regex("[^a-zA-Z\\s-]"), "")
                if (!isNoiseWord(rawWord)) {
                    val detectedPos = extractPos(parts[1])
                    val definitionIndex = if (detectedPos.isNotBlank()) 2 else 1
                    val sentenceIndex = definitionIndex + 1
                    val dictEntry = DictionaryEngine.findExact(rawWord)

                    candidates.add(
                        OcrCardCandidate(
                            word = rawWord,
                            partOfSpeech = detectedPos.ifBlank { dictEntry?.partOfSpeech.orEmpty() },
                            definition = cleanDefinition(
                                parts.getOrNull(definitionIndex)?.trim()
                                    .orEmpty()
                                    .ifBlank { dictEntry?.definition.orEmpty() }
                            ),
                            exampleSentence = parts.getOrNull(sentenceIndex)?.trim()
                                .orEmpty()
                                .ifBlank { dictEntry?.exampleSentence.orEmpty() },
                            phonetic = dictEntry?.phonetic.orEmpty(),
                            exampleTranslation = dictEntry?.exampleTranslation.orEmpty()
                        )
                    )
                }
            } else {
                // Only accept an actual standalone English word here. Joining every
                // letter in a sentence creates fake candidates such as
                // "Thisisanexamplesentence".
                val cleanWord = line.trim()
                if (
                    cleanWord.matches(Regex("^[A-Za-z][A-Za-z'-]{1,29}$")) &&
                    !isNoiseWord(cleanWord)
                ) {
                    val dictEntry = DictionaryEngine.findExact(cleanWord)
                    val adjacentDefinition = lines.getOrNull(lineIndex + 1)
                        ?.takeIf { nextLine ->
                            nextLine.any { it.code in 0x3400..0x9FFF } &&
                                !nextLine.contains(Regex("[|=\\t]"))
                        }
                        ?.let(::cleanDefinition)
                        .orEmpty()
                    if (adjacentDefinition.isNotBlank()) {
                        consumedLineIndexes += lineIndex + 1
                    }

                    candidates.add(
                        OcrCardCandidate(
                            word = cleanWord,
                            partOfSpeech = dictEntry?.partOfSpeech.orEmpty(),
                            definition = adjacentDefinition.ifBlank {
                                cleanDefinition(dictEntry?.definition.orEmpty())
                            },
                            exampleSentence = dictEntry?.exampleSentence.orEmpty(),
                            phonetic = dictEntry?.phonetic.orEmpty(),
                            exampleTranslation = dictEntry?.exampleTranslation.orEmpty()
                        )
                    )
                }
            }
        }

        return candidates.distinctBy { it.word.lowercase() }
    }

    /**
     * Fills only fields backed by an exact local dictionary match.
     * Unknown words stay blank so the UI never presents fabricated phonetics,
     * definitions, or example sentences as real data.
     */
    fun enrichCandidatesWithDictionary(candidates: List<OcrCardCandidate>): List<OcrCardCandidate> {
        return candidates.map { candidate ->
            val dictEntry = DictionaryEngine.findExact(candidate.word)

            candidate.copy(
                phonetic = candidate.phonetic.ifBlank { dictEntry?.phonetic.orEmpty() },
                partOfSpeech = candidate.partOfSpeech.ifBlank { dictEntry?.partOfSpeech.orEmpty() },
                definition = if (
                    candidate.definition.isBlank() ||
                    candidate.definition.contains("待補充") ||
                    candidate.definition.contains("暫無")
                ) {
                    cleanDefinition(dictEntry?.definition.orEmpty())
                } else {
                    cleanDefinition(candidate.definition)
                },
                exampleSentence = candidate.exampleSentence.ifBlank {
                    dictEntry?.exampleSentence.orEmpty()
                },
                exampleTranslation = candidate.exampleTranslation.ifBlank {
                    dictEntry?.exampleTranslation.orEmpty()
                }
            )
        }
    }

    @Deprecated(
        message = "This method performs dictionary enrichment only; use enrichCandidatesWithDictionary",
        replaceWith = ReplaceWith("enrichCandidatesWithDictionary(candidates)")
    )
    fun enrichCandidatesWithAi(candidates: List<OcrCardCandidate>): List<OcrCardCandidate> =
        enrichCandidatesWithDictionary(candidates)

    private fun extractPos(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("v.") || lower.contains("verb") -> "v."
            lower.contains("n.") || lower.contains("noun") -> "n."
            lower.contains("adj.") || lower.contains("adjective") -> "adj."
            lower.contains("adv.") || lower.contains("adverb") -> "adv."
            lower.contains("prep.") -> "prep."
            lower.contains("conj.") -> "conj."
            else -> ""
        }
    }

}
