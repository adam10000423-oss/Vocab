package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.draw.clip

@Composable
fun ModernButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null, border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled, RoundedCornerShape(16.dp), colors, null, border, contentPadding, interactionSource, content)
}

@Composable
fun ModernOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null, border: androidx.compose.foundation.BorderStroke? = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, RoundedCornerShape(16.dp), colors, null, border, contentPadding, interactionSource, content)
}

@Composable
fun ModernTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null, border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) {
    FilledTonalButton(onClick, modifier.heightIn(min = 48.dp), enabled, RoundedCornerShape(16.dp), colors, null, border, contentPadding, interactionSource, content)
}

@Composable
fun ModernChoiceRow(text: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true, label: String? = null, modifier: Modifier = Modifier, feedback: Boolean? = null) {
    val container = when (feedback) { false -> MaterialTheme.colorScheme.errorContainer; true -> MaterialTheme.colorScheme.primaryContainer; null -> if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface }
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 62.dp), shape = RoundedCornerShape(16.dp), color = container, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            label?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Default.CheckCircle, "已選擇", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    indicator: @Composable (List<TabPosition>) -> Unit = {},
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit
) {
    TabRow(selectedTabIndex = selectedTabIndex, modifier = modifier.clip(RoundedCornerShape(18.dp)), containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { positions ->
            if (selectedTabIndex in positions.indices) {
                val position = positions[selectedTabIndex]
                Box(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomStart).offset(x = position.left + 14.dp).width((position.width - 28.dp).coerceAtLeast(0.dp)).height(3.dp).clip(RoundedCornerShape(2.dp)).then(Modifier)) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }, divider = {}, tabs = tabs)
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = colors, elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = border, content = content)
}

@Composable
fun ModernCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = RoundedCornerShape(20.dp), colors = colors, elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = border, interactionSource = interactionSource, content = content)
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(if (leadingIcon != null && singleLine) 28.dp else 16.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent
    )
) {
    // Keep modifier/focus/menu anchors on the input itself, including editor scroll targets.
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier,
        enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label,
        placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        supportingText = supportingText, isError = isError, visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, singleLine = singleLine,
        maxLines = maxLines, minLines = minLines, interactionSource = interactionSource,
        shape = RoundedCornerShape(if (singleLine && label == null && placeholder != null) 28.dp else 16.dp), colors = colors)
}

/** Shared layout for vocabulary folders and grammar libraries, with actions kept in reach. */
@Composable
fun ModernCollectionRow(
    title: String,
    subtitle: String,
    count: Int,
    due: Int? = null,
    progress: Float? = null,
    mode: String = "manage",
    grammar: Boolean = false,
    onManage: () -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    dragModifier: Modifier? = null,
    primaryActionModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val primaryAction = when (mode) { "learn" -> onLearn; "quiz" -> onQuiz; else -> onManage }
    Surface(onClick = primaryAction, enabled = mode == "manage" || count > 0, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.then(primaryActionModifier).fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(if (grammar) Icons.Default.MenuBook else Icons.Default.Folder, null, Modifier.padding(9.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(buildString { append("$count ${if (grammar) "個文法" else "張單字"}"); due?.let { append(" · 待複習 $it") } }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            progress?.let {
                Column(Modifier.width(34.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${(it.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                }
            }
            dragModifier?.let { Icon(Icons.Default.DragHandle, "拖曳排序", it.size(28.dp).padding(3.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, "更多操作") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("管理") }, onClick = { menu = false; onManage() })
                    DropdownMenuItem(text = { Text("學習") }, enabled = count > 0, onClick = { menu = false; onLearn() })
                    DropdownMenuItem(text = { Text("測驗") }, enabled = count > 0, onClick = { menu = false; onQuiz() })
                    onEdit?.let { action -> DropdownMenuItem(text = { Text("編輯") }, onClick = { menu = false; action() }) }
                    onDelete?.let { action -> DropdownMenuItem(text = { Text("刪除", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; action() }) }
                }
            }
        }
    }
}

@Composable
fun ModernListRow(title: String, subtitle: String? = null, icon: ImageVector? = null, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon?.let { Icon(it, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            if (trailing != null) trailing() else Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Scrollable bottom-aligned dialog: no old oversized centre panel, safe with keyboard. */
@Composable
fun ModernAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    properties: DialogProperties = DialogProperties()
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnBackPress = properties.dismissOnBackPress, dismissOnClickOutside = properties.dismissOnClickOutside, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismissRequest).safeDrawingPadding().imePadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
            Surface(modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 640.dp).clickable(onClick = {}), shape = shape, color = containerColor, tonalElevation = tonalElevation) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp), shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.outlineVariant) {}
                    icon?.let { CompositionLocalProvider(LocalContentColor provides iconContentColor) { it() } }
                    title?.let { CompositionLocalProvider(LocalContentColor provides titleContentColor) { ProvideTextStyle(MaterialTheme.typography.titleLarge) { it() } } }
                    text?.let { Box(Modifier.weight(1f, fill = false)) { CompositionLocalProvider(LocalContentColor provides textContentColor) { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() } } } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { dismissButton?.invoke(); Spacer(Modifier.width(8.dp)); confirmButton() }
                }
            }
        }
    }
}
