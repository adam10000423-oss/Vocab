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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFolderDialog(
    existingCourses: List<String>,
    defaultCourse: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (courseName: String, folderName: String, description: String, colorHex: String) -> Unit
) {
    val courseOptions = remember(existingCourses) { existingCourses.filter { it.isNotBlank() }.distinct() }
    var courseName by remember(defaultCourse, courseOptions) {
        mutableStateOf(defaultCourse?.takeIf { it.isNotBlank() } ?: courseOptions.firstOrNull().orEmpty())
    }
    var newCourseInput by remember { mutableStateOf("") }
    var isAddingNewCourse by remember(courseOptions) { mutableStateOf(courseOptions.isEmpty()) }
    var isCourseMenuExpanded by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val newCourseFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isAddingNewCourse) {
        if (isAddingNewCourse) {
            delay(120)
            newCourseFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val colorOptions = listOf(
        "#426B63", // Sage
        "#5E7180", // Slate
        "#8A6657", // Clay
        "#55749A", // Blue
        "#6F7D52", // Olive
        "#A7793D"  // Ochre
    )
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新增資料夾",
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
                                },
                                modifier = Modifier.testTag("add_new_course_toggle_button")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(newCourseFocusRequester)
                            .testTag("new_course_name_input")
                    )
                    if (courseOptions.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { isAddingNewCourse = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("改選現有課程")
                        }
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
                    modifier = Modifier.fillMaxWidth().testTag("folder_name_input")
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
                        onConfirm(finalCourse, folderName.trim(), description.trim(), selectedColor)
                    }
                },
                enabled = (if (isAddingNewCourse) newCourseInput.isNotBlank() else courseName.isNotBlank()) && folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_create_folder_button")
            ) {
                Text("建立資料夾", fontWeight = FontWeight.Bold)
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
