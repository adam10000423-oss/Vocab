package com.example.data.learning

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** Stores manual make-up check-ins separately from real study logs. */
class StudyCheckInStore(context: Context) {
    private val preferences = context.getSharedPreferences("study_check_ins", Context.MODE_PRIVATE)
    private val _dates = MutableStateFlow(loadDates())
    val dates: StateFlow<Set<LocalDate>> = _dates.asStateFlow()

    fun toggle(date: LocalDate) {
        val next = _dates.value.toMutableSet().apply {
            if (!add(date)) remove(date)
        }
        preferences.edit().putStringSet(KEY_DATES, next.map(LocalDate::toString).toSet()).apply()
        _dates.value = next
    }

    fun clear() {
        preferences.edit().clear().apply()
        _dates.value = emptySet()
    }

    fun merge(dates: Set<LocalDate>) {
        val next = loadDates() + dates.filter { it <= LocalDate.now() }
        preferences.edit().putStringSet(KEY_DATES, next.map(LocalDate::toString).toSet()).apply()
        _dates.value = next
    }

    fun reload() {
        _dates.value = loadDates()
    }

    fun currentDates(): Set<LocalDate> = loadDates()

    private fun loadDates(): Set<LocalDate> = preferences.getStringSet(KEY_DATES, emptySet())
        .orEmpty()
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .toSet()

    private companion object {
        const val KEY_DATES = "dates"
    }
}
