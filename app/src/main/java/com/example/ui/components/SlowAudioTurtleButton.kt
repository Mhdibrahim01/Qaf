package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Slow Pronunciation ("Turtle" `0.6x`) button paired next to normal speaker controls
 * across Flashcards, Reading, Sentences, and Word Detail panels.
 */
@Composable
fun SlowAudioTurtleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlayingSlow: Boolean = false,
    showSpeedLabel: Boolean = true,
    testTag: String = "slow_audio_turtle_button"
) {
    val containerColor = if (isPlayingSlow) GoldAccentDim else SurfaceElevatedDark
    val borderColor = when {
        isPlayingSlow -> GoldAccentBorder
        !showSpeedLabel -> Color(0xFF10B981).copy(alpha = 0.35f)
        else -> BorderSubtle
    }
    val labelColor = if (isPlayingSlow) GoldAccent else TextSecondary

    Surface(
        modifier = modifier
            .then(if (showSpeedLabel) Modifier.height(34.dp) else Modifier.requiredSize(34.dp))
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Play slow pronunciation at 0.6x speed"
            }
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showSpeedLabel) 10.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (showSpeedLabel) Arrangement.spacedBy(4.dp) else Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = com.example.R.drawable.ic_turtle_flat),
                contentDescription = "Slow pronunciation",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(16.dp)
            )
            if (showSpeedLabel) {
                Text(
                    text = "0.6x",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
