package com.example.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * A dedicated drag target for list reordering. Only this handle consumes the drag,
 * so dragging anywhere else on the card continues to scroll the surrounding list.
 */
@Composable
fun ReorderDragHandle(
    enabled: Boolean = true,
    onDragStart: () -> Unit,
    onMoveOneStep: (direction: Int) -> Boolean,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .pointerInput(enabled, thresholdPx) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                        onDragStart()
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        onDragEnd()
                    },
                    onDragEnd = {
                        accumulatedDrag = 0f
                        onDragEnd()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        while (accumulatedDrag >= thresholdPx) {
                            if (!onMoveOneStep(1)) break
                            accumulatedDrag -= thresholdPx
                        }
                        while (accumulatedDrag <= -thresholdPx) {
                            if (!onMoveOneStep(-1)) break
                            accumulatedDrag += thresholdPx
                        }
                    }
                )
            }
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "拖曳調整順序",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
