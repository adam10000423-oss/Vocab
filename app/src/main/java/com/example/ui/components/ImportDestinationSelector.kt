package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDestinationSelector(
    decks: List<Deck>,
    selectedDeckId: Long?,
    onDeckSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses = remember(decks) { decks.map { it.category }.distinct() }
    var selectedCourse by remember(decks, selectedDeckId) {
        mutableStateOf(decks.firstOrNull { it.id == selectedDeckId }?.category ?: courses.firstOrNull())
    }
    var courseExpanded by remember { mutableStateOf(false) }
    var deckExpanded by remember { mutableStateOf(false) }
    val courseDecks = remember(decks, selectedCourse) { decks.filter { it.category == selectedCourse } }

    LaunchedEffect(decks, selectedDeckId, selectedCourse) {
        val selected = decks.firstOrNull { it.id == selectedDeckId }
        if (selected != null && selected.category != selectedCourse) selectedCourse = selected.category
        if (selected == null || selected.category != selectedCourse) {
            courseDecks.firstOrNull()?.let { onDeckSelected(it.id) }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = courseExpanded,
            onExpandedChange = { courseExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedCourse.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("選擇課程") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course) },
                        onClick = {
                            selectedCourse = course
                            courseExpanded = false
                            decks.firstOrNull { it.category == course }?.let { onDeckSelected(it.id) }
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = deckExpanded,
            onExpandedChange = { deckExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = courseDecks.firstOrNull { it.id == selectedDeckId }?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("選擇資料夾") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deckExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = deckExpanded, onDismissRequest = { deckExpanded = false }) {
                courseDecks.forEach { deck ->
                    DropdownMenuItem(
                        text = { Text(deck.name) },
                        onClick = { onDeckSelected(deck.id); deckExpanded = false }
                    )
                }
            }
        }
    }
}
