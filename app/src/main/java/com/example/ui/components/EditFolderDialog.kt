package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFolderDialog(
    deck: Deck,
    existingCourses: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (updatedDeck: Deck) -> Unit
) {
    val courseOptions = remember(existingCourses, deck.category) {
        (existingCourses + deck.category).filter { it.isNotBlank() }.distinct()
    }
    var courseName by remember(deck.id) { mutableStateOf(deck.category) }
    var newCourseInput by remember { mutableStateOf("") }
    var isAddingNewCourse by remember { mutableStateOf(false) }
    var isCourseMenuExpanded by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf(deck.name) }
    var description by remember { mutableStateOf(deck.description) }

    val colorOptions = listOf(
        "#426B63", // Sage
        "#5E7180", // Slate
        "#8A6657", // Clay
        "#55749A", // Blue
        "#6F7D52", // Olive
        "#A7793D"  // Ochre
    )
    var selectedColor by remember { mutableStateOf(deck.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "編輯資料夾",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Course Selection or Creation
                Text(
                    text = "課程",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (!isAddingNewCourse) {
                    ExposedDropdownMenuBox(
                        expanded = isCourseMenuExpanded,
                        onExpandedChange = { isCourseMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("選擇課程") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCourseMenuExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = isCourseMenuExpanded,
                            onDismissRequest = { isCourseMenuExpanded = false }
                        ) {
                            courseOptions.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course) },
                                    onClick = {
                                        courseName = course
                                        isCourseMenuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("新增課程…") },
                                onClick = {
                                    isCourseMenuExpanded = false
                                    isAddingNewCourse = true
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newCourseInput,
                        onValueChange = { newCourseInput = it },
                        label = { Text("課程名稱") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { isAddingNewCourse = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("改選現有課程")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Folder Name Input
                Text(
                    text = "資料夾",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("名稱 *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("說明（選填）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Picker
                Text(
                    text = "顏色",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorOptions.forEach { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val finalCourse = if (isAddingNewCourse) newCourseInput.trim() else courseName.trim()
            Button(
                onClick = {
                    if (finalCourse.isNotBlank() && folderName.isNotBlank()) {
                        onConfirm(
                            deck.copy(
                                category = finalCourse,
                                name = folderName.trim(),
                                description = description.trim(),
                                colorHex = selectedColor
                            )
                        )
                    }
                },
                enabled = (if (isAddingNewCourse) newCourseInput.isNotBlank() else courseName.isNotBlank()) && folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("儲存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
