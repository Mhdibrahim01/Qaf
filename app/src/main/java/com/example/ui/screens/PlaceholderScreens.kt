package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.ArabicText
import com.example.ui.components.BilingualPreviewCard
import com.example.ui.components.GoldBadge
import com.example.ui.navigation.MainSection
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable Phase 1 placeholder screen for each of the 7 sections.
 * Demonstrates the dark theme (#0b0b0c / #141416 / #1a1a1c), borders (#26262a),
 * gold accents (#F5C542), English (Inter) + Arabic (Cairo/Tajawal) RTL rendering,
 * and a clear blueprint of what will be built in this section.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionPlaceholderScreen(
    section: MainSection,
    plannedHighlights: List<Pair<String, String>>,
    sampleBilingualMixedText: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_${section.name.lowercase()}"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Hero Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccentDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = section.selectedIcon,
                                    contentDescription = section.titleEn,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = section.titleEn,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Phase 1 • Navigation & Theme Shell",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                            }
                        }

                        GoldBadge(text = section.badgeTag)
                    }

                    // Bilingual English + Arabic RTL Card
                    BilingualPreviewCard(
                        englishTitle = "${section.titleEn} Overview",
                        englishSubtitle = section.subtitleEn,
                        arabicTitle = section.titleAr,
                        arabicDescription = section.descriptionAr,
                        tagText = "RTL Ready",
                        elevated = true
                    )
                }
            }

            // Mixed Arabic / English & Gold Token Verification Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Typography & Mixed Bidi Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        GoldBadge(
                            text = "US /ˈwɜːrd.bʊks/",
                            icon = Icons.AutoMirrored.Filled.VolumeUp
                        )
                    }

                    // Highlighted English sentence with gold keyword
                    Text(
                        text = buildAnnotatedString {
                            append("Consistent daily practice builds lasting ")
                            withStyle(
                                SpanStyle(
                                    color = GoldAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append("fluency")
                            }
                            append(" in English.")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )

                    // Mixed Arabic + English RTL line
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceElevatedDark,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ArabicText(
                                text = sampleBilingualMixedText,
                                style = ArabicTypography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    // Planned features chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plannedHighlights.forEach { (enLabel, arLabel) ->
                            Surface(
                                color = SurfaceElevatedDark,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$enLabel • $arLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HomePlaceholderScreen(
    decks: List<com.example.data.local.DeckEntity> = emptyList(),
    importState: com.example.data.importer.ImportProgressState = com.example.data.importer.ImportProgressState(),
    totalEntriesCount: Int = 0,
    reviewDueTodayCount: Int = 0,
    studiedTodayCount: Int = 0,
    masteredCount: Int = 0,
    deckStudyStates: List<com.example.data.local.DeckStudyStateEntity> = emptyList(),
    todayChecklist: com.example.data.local.DailyChecklistEntity? = null,
    studyStreak: Int = 0,
    onOpenDeck: (String) -> Unit = {},
    onOpenFlashcardsForDeck: (String) -> Unit = {},
    onOpenPlanScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    HomeScreen(
        decks = decks,
        importState = importState,
        totalEntriesCount = totalEntriesCount,
        reviewDueTodayCount = reviewDueTodayCount,
        studiedTodayCount = studiedTodayCount,
        masteredCount = masteredCount,
        deckStudyStates = deckStudyStates,
        todayChecklist = todayChecklist,
        studyStreak = studyStreak,
        onOpenDeck = onOpenDeck,
        onOpenFlashcardsForDeck = onOpenFlashcardsForDeck,
        onOpenPlanScreen = onOpenPlanScreen,
        modifier = modifier
    )
}

@Composable
fun WordBooksPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.WORD_BOOKS,
        plannedHighlights = listOf(
            "Colored Book Covers" to "أغلفة ملونة",
            "Oxford 5000 Merged Deck" to "قاموس أكسفورد 5000",
            "US IPA + Audio" to "النطق الصوتي IPA",
            "POS Meanings (n., v., adj., web.)" to "تصنيف المعاني",
            "Mastered & Favorite" to "المتقنة والمفضلة"
        ),
        sampleBilingualMixedText = "كلمة \"resilient\" (adj.) تعني: مرن / قادر على التكيف — المستوى C1 في Oxford 5000.",
        modifier = modifier
    )
}

@Composable
fun FlashcardsPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.FLASHCARDS,
        plannedHighlights = listOf(
            "Swipe Gestures" to "إيماءات السحب",
            "Gold 1 / N Counter" to "عداد البطاقات الذهبي",
            "Highlighted Examples" to "أمثلة مميزة بالذهبي",
            "AI Detailed Definition" to "شرح تفصيلي بالذكاء الاصطناعي",
            "SRS: Again / Hard / Good / Easy" to "التكرار المتباعد"
        ),
        sampleBilingualMixedText = "البطاقة 1 / 20 • اضغط على AI Definition لعرض الشرح التفصيلي والمقاطع الصوتية.",
        modifier = modifier
    )
}

@Composable
fun SentencesPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.SENTENCES,
        plannedHighlights = listOf(
            "Favorites (with count)" to "المفضلة",
            "Dictation Tab" to "الإملاء",
            "Speaking Tab" to "التحدث",
            "Show/Hide Translation" to "إظهار/إخفاء الترجمة",
            "AI Explain Sparkle" to "تحليل الجملة ذكياً"
        ),
        sampleBilingualMixedText = "تبويبات الجمل: Favorites (12) • Dictation • Speaking مع شرح القواعد بالذكاء الاصطناعي.",
        modifier = modifier
    )
}

@Composable
fun GrammarPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.GRAMMAR,
        plannedHighlights = listOf(
            "CEFR Levels (A1–C1)" to "المستويات A1-C1",
            "Tenses & Conditionals" to "الأزمنة والشرط",
            "Simple Arabic Explanations" to "شرح عربي مبسط",
            "Multiple Choice & Reorder" to "تمارين اختيار وترتيب"
        ),
        sampleBilingualMixedText = "درس Present Perfect (المستوى B1): يُستخدم للتعبير عن حدث بدأ في الماضي وله أثر في الحاضر.",
        modifier = modifier
    )
}

@Composable
fun ReadingPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.READING,
        plannedHighlights = listOf(
            "Graded Texts (A1–C1)" to "نصوص متدرجة",
            "Tap Any Word Lookup" to "اضغط على أي كلمة لمعناها",
            "Sentence Translation Toggle" to "ترجمة الجمل",
            "Full Text Audio & Quiz" to "استماع كامل وأسئلة فهم"
        ),
        sampleBilingualMixedText = "انقر على أي كلمة إنجليزية أثناء القراءة لعرض النطق IPA والمعنى العربي وإضافتها لبطاقاتك.",
        modifier = modifier
    )
}

@Composable
fun PlanPlaceholderScreen(modifier: Modifier = Modifier) {
    SectionPlaceholderScreen(
        section = MainSection.PLAN,
        plannedHighlights = listOf(
            "Monthly Goals & Weekly Focus" to "الأهداف الشهرية والتركيز الأسبوعي",
            "Adaptive Daily Checklist" to "قائمة مهام يومية ذكية",
            "Multi-Skill Quizzes" to "اختبارات متعددة المهارات",
            "Streak & Analytics" to "سلسلة الأيام والتحليلات البيانية"
        ),
        sampleBilingualMixedText = "خطة دراسية شهرية ويومية ذكية تتكيف مع أدائك وتتبع تقدمك عبر التكرار المتباعد والاختبارات التفاعلية.",
        modifier = modifier
    )
}
