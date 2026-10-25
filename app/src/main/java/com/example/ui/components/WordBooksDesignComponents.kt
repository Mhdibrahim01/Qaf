package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.BottomNavDestination
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * 1. WordBooksTopAppBar: Refined Dark Luxury top app bar with clean English title,
 * subtle subtitle, optional back navigation, and right-aligned actions.
 */
@Composable
fun WordBooksTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    arabicTitle: String? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = BackgroundDark.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading section: Back button OR WB Monogram + Clean English Titles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (canNavigateBack && onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevatedDark)
                                .testTag("btn_top_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            GoldAccent.copy(alpha = 0.22f),
                                            GoldAccent.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(1.dp, GoldAccentBorder, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ق",
                                style = ArabicTypography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = GoldAccent
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitleContent != null) {
                            subtitleContent()
                        } else if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Trailing section: Action icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actions()
                }
            }
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        }
    }
}

/**
 * 2. WordBooksBottomNavigation: 5-tab bottom navigation for phones (< 600dp).
 * Destinations: Home, Books, Cards, Sentences, Hub.
 */
@Composable
fun WordBooksBottomNavigation(
    currentRoute: AppRoute?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_navigation_bar")
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavDestination.entries.forEach { destination ->
                    val isSelected = when (destination) {
                        BottomNavDestination.HOME -> currentRoute == AppRoute.Home
                        BottomNavDestination.WORD_BOOKS -> currentRoute == AppRoute.WordBooks || currentRoute == AppRoute.Flashcards
                        BottomNavDestination.GRAMMAR -> currentRoute == AppRoute.Grammar
                        BottomNavDestination.READING -> currentRoute == AppRoute.Reading
                        BottomNavDestination.MORE -> currentRoute == AppRoute.More ||
                            currentRoute in listOf(
                                AppRoute.Sentences,
                                AppRoute.Plan,
                                AppRoute.CorrectionNotebook,
                                AppRoute.Settings,
                                AppRoute.Labs
                            )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                role = Role.Tab,
                                onClick = { onNavigateToDestination(destination) }
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag(destination.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) GoldAccentDim else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.titleEn,
                                tint = if (isSelected) GoldAccent else TextMuted,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = destination.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) GoldAccent else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. WordBooksNavigationRail: Side navigation rail for tablets and landscape layouts (>= 600dp).
 */
@Composable
fun WordBooksNavigationRail(
    currentRoute: AppRoute?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        modifier = modifier
            .fillMaxHeight()
            .width(84.dp)
            .testTag("side_navigation_rail")
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(GoldAccentDim)
                .border(1.dp, GoldAccentBorder, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ق",
                style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = GoldAccent
            )
        }

        BottomNavDestination.entries.forEach { destination ->
            val isSelected = when (destination) {
                BottomNavDestination.HOME -> currentRoute == AppRoute.Home
                BottomNavDestination.WORD_BOOKS -> currentRoute == AppRoute.WordBooks || currentRoute == AppRoute.Flashcards
                BottomNavDestination.GRAMMAR -> currentRoute == AppRoute.Grammar
                BottomNavDestination.READING -> currentRoute == AppRoute.Reading
                BottomNavDestination.MORE -> currentRoute == AppRoute.More ||
                    currentRoute in listOf(
                        AppRoute.Sentences,
                        AppRoute.Plan,
                        AppRoute.CorrectionNotebook,
                        AppRoute.Settings,
                        AppRoute.Labs
                    )
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.titleEn,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = destination.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = GoldAccent,
                    selectedTextColor = GoldAccent,
                    indicatorColor = GoldAccentDim,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("${destination.testTag}_rail")
            )
        }
    }
}

/**
 * 3b. BooksModeSwitcher: Segmented switcher inside the Books tab allowing users to switch
 * effortlessly between Word Books (Library & Dictionary List) and Flashcards (Study Cards).
 */
@Composable
fun BooksModeSwitcher(
    isFlashcardsActive: Boolean,
    dueTodayCount: Int = 0,
    onSelectBooksMode: () -> Unit,
    onSelectFlashcardsMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("books_mode_switcher")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segment 1: Word Books & Dictionary
            val booksSelected = !isFlashcardsActive
            Surface(
                onClick = onSelectBooksMode,
                shape = RoundedCornerShape(10.dp),
                color = if (booksSelected) GoldAccentDim else Color.Transparent,
                border = if (booksSelected) BorderStroke(1.dp, GoldAccentBorder) else null,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .testTag("mode_tab_books")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = if (booksSelected) GoldAccent else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Word Books",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (booksSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (booksSelected) GoldAccent else TextSecondary
                    )
                }
            }

            // Segment 2: Study Cards (Flashcards)
            val cardsSelected = isFlashcardsActive
            Surface(
                onClick = onSelectFlashcardsMode,
                shape = RoundedCornerShape(10.dp),
                color = if (cardsSelected) GoldAccentDim else Color.Transparent,
                border = if (cardsSelected) BorderStroke(1.dp, GoldAccentBorder) else null,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .testTag("mode_tab_flashcards")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Style,
                        contentDescription = null,
                        tint = if (cardsSelected) GoldAccent else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flashcards",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (cardsSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (cardsSelected) GoldAccent else TextSecondary
                    )
                    if (dueTodayCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (cardsSelected) GoldAccent.copy(alpha = 0.22f) else SurfaceElevatedDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$dueTodayCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (cardsSelected) GoldAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. ScreenHeader: Clean, compact section banner with icon, English title,
 * concise subtitle, and optional badge tag.
 */
@Composable
fun ScreenHeader(
    titleEn: String,
    modifier: Modifier = Modifier,
    titleAr: String? = null,
    subtitleEn: String? = null,
    badgeText: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = GoldAccent
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (!subtitleEn.isNullOrBlank()) {
                        Text(
                            text = subtitleEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                GoldBadge(text = badgeText)
            }
        }
    }
}

