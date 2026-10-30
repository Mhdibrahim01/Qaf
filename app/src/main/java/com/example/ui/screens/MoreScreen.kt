package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArabicText
import com.example.ui.components.LearningStatCard
import com.example.ui.components.ProgressBadge
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    onNavigateToGrammar: () -> Unit,
    onNavigateToReading: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToCorrectionNotebook: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToLabs: () -> Unit = {},
    onNavigateToSentences: () -> Unit = {},
    onExportBackupToUri: (Uri, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onImportBackupFromUri: (Uri, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    grammarLessonsCount: Int = 90,
    readingTextsCount: Int = 50,
    correctionEntriesCount: Int = 0,
    savedSentencesCount: Int = 0,
    studyStreak: Int = 0,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()
    var backupStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportBackupToUri(uri) { success, msg ->
                backupStatusMessage = success to msg
            }
        }
    }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    if (pendingRestoreUri != null) {
        val targetUri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = {
                Text(
                    text = "Restore Backup / استعادة النسخة الاحتياطية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Restoring this backup will merge and update your progress, learning history, and study plan. Are you sure you want to proceed?\n\nاستعادة هذه النسخة ستؤدي إلى دمج وتحديث تقدمك وسجل دراستك. هل ترغب بالاستمرار؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        onImportBackupFromUri(targetUri) { success, msg ->
                            backupStatusMessage = success to msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    )
                ) {
                    Text("Restore / استعادة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancel / إلغاء", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_more"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Quick learning stats overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LearningStatCard(
                    value = "$grammarLessonsCount",
                    labelEn = "Grammar",
                    labelAr = "القواعد",
                    accentColor = VioletAccent,
                    onClick = onNavigateToGrammar,
                    modifier = Modifier.weight(1f)
                )
                LearningStatCard(
                    value = "$readingTextsCount",
                    labelEn = "Reading",
                    labelAr = "القراءة",
                    accentColor = SkyAccent,
                    onClick = onNavigateToReading,
                    modifier = Modifier.weight(1f)
                )
                LearningStatCard(
                    value = "$correctionEntriesCount",
                    labelEn = "Notebook",
                    labelAr = "دفتر التصحيح",
                    accentColor = CoralAccent,
                    onClick = onNavigateToCorrectionNotebook,
                    modifier = Modifier.weight(1f)
                )
                LearningStatCard(
                    value = "${studyStreak}d",
                    labelEn = "Streak",
                    labelAr = "الحماسة",
                    accentColor = GoldAccent,
                    onClick = onNavigateToPlan,
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 1: Core Learning Modules
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HubSectionHeader(
                    eyebrow = "CURRICULUM & PRACTICE",
                    title = "Study Modules"
                )

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MoreModuleCard(
                            titleEn = "Study Plan",
                            titleAr = "خطة التعلم اليومية والشهرية",
                            descriptionEn = "Adaptive daily checklist, monthly CEFR targets, skill accuracy analytics, and comprehensive quizzes.",
                            tagText = "Daily Checklist",
                            accentColor = EmeraldAccent,
                            icon = Icons.AutoMirrored.Filled.EventNote,
                            testTag = "nav_plan",
                            onClick = onNavigateToPlan,
                            modifier = Modifier.weight(1f)
                        )
                        MoreModuleCard(
                            titleEn = "Saved Sentences",
                            titleAr = "الجمل المحفوظة • الإملاء والتحدث",
                            descriptionEn = "Practice saved example sentences with audio playback, dictation mode, speaking practice, and AI explanations.",
                            tagText = "$savedSentencesCount Saved",
                            accentColor = GoldAccent,
                            icon = Icons.Default.FormatQuote,
                            testTag = "nav_sentences",
                            onClick = onNavigateToSentences,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MoreModuleCard(
                            titleEn = "Correction Notebook",
                            titleAr = "دفتر تصحيح الجمل والمراجعة المتباعدة",
                            descriptionEn = "Track grammar mistakes, practice writing across 3-7-21 day intervals, and receive instant AI feedback.",
                            tagText = "Spaced Review",
                            accentColor = CoralAccent,
                            icon = Icons.Default.AutoFixHigh,
                            testTag = "nav_correction_notebook",
                            onClick = onNavigateToCorrectionNotebook,
                            modifier = Modifier.weight(1f)
                        )
                        MoreModuleCard(
                            titleEn = "Grammar Lessons",
                            titleAr = "القواعد النحوية • A1–C1",
                            descriptionEn = "CEFR-graded lessons explained in clear Arabic with bilingual examples, interactive quizzes, and AI tutoring.",
                            tagText = "$grammarLessonsCount Lessons",
                            accentColor = VioletAccent,
                            icon = Icons.Default.Spellcheck,
                            testTag = "nav_grammar_hub",
                            onClick = onNavigateToGrammar,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MoreModuleCard(
                            titleEn = "Graded Reading",
                            titleAr = "القراءة المتدرجة • نصوص تفاعلية",
                            descriptionEn = "Interactive bilingual stories where tapping any word reveals its Arabic meaning, IPA phonetics, and audio.",
                            tagText = "$readingTextsCount Passages",
                            accentColor = SkyAccent,
                            icon = Icons.Default.Description,
                            testTag = "nav_reading_hub",
                            onClick = onNavigateToReading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoreModuleCard(
                            titleEn = "Study Plan",
                            titleAr = "خطة التعلم اليومية والشهرية",
                            descriptionEn = "Adaptive daily checklist, monthly CEFR targets, skill accuracy analytics, and comprehensive quizzes.",
                            tagText = "Daily Checklist",
                            accentColor = EmeraldAccent,
                            icon = Icons.AutoMirrored.Filled.EventNote,
                            testTag = "nav_plan",
                            onClick = onNavigateToPlan
                        )
                        MoreModuleCard(
                            titleEn = "Saved Sentences",
                            titleAr = "الجمل المحفوظة • الإملاء والتحدث",
                            descriptionEn = "Practice saved example sentences with audio playback, dictation mode, speaking practice, and AI explanations.",
                            tagText = "$savedSentencesCount Saved",
                            accentColor = GoldAccent,
                            icon = Icons.Default.FormatQuote,
                            testTag = "nav_sentences",
                            onClick = onNavigateToSentences
                        )
                        MoreModuleCard(
                            titleEn = "Correction Notebook",
                            titleAr = "دفتر تصحيح الجمل والمراجعة المتباعدة",
                            descriptionEn = "Track grammar mistakes, practice writing across 3-7-21 day intervals, and receive instant AI feedback.",
                            tagText = "Spaced Review",
                            accentColor = CoralAccent,
                            icon = Icons.Default.AutoFixHigh,
                            testTag = "nav_correction_notebook",
                            onClick = onNavigateToCorrectionNotebook
                        )
                        MoreModuleCard(
                            titleEn = "Grammar Lessons",
                            titleAr = "القواعد النحوية • A1–C1",
                            descriptionEn = "CEFR-graded lessons explained in clear Arabic with bilingual examples, interactive quizzes, and AI tutoring.",
                            tagText = "$grammarLessonsCount Lessons",
                            accentColor = VioletAccent,
                            icon = Icons.Default.Spellcheck,
                            testTag = "nav_grammar_hub",
                            onClick = onNavigateToGrammar
                        )
                        MoreModuleCard(
                            titleEn = "Graded Reading",
                            titleAr = "القراءة المتدرجة • نصوص تفاعلية",
                            descriptionEn = "Interactive bilingual stories where tapping any word reveals its Arabic meaning, IPA phonetics, and audio.",
                            tagText = "$readingTextsCount Passages",
                            accentColor = SkyAccent,
                            icon = Icons.Default.Description,
                            testTag = "nav_reading_hub",
                            onClick = onNavigateToReading
                        )
                    }
                }
            }

            // Section 2: Preferences & Utilities
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HubSectionHeader(
                    eyebrow = "SYSTEM & UTILITIES",
                    title = "Preferences & Data Backup"
                )

                backupStatusMessage?.let { (isSuccess, msg) ->
                    Surface(
                        color = if (isSuccess) EmeraldAccent.copy(alpha = 0.14f) else CoralAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSuccess) EmeraldAccent else CoralAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_backup_status_banner")
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSuccess) EmeraldAccent else CoralAccent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                val triggerExport: () -> Unit = {
                    val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    exportBackupLauncher.launch("qaf_backup_$dateStamp.json")
                }
                val triggerImport: () -> Unit = {
                    importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MoreModuleCard(
                            titleEn = "Settings",
                            titleAr = "إعدادات التطبيق والصوت والذكاء الاصطناعي",
                            descriptionEn = "Configure pronunciation accent & speed, study goals, backups, Gemini API keys, and app preferences.",
                            tagText = "Preferences",
                            accentColor = GoldAccent,
                            icon = Icons.Default.Settings,
                            testTag = "nav_settings",
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        )
                        MoreModuleCard(
                            titleEn = "Export Progress Backup",
                            titleAr = "تصدير نسخة احتياطية كاملة لتقدمك (JSON)",
                            descriptionEn = "Save all word progress, SM-2 intervals, streaks, correction notebook, and study plans to a local JSON file.",
                            tagText = "Offline JSON",
                            accentColor = EmeraldAccent,
                            icon = Icons.Default.CloudDownload,
                            testTag = "btn_export_backup",
                            onClick = triggerExport,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MoreModuleCard(
                            titleEn = "Restore Progress Backup",
                            titleAr = "استعادة نسخة احتياطية من ملف JSON",
                            descriptionEn = "Restore your vocabulary stages, streaks, study plans, and review history from a previously exported JSON file.",
                            tagText = "Restore",
                            accentColor = SkyAccent,
                            icon = Icons.Default.CloudUpload,
                            testTag = "btn_restore_backup",
                            onClick = triggerImport,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoreModuleCard(
                            titleEn = "Settings",
                            titleAr = "إعدادات التطبيق والصوت والذكاء الاصطناعي",
                            descriptionEn = "Configure pronunciation accent & speed, study goals, backups, Gemini API keys, and app preferences.",
                            tagText = "Preferences",
                            accentColor = GoldAccent,
                            icon = Icons.Default.Settings,
                            testTag = "nav_settings",
                            onClick = onOpenSettings
                        )
                        MoreModuleCard(
                            titleEn = "Export Progress Backup",
                            titleAr = "تصدير نسخة احتياطية كاملة لتقدمك (JSON)",
                            descriptionEn = "Save all word progress, SM-2 intervals, streaks, correction notebook, and study plans to a local JSON file.",
                            tagText = "Offline JSON",
                            accentColor = EmeraldAccent,
                            icon = Icons.Default.CloudDownload,
                            testTag = "btn_export_backup",
                            onClick = triggerExport
                        )
                        MoreModuleCard(
                            titleEn = "Restore Progress Backup",
                            titleAr = "استعادة نسخة احتياطية من ملف JSON",
                            descriptionEn = "Restore your vocabulary stages, streaks, study plans, and review history from a previously exported JSON file.",
                            tagText = "Restore",
                            accentColor = SkyAccent,
                            icon = Icons.Default.CloudUpload,
                            testTag = "btn_restore_backup",
                            onClick = triggerImport
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HubSectionHeader(
    eyebrow: String,
    title: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = TextMuted
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            ),
            color = TextPrimary
        )
    }
}

@Composable
private fun MoreModuleCard(
    titleEn: String,
    titleAr: String,
    descriptionEn: String,
    tagText: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = titleEn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            ArabicText(
                                text = titleAr,
                                style = ArabicTypography.bodySmall,
                                color = accentColor.copy(alpha = 0.9f)
                            )
                        }
                    }

                    ProgressBadge(text = tagText, color = accentColor)
                }

                Text(
                    text = descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
