package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BorderDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.SquircleCardShape
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Immutable
data class OnboardingDeckInfo(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val entryCount: Int,
    val levelBadge: String,
    val category: String, // "core" or "career"
    val descriptionEn: String,
    val descriptionAr: String
)

object OnboardingStep4Defaults {
    val ALL_DECKS = listOf(
        OnboardingDeckInfo(
            id = "oxford5000",
            title = "Oxford 5000",
            arabicTitle = "مفردات أكسفورد الأساسية",
            entryCount = 5948,
            levelBadge = "A1–C1",
            category = "core",
            descriptionEn = "Core vocabulary from everyday English to advanced C1",
            descriptionAr = "المفردات الأساسية المعتمدة من أكسفورد لجميع المستويات"
        ),
        OnboardingDeckInfo(
            id = "oxford_opal",
            title = "Academic OPAL",
            arabicTitle = "المفردات الأكاديمية \u2066(OPAL)\u2069",
            entryCount = 2096,
            levelBadge = "B2–C1",
            category = "core",
            descriptionEn = "Oxford Phrasal Academic Lexicon for essays and formal reading",
            descriptionAr = "المفردات الأكاديمية للمقالات والأبحاث والقراءة المتقدمة"
        ),
        OnboardingDeckInfo(
            id = "oxford_phrases",
            title = "Oxford Phrases",
            arabicTitle = "عبارات أكسفورد الشائعة",
            entryCount = 750,
            levelBadge = "A2–B2",
            category = "core",
            descriptionEn = "High-frequency conversational and functional phrases",
            descriptionAr = "عبارات وتركيبات متكررة في المحادثة والتواصل اليومي"
        ),
        OnboardingDeckInfo(
            id = "everyday_expressions",
            title = "Everyday Expressions",
            arabicTitle = "تعبيرات الحياة اليومية",
            entryCount = 600,
            levelBadge = "A2–B1",
            category = "core",
            descriptionEn = "Natural spoken expressions and social interactions",
            descriptionAr = "تعبيرات شائعة في المواقف الاجتماعية اليومية"
        ),
        OnboardingDeckInfo(
            id = "essential_idioms",
            title = "Essential Idioms",
            arabicTitle = "المصطلحات والتعبيرات الاصطلاحية",
            entryCount = 468,
            levelBadge = "B1–C1",
            category = "core",
            descriptionEn = "Authentic native idioms and figurative speech",
            descriptionAr = "مصطلحات مجازية طبيعية للمتحدثين الأصليين"
        ),
        OnboardingDeckInfo(
            id = "ielts_core",
            title = "IELTS Core",
            arabicTitle = "مفردات الآيلتس الأساسية",
            entryCount = 3575,
            levelBadge = "B1–C1",
            category = "career",
            descriptionEn = "Targeted vocabulary for IELTS Academic & General tests",
            descriptionAr = "مفردات مركزة لاختبار الآيلتس الأكاديمي والعام"
        ),
        OnboardingDeckInfo(
            id = "it_industry",
            title = "IT & Tech Industry",
            arabicTitle = "مصطلحات تكنولوجيا المعلومات والبرمجة",
            entryCount = 1622,
            levelBadge = "B1–C1",
            category = "career",
            descriptionEn = "Software engineering, cloud, standups, and tech specs",
            descriptionAr = "مصطلحات هندسة البرمجيات واجتماعات الفرق التقنية"
        ),
        OnboardingDeckInfo(
            id = "business_english",
            title = "Business English",
            arabicTitle = "اللغة الإنجليزية للأعمال",
            entryCount = 1250,
            levelBadge = "B1–C1",
            category = "career",
            descriptionEn = "Corporate emails, negotiations, and business presentations",
            descriptionAr = "المراسلات الإدارية والتفاوض والعروض التقديمية"
        ),
        OnboardingDeckInfo(
            id = "career_interview",
            title = "Job Interview & Career",
            arabicTitle = "المقابلات الوظيفية والمسار المهني",
            entryCount = 406,
            levelBadge = "B1–B2",
            category = "career",
            descriptionEn = "Interview responses, resume phrases, and workplace etiquette",
            descriptionAr = "إجابات المقابلات الوظيفية والمسار المهني والسيرة الذاتية"
        ),
        OnboardingDeckInfo(
            id = "cefr_c1",
            title = "CEFR C1 Advanced",
            arabicTitle = "المستوى المتقدم \u2066C1\u2069",
            entryCount = 1853,
            levelBadge = "C1",
            category = "career",
            descriptionEn = "Advanced expressive words for native-level precision",
            descriptionAr = "مفردات دقيقة وعميقة للوصول إلى الطلاقة المتقدمة"
        ),
        OnboardingDeckInfo(
            id = "pte_academic",
            title = "PTE Academic",
            arabicTitle = "مفردات اختبار \u2066PTE\u2069 الأكاديمي",
            entryCount = 2651,
            levelBadge = "B1–C1",
            category = "career",
            descriptionEn = "Pearson Test of English core academic lexicon",
            descriptionAr = "مفردات مركزة لاختبار بيرسون الأكاديمي"
        ),
        OnboardingDeckInfo(
            id = "medical_healthcare",
            title = "Medical & Healthcare",
            arabicTitle = "المصطلحات الطبية والرعاية الصحية",
            entryCount = 405,
            levelBadge = "B1–C1",
            category = "career",
            descriptionEn = "Clinical vocabulary and healthcare communication",
            descriptionAr = "المصطلحات الطبية ورعاية المرضى والصحة العامة"
        )
    )