/**
 * 5. LearningStatCard: Clean metric tile with large accented counter and crisp English label.
 */
@Composable
fun LearningStatCard(
    value: String,
    labelEn: String,
    modifier: Modifier = Modifier,
    labelAr: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = GoldAccent,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val cardModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .then(
            if (onClick != null) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            }
        )

    Surface(
        modifier = cardModifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = accentColor,
                    maxLines = 1
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = labelEn,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 6. StudyTaskCard: Card for learning tasks (checklist items, lessons, exercises).
 */
@Composable
fun StudyTaskCard(
    titleEn: String,
    modifier: Modifier = Modifier,
    titleAr: String? = null,
    descriptionEn: String? = null,
    tagText: String? = null,
    tagColor: Color = GoldAccent,
    icon: ImageVector? = null,
    iconTint: Color = GoldAccent,
    progress: Float? = null,
    isCompleted: Boolean = false,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .then(
            if (onClick != null) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            }
        )

    Surface(
        modifier = cardModifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCompleted) EmeraldAccent.copy(alpha = 0.35f) else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconTint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (!titleAr.isNullOrBlank()) {
                            ArabicText(
                                text = titleAr,
                                style = ArabicTypography.bodySmall,
                                color = if (isCompleted) EmeraldAccent else TextSecondary
                            )
                        }
                    }
                }

                if (!tagText.isNullOrBlank()) {
                    ProgressBadge(text = tagText, color = tagColor)
                }
            }

            if (!descriptionEn.isNullOrBlank()) {
                Text(
                    text = descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isCompleted) EmeraldAccent else GoldAccent,
                    trackColor = SurfaceElevatedDark
                )
            }

            if (actionText != null && onActionClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) SurfaceElevatedDark else GoldAccent,
                            contentColor = if (isCompleted) EmeraldAccent else OnGoldDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(actionText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 7. DeckListItem: Displays a Word Book card with cover accent, entry count,
 * saved flashcard position, and clear actions.
 */
@Composable
fun DeckListItem(
    titleEn: String,
    titleAr: String,
    countText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = GoldAccent,
    savedPositionText: String? = null,
    onListClick: (() -> Unit)? = null,
    onStudyClick: (() -> Unit)? = null,
    testTagStudyButton: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (!savedPositionText.isNullOrBlank()) "$countText • $savedPositionText" else countText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onListClick != null) {
                    androidx.compose.material3.TextButton(
                        onClick = onListClick,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Browse", color = TextSecondary)
                    }
                }
                if (onStudyClick != null) {
                    val studyBtnMod = Modifier
                        .heightIn(min = 44.dp)
                        .then(
                            if (testTagStudyButton != null) {
                                Modifier.testTag(testTagStudyButton)
                            } else {
                                Modifier
                            }
                        )
                    Button(
                        onClick = onStudyClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccentDim,
                            contentColor = GoldAccent
                        ),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = studyBtnMod
                    ) {
                        Text("Study", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 8. ProgressBadge: Compact semantic badge for progress, level chips, tags.
 */
@Composable
fun ProgressBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GoldAccent,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/**
 * 9. PrimaryActionButton: Prominent gold primary button with minimum 48dp touch target.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = GoldAccent,
    contentColor: Color = OnGoldDark,
    testTag: String? = null
) {
    val btnModifier = modifier
        .heightIn(min = 48.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SurfaceElevatedDark,
            disabledContentColor = TextMuted
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = btnModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * 10. SearchField: Polished search field with dark elevated background and subtle border.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search words, meanings, or topics...",
    onClear: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    testTag: String? = null
) {
    val focusManager = LocalFocusManager.current
    val fieldModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = fieldModifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                onSearch?.invoke()
            }
        ),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = GoldAccent,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onClear?.invoke() ?: onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedBorderColor = GoldAccentBorder,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = GoldAccent
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * 11. EmptyState: Clean empty state with icon, message, and optional action.
 */
@Composable
fun EmptyState(
    titleEn: String,
    modifier: Modifier = Modifier,
    titleAr: String? = null,
    descriptionEn: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val stateModifier = modifier
        .fillMaxWidth()
        .padding(32.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    Column(
        modifier = stateModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(GoldAccentDim)
                    .border(1.dp, GoldAccentBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titleEn,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            if (!descriptionEn.isNullOrBlank()) {
                Text(
                    text = descriptionEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            PrimaryActionButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}

/**
 * 12. LoadingState: Centered loading spinner in gold with message.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    messageEn: String = "Loading...",
    messageAr: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = GoldAccent,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = messageEn,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

/**
 * 13. ErrorState: Coral-themed error display with explanation and retry action.
 */
@Composable
fun ErrorState(
    messageEn: String,
    modifier: Modifier = Modifier,
    messageAr: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = CoralAccent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = CoralAccent,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = messageEn,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CoralAccent,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
