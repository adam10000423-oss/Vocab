package com.example.util

fun pronunciationScore(target: String, heard: String, confidence: Float?): Int {
    val expected = target.lowercase().filter(Char::isLetter)
    val actualWords = heard.lowercase().split(Regex("[^a-z]+"))
        .map { it.filter(Char::isLetter) }
        .filter(String::isNotBlank)
    if (expected.isBlank() || actualWords.isEmpty()) return 0
    val similarity = actualWords.maxOf { word ->
        val distance = levenshteinDistance(expected, word)
        1f - distance.toFloat() / maxOf(expected.length, word.length, 1)
    }.coerceIn(0f, 1f)
    val confidenceFactor = confidence?.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: similarity
    return (similarity * 80f + confidenceFactor * 20f).toInt().coerceIn(0, 100)
}

private fun levenshteinDistance(first: String, second: String): Int {
    if (first.isEmpty()) return second.length
    if (second.isEmpty()) return first.length
    var previous = IntArray(second.length + 1) { it }
    first.forEachIndexed { firstIndex, firstChar ->
        val current = IntArray(second.length + 1)
        current[0] = firstIndex + 1
        second.forEachIndexed { secondIndex, secondChar ->
            current[secondIndex + 1] = minOf(
                current[secondIndex] + 1,
                previous[secondIndex + 1] + 1,
                previous[secondIndex] + if (firstChar == secondChar) 0 else 1
            )
        }
        previous = current
    }
    return previous.last()
}
