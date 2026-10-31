package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.blitz.BlitzFilterConfig
import com.example.data.local.DeckEntity
import com.example.ui.components.BidiText
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.PillShape
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.bi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabularyBlitzSetupDialog(
    initialConfig: BlitzFilterConfig,
    availableDecks: List<DeckEntity>,
    userCurrentLevel: String = "B2",
    highScore: Int = 0,
    availableWordCount: Int,
    isSurvivalMode: Boolean = false,
    bestRecordValue: Int = highScore,
    onFilterConfigChanged: (BlitzFilterConfig) -> Unit,
    onStartChallenge: (BlitzFilterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDecks by remember(initialConfig.selectedDecks) { mutableStateOf(initialConfig.selectedDecks) }
    var allBooksSelected by remember(initialConfig.allBooksSelected) { mutableStateOf(initialConfig.allBooksSelected) }
    var selectedLevels by remember(initialConfig.selectedLevels) { mutableStateOf(initialConfig.selectedLevels) }
    var studiedOnly by remember(initialConfig.studiedOnly) { mutableStateOf(initialConfig.studiedOnly) }

    val updateConfig: () -> Unit = {
        val config = BlitzFilterConfig(
            selectedDecks = selectedDecks,
            allBooksSelected = allBooksSelected,
            selectedLevels = selectedLevels,
            studiedOnly = studiedOnly
        )
        onFilterConfigChanged(config)
    }

    val cefrLevels = listOf("A1", "A2", "B1", "B2", "C1")
    val isStartEnabled = availableWordCount >= 10

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .testTag("blitz_setup_dialog"),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                // Top Bar: Title + Personal Best + Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSurvivalMode) CoralAccent.copy(alpha = 0.2f) else GoldAccentDim,
                            border = BorderStroke(1.5.dp, if (isSurvivalMode) CoralAccent else GoldAccentBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSurvivalMode) Icons.Default.Favorite else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isSurvivalMode) CoralAccent else GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (isSurvivalMode) {
                                    bi("Survival Challenge ❤️", "تحدي النجاة (3 قلوب) ❤️")
                                } else {
                                    bi("60-Second Speed Challenge", "تحدي الـ 60 ثانية السريع ⚡")
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (isSurvivalMode) {
                                    bi("Untimed • 3 Hearts Survival", "تحدي مفتوح بدون وقت مع 3 قلوب")
                                } else {
                                    bi("Pre-Challenge Setup • \u2067\u200Fإعدادات التحدي\u200F\u2069", "خصص الكلمات والمستوى قبل الانطلاق")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("blitz_setup_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close setup",
                            tint = TextSecondary
                        )
                    }
                }

                // Scrollable Setup Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Stats Banner: Fixed Duration (60s) + Personal Best High Score
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Duration / Lives Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSurvivalMode) Icons.Default.Favorite else Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isSurvivalMode) bi("Mode & Lives", "الوضع والقلوب") else bi("Fixed Duration", "مدة ثابتة ومضبوطة"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = if (isSurvivalMode) bi("3 Hearts Survival ❤️", "٣ قلوب نجاة ❤️") else bi("60 Seconds Sprint", "٦٠ ثانية بالضبط ⏱️"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TextPrimary
                                    )
                                }
                            }

                            // High Score / Personal Best Badge
                            Surface(
                                color = GoldAccentDim,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                modifier = Modifier.testTag("blitz_setup_high_score_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isSurvivalMode) bi("BEST STREAK", "أعلى سلسلة") else bi("PERSONAL BEST", "أعلى نتيجة"),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = GoldAccent
                                        )
                                        Text(
                                            text = if (isSurvivalMode) "$bestRecordValue" else "$highScore pts",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Book Selection (Multi-Select Books)
                    SetupSectionCard(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        titleEn = "Select Word Books",
                        titleAr = "اختيار الكتب"
                    ) {
                        // "All Selected Books" Chip Shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SetupFilterChip(
                                label = bi("All Books / All Selected", "جميع الكتب المختارة 📚"),
                                isSelected = allBooksSelected,
                                onClick = {
                                    allBooksSelected = !allBooksSelected
                                    if (allBooksSelected) {
                                        selectedDecks = emptyList()
                                    }
                                    updateConfig()
                                },
                                testTag = "blitz_book_all_chip"
                            )
                        }

                        // Individual Book 2-Column Equal-Width Grid (without character slicing)
                        val filteredDecks = availableDecks.filter { it.id != "saved_words" }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredDecks.chunked(2).forEach { rowDecks ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowDecks.forEach { deck ->
                                        val isSelected = !allBooksSelected && deck.id in selectedDecks
                                        DeckSelectionCard(
                                            deck = deck,
                                            isSelected = isSelected,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .heightIn(min = 64.dp),
                                            onClick = {
                                                allBooksSelected = false
                                                selectedDecks = if (deck.id in selectedDecks) {
                                                    selectedDecks - deck.id
                                                } else {
                                                    selectedDecks + deck.id
                                                }
                                                if (selectedDecks.isEmpty()) {
                                                    allBooksSelected = true
                                                }
                                                updateConfig()
                                            },
                                            testTag = "blitz_book_${deck.id}"
                                        )
                                    }
                                    if (rowDecks.size == 1) {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: CEFR Level Filter (Multi-Select)
                    SetupSectionCard(
                        icon = Icons.Default.School,
                        titleEn = "CEFR Level Filter",
                        titleAr = "تحديد المستوى"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bi("Target Levels:", "المستويات المستهدفة:"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )

                            // Quick Button: "My Current Level"
                            Surface(
                                color = SkyAccent.copy(alpha = 0.16f),
                                shape = PillShape,
                                border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(PillShape)
                                    .clickable {
                                        selectedLevels = setOf(userCurrentLevel)
                                        updateConfig()
                                    }
                                    .testTag("blitz_my_level_btn")
                            ) {
                                Text(
                                    text = bi("My Level: $userCurrentLevel 🎯", "مستواي الحالي: $userCurrentLevel 🎯"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SkyAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            cefrLevels.forEach { level ->
                                val isSelected = level in selectedLevels
                                SetupFilterChip(
                                    label = level,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedLevels = if (isSelected) {
                                            if (selectedLevels.size > 1) selectedLevels - level else selectedLevels
                                        } else {
                                            selectedLevels + level
                                        }
                                        updateConfig()
                                    },
                                    testTag = "blitz_level_$level"
                                )
                            }
                        }
                    }

                    // Section 3: Word Lifecycle Stage Filter
                    SetupSectionCard(
                        icon = Icons.Default.Layers,
                        titleEn = "Word Lifecycle Stage",
                        titleAr = "حالة وحداثة الكلمات"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Option A: Studied / Active Words Only
                            LifecycleRadioCard(
                                titleEn = "Studied / Active Words Only",
                                titleAr = "الكلمات المدروسة سابقاً فقط (تثبيت الحفظ)",
                                subtitleEn = "Seen, Learning, Familiar & Mature stages",
                                subtitleAr = "يشمل المفردات قيد الدراسة والمراجعة السابقة",
                                isSelected = studiedOnly,
                                onClick = {
                                    studiedOnly = true
                                    updateConfig()
                                },
                                testTag = "blitz_stage_studied_only"
                            )

                            // Option B: All Words Including New
                            LifecycleRadioCard(
                                titleEn = "All Words (Including New)",
                                titleAr = "جميع الكلمات شاملة الكلمات الجديدة",
                                subtitleEn = "Complete vocabulary pool from chosen books",
                                subtitleAr = "سحب شامل من كافة محتوى الكتب والمستويات المختارة",
                                isSelected = !studiedOnly,
                                onClick = {
                                    studiedOnly = false
                                    updateConfig()
                                },
                                testTag = "blitz_stage_all_words"
                            )
                        }
                    }

                    // Available Words Live Counter Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blitz_available_words_card"),
                        color = if (isStartEnabled) SurfaceDark else CoralAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (isStartEnabled) EmeraldAccent.copy(alpha = 0.6f) else CoralAccent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isStartEnabled) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isStartEnabled) EmeraldAccent else CoralAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = bi("Available Matching Words:", "الكلمات المطابقة للفلاتر:"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    if (!isStartEnabled) {
                                        Text(
                                            text = bi("At least 10 words required to start", "يلزم 10 كلمات على الأقل لبدء التحدي"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = CoralAccent
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "$availableWordCount words",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isStartEnabled) EmeraldAccent else CoralAccent,
                                modifier = Modifier.testTag("blitz_available_words_count")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Sticky Footer: Bottom Action Buttons with safe bottom clearance for Dialog window overhang + gesture bar
                val screenHeightDp = LocalConfiguration.current.screenHeightDp
                val footerBottomPadding = if (screenHeightDp >= 640) 48.dp else 12.dp
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BackgroundDark
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = BorderDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = footerBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceDark,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, BorderDark),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.36f)
                                    .height(50.dp)
                                    .testTag("blitz_setup_cancel_btn")
                            ) {
                                Text(
                                    text = bi("Cancel", "إلغاء"),
                                    color = TextPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val buttonBgColor = if (isSurvivalMode) CoralAccent else GoldAccent
                            val darkTextColor = Color(0xFF0B0D12)

                            Button(
                                onClick = {
                                    val finalConfig = BlitzFilterConfig(
                                        selectedDecks = selectedDecks,
                                        allBooksSelected = allBooksSelected,
                                        selectedLevels = selectedLevels,
                                        studiedOnly = studiedOnly
                                    )
                                    onStartChallenge(finalConfig)
                                },
                                enabled = isStartEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonBgColor,
                                    contentColor = darkTextColor,
                                    disabledContainerColor = SurfaceElevatedDark,
                                    disabledContentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.64f)
                                    .height(50.dp)
                                    .testTag("blitz_setup_start_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSurvivalMode) Icons.Default.Favorite else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isStartEnabled) darkTextColor else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSurvivalMode) {
                                        bi("Start Survival", "بدء النجاة")
                                    } else {
                                        bi("Start 60s Challenge", "بدء التحدي (60 ثانية)")
                                    },
                                    color = if (isStartEnabled) darkTextColor else TextMuted,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupSectionCard(
    icon: ImageVector,
    titleEn: String,
    titleAr: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = titleAr,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Rtl
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun DeckSelectionCard(
    deck: DeckEntity,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (isSelected) GoldAccent.copy(alpha = 0.18f) else SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = deck.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                ),
                color = if (isSelected) GoldAccent else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (deck.arabicSubtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                BidiText(
                    text = deck.arabicSubtitle,
                    style = ArabicTypography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = if (isSelected) Color(0xFFFDE08D) else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Left,
                    fillMaxWidth = true
                )
            }
        }
    }
}

@Composable
private fun SetupFilterChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (isSelected) GoldAccent.copy(alpha = 0.20f) else SurfaceElevatedDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) GoldAccent else TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LifecycleRadioCard(
    titleEn: String,
    titleAr: String,
    subtitleEn: String,
    subtitleAr: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (isSelected) GoldAccent.copy(alpha = 0.16f) else SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (isSelected) 1.6.dp else 1.dp,
            if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) GoldAccent else SurfaceDark,
                border = BorderStroke(1.5.dp, if (isSelected) GoldAccent else BorderDark),
                modifier = Modifier.size(20.dp)
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BackgroundDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BidiText(
                    text = "$titleEn • $titleAr",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = if (isSelected) GoldAccent else TextPrimary
                )
                BidiText(
                    text = "$subtitleEn • $subtitleAr",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}
