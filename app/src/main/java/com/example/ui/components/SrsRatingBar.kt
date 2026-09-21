package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Flashcard
import com.example.data.srs.SrsCalculator

@Composable
fun SrsRatingBar(
    card: Flashcard,
    onRatingSelected: (Int) -> Unit,
    advancedMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rating 1: Not Mastered
        SrsRatingButton(
            label = "不熟",
            interval = SrsCalculator.getIntervalText(card, 1),
            color = MaterialTheme.colorScheme.error,
            onClick = { onRatingSelected(1) },
            modifier = Modifier.weight(1f).testTag("rating_button_again")
        )

        if (advancedMode) {
            SrsRatingButton(
                label = "困難",
                interval = SrsCalculator.getIntervalText(card, 2),
                color = MaterialTheme.colorScheme.tertiary,
                onClick = { onRatingSelected(2) },
                modifier = Modifier.weight(1f).testTag("rating_button_hard")
            )
        }

        SrsRatingButton(
            label = if (advancedMode) "良好" else "記得",
            interval = SrsCalculator.getIntervalText(card, 3),
            color = MaterialTheme.colorScheme.primary,
            onClick = { onRatingSelected(3) },
            modifier = Modifier.weight(1f).testTag("rating_button_good")
        )

        if (advancedMode) {
            SrsRatingButton(
                label = "簡單",
                interval = SrsCalculator.getIntervalText(card, 4),
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onRatingSelected(4) },
                modifier = Modifier.weight(1f).testTag("rating_button_easy")
            )
        }
    }
}

@Composable
private fun SrsRatingButton(
    label: String,
    interval: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonContentColor = contentColorFor(color)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = buttonContentColor
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = interval,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = buttonContentColor.copy(alpha = 0.85f)
            )
        }
    }
}