    fun initialDecksForGoal(goal: String): List<String> {
        return when (goal.trim().lowercase()) {
            "ielts" -> listOf("oxford5000", "oxford_opal", "ielts_core")
            "it_career" -> listOf("oxford5000", "oxford_opal", "it_industry")
            else -> listOf("oxford5000", "oxford_opal")
        }
    }
}

@Composable
fun OnboardingDecksAndPlacementStep(
    selectedLevel: String,
    selectedGoal: String,
    initialSelectedDecks: List<String>,
    initialStartAtUserLevel: Boolean = true,
    onContinueStep4: (selectedDecks: List<String>, startAtUserLevel: Boolean) -> Unit,
    onStep4Back: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStep4Back)

    var selectedDecks by remember(initialSelectedDecks, selectedGoal) {
        mutableStateOf(
            if (initialSelectedDecks.isNotEmpty()) initialSelectedDecks
            else OnboardingStep4Defaults.initialDecksForGoal(selectedGoal)
        )
    }

    var startAtUserLevel by remember(initialStartAtUserLevel) {
        mutableStateOf(initialStartAtUserLevel)
    }

    val totalSelectedEntries = remember(selectedDecks) {
        OnboardingStep4Defaults.ALL_DECKS
            .filter { it.id in selectedDecks }
            .sumOf { it.entryCount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_step_4_decks_placement"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button to Step 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onStep4Back,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("btn_onboarding_step4_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to goal and time",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Goal & Time",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            OnboardingHeroHeader(
                titleEn = "Choose your study decks",
                titleAr = "اختر قوائم المفردات ونقطة البداية",
                titleEnTestTag = "onboarding_step4_header_en",
                titleArTestTag = "onboarding_step4_header_ar"
            )

            // Section 1: Starting Point (Placement / Pacing)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "1",
                    titleEn = "Starting Point",
                    titleAr = "نقطة انطلاق الخطة"
                )

                // Option A: Start at my level (recommended)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleCardShape)
                        .clickable(role = Role.RadioButton, onClick = { startAtUserLevel = true })
                        .testTag("onboarding_placement_option_level"),
                    shape = SquircleCardShape,
                    color = if (startAtUserLevel) GoldAccentDim else SurfaceDark,
                    border = BorderStroke(
                        width = if (startAtUserLevel) 1.5.dp else 1.dp,
                        color = if (startAtUserLevel) GoldAccent else BorderDark
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (startAtUserLevel) GoldAccent else SurfaceElevatedDark)
                                .border(
                                    width = 1.dp,
                                    color = if (startAtUserLevel) GoldAccent else BorderDark,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Start at level",
                                tint = if (startAtUserLevel) OnGoldDark else GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Start at my level ($selectedLevel)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (startAtUserLevel) GoldAccent else TextPrimary
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "التركيز على مفردات المستوى \u2066$selectedLevel\u2069 وتخطي الأساسيات السهلة.",
                                    style = ArabicTypography.labelSmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFFD1D5DB),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                            Text(
                                text = "Prioritizes $selectedLevel words, skipping basics you already know.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = TextSecondary
                            )
                        }

                        OnboardingRadioCircle(isSelected = startAtUserLevel)
                    }
                }

                // Option B: Start from beginning
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleCardShape)
                        .clickable(role = Role.RadioButton, onClick = { startAtUserLevel = false })
                        .testTag("onboarding_placement_option_start"),
                    shape = SquircleCardShape,
                    color = if (!startAtUserLevel) GoldAccentDim else SurfaceDark,
                    border = BorderStroke(
                        width = if (!startAtUserLevel) 1.5.dp else 1.dp,
                        color = if (!startAtUserLevel) GoldAccent else BorderDark
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!startAtUserLevel) GoldAccent else SurfaceElevatedDark)
                                .border(
                                    width = 1.dp,
                                    color = if (!startAtUserLevel) GoldAccent else BorderDark,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Start from beginning",
                                tint = if (!startAtUserLevel) OnGoldDark else GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Start from the beginning (A1)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (!startAtUserLevel) GoldAccent else TextPrimary
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "مراجعة شاملة متدرجة من الكلمات التأسيسية الأولى.",
                                    style = ArabicTypography.labelSmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFFD1D5DB),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                            Text(
                                text = "Comprehensive review from foundational vocabulary.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = TextSecondary
                            )
                        }

                        OnboardingRadioCircle(isSelected = !startAtUserLevel)
                    }
                }
            }

            // Section 2: Core Decks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "2",
                    titleEn = "Core Vocabulary Decks",
                    titleAr = "القوائم الأساسية"
                )

                OnboardingStep4Defaults.ALL_DECKS.filter { it.category == "core" }.forEach { deck ->
                    val isChecked = deck.id in selectedDecks
                    DeckItemCard(
                        deck = deck,
                        isChecked = isChecked,
                        onToggle = {
                            selectedDecks = if (isChecked) {
                                if (selectedDecks.size > 1) selectedDecks - deck.id else selectedDecks
                            } else {
                                selectedDecks + deck.id
                            }
                        }
                    )
                }
            }

            // Section 3: Specialized & Exam Decks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "3",
                    titleEn = "Specialized & Exam Decks",
                    titleAr = "القوائم التخصصية"
                )

                OnboardingStep4Defaults.ALL_DECKS.filter { it.category == "career" }.forEach { deck ->
                    val isChecked = deck.id in selectedDecks
                    DeckItemCard(
                        deck = deck,
                        isChecked = isChecked,
                        onToggle = {
                            selectedDecks = if (isChecked) {
                                if (selectedDecks.size > 1) selectedDecks - deck.id else selectedDecks
                            } else {
                                selectedDecks + deck.id
                            }
                        }
                    )
                }
            }

            // Selection Summary banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_step4_summary"),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevatedDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = "Selected decks count",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${selectedDecks.size} decks selected (${totalSelectedEntries} words)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (startAtUserLevel) "Level $selectedLevel" else "From A1",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Sticky Primary CTA Footer
        OnboardingStickyFooter(
            ctaTextEn = "Continue (${selectedDecks.size} decks)",
            ctaTextAr = "المتابعة بالقوائم المحددة",
            buttonTestTag = "btn_onboarding_step4_continue",
            onClick = {
                onContinueStep4(selectedDecks, startAtUserLevel)
            }
        )
    }
}

@Composable
private fun DeckItemCard(
    deck: OnboardingDeckInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(SquircleCardShape)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .testTag("onboarding_deck_card_${deck.id}"),
        shape = SquircleCardShape,
        color = if (isChecked) GoldAccentDim else SurfaceDark,
        border = BorderStroke(
            width = if (isChecked) 1.5.dp else 1.dp,
            color = if (isChecked) GoldAccent else BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnboardingCheckboxSquare(isChecked = isChecked)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isChecked) GoldAccent else TextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Compact Level + Count Metadata Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = deck.levelBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GoldAccent
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                                Text(
                                    text = "${deck.entryCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = deck.arabicTitle,
                        style = ArabicTypography.labelSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFD1D5DB),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }

                Text(
                    text = deck.descriptionEn,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}
