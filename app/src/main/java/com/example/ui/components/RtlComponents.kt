package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Right-aligned, RTL-correct Arabic text using Cairo / Tajawal font families.
 * Handles mixed Arabic/English punctuation and bidirectional runs properly using BidiText.
 */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ArabicTypography.bodyLarge,
    color: Color = TextPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    fillMaxWidth: Boolean = true
) {
    BidiText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        fillMaxWidth = fillMaxWidth
    )
}

/**
 * Bilingual card pairing English (Inter, LTR) and Arabic (Cairo/Tajawal, RTL)
 * with subtle border (#26262a) and dark surface (#141416 / #1a1a1c).
 */
@Composable
fun BilingualPreviewCard(
    englishTitle: String,
    englishSubtitle: String,
    arabicTitle: String,
    arabicDescription: String,
    tagText: String,
    modifier: Modifier = Modifier,
    elevated: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (elevated) SurfaceElevatedDark else SurfaceDark,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = englishTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                GoldBadge(text = tagText)
            }

            Text(
                text = englishSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceElevatedDark,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ArabicText(
                        text = arabicTitle,
                        style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    ArabicText(
                        text = arabicDescription,
                        style = ArabicTypography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Gold accent badge for tags, counters, CEFR levels, and IPA chips.
 */
@Composable
fun GoldBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = GoldAccentDim,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GoldAccent
            )
        }
    }
}
