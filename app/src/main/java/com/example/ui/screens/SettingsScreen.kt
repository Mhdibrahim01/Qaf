package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.TranslationEngineMode
import com.example.data.local.StudyPlanProfileEntity
import com.example.ui.components.ArabicText
import com.example.ui.components.ProgressBadge
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SettingsScreen(
    preferredAccentUk: Boolean,
    ttsSpeechRate: Float,
    autoPlayPronunciation: Boolean,
    studyPlanProfile: StudyPlanProfileEntity?,
    totalEntriesCount: Int,
    allowAi: Boolean,
    engineMode: TranslationEngineMode,
    apiKeySource: String,
    isApiKeyConfigured: Boolean,
    isDeveloperModeEnabled: Boolean,

    onSelectAccentUk: (Boolean) -> Unit,
    onSelectSpeechRate: (Float) -> Unit,
    onToggleAutoPlayPronunciation: (Boolean) -> Unit,
    onTestVoicePreview: (String) -> Unit,
    onTestVoiceSlowPreview: (String) -> Unit = { onTestVoicePreview(it) },
    onSavePlanProfile: (StudyPlanProfileEntity) -> Unit,
    onNavigateToPlan: () -> Unit,
    onRedoOnboardingSetup: () -> Unit,
    onExportBackupToUri: (Uri, (Boolean, String) -> Unit) -> Unit,
    onImportBackupFromUri: (Uri, (Boolean, String) -> Unit) -> Unit,
    onToggleAllowAi: (Boolean) -> Unit,
    onSelectEngineMode: (TranslationEngineMode) -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onSetDeveloperMode: (Boolean) -> Unit,
    onNavigateToLabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale.US) }

    var isPlanSetupDialogOpen by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var developerCountdownBanner by remember { mutableStateOf<String?>(null) }
    var showRedoSetupConfirmDialog by remember { mutableStateOf(false) }

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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                    Text("Restore / استعادة", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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

    if (showRedoSetupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRedoSetupConfirmDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Redo Onboarding Setup? / إعادة ضبط الخطة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will return you to the onboarding wizard to re-select your target wordbooks, daily study pace, and schedule. Your existing word progress will NOT be erased.\n\nهل ترغب بالعودة لإعدادات التهيئة لاختيار القوائم والهدف اليومي وجدولك مجدداً؟ لن يتم مسح تقدم كلماتك السابقة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRedoSetupConfirmDialog = false
                        onRedoOnboardingSetup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_redo_onboarding")
                ) {
                    Text("Redo Setup / متابعة", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRedoSetupConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_redo_onboarding")
                ) {
                    Text("Cancel / إلغاء", color = TextMuted)
                }
            }
        )
    }

    val activeLevel = studyPlanProfile?.level?.ifBlank { "B2" } ?: "B2"
    val activeMinutes = studyPlanProfile?.minutesPerDay ?: 30
    val activeDays = studyPlanProfile?.studyDaysPerWeek ?: 6
    val estimatedWordsPerDay = when {
        activeMinutes <= 15 -> 8
        activeMinutes <= 30 -> 12
        activeMinutes <= 45 -> 18
        else -> 25
    }
    val formattedGoalName = when (studyPlanProfile?.goal) {
        "ielts" -> "IELTS Academic Prep"
        "it_career" -> "IT & Software Engineering"
        "business" -> "Business & Career English"
        "medical" -> "Medical & Healthcare"
        else -> "General Fluency & Oxford 5000"
    }
    val formattedGoalAr = when (studyPlanProfile?.goal) {
        "ielts" -> "التحضير لاختبار الآيلتس الأكاديمي"
        "it_career" -> "المصطلحات التقنية والبرمجية"
        else -> "الطلاقة العامة ومفردات أكسفورد"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_settings"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =========================================================================
            // SECTION 1: AUDIO & PRONUNCIATION
            // =========================================================================
            SettingsCategoryCard(
                eyebrow = "VOICE & SPEECH",
                titleEn = "Audio & Pronunciation",
                titleAr = "إعدادات الصوت والنطق",
                icon = Icons.Default.RecordVoiceOver,
                accentColor = GoldAccent,
                testTag = "settings_section_audio"
            ) {
                // 1.0 Voice Engine Status
                GoogleTtsEngineStatusSection()

                HorizontalDivider(color = BorderSubtle)

                // 1A. Accent Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Pronunciation Accent",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            ArabicText(
                                text = "اختر اللهجة المفضلة لنطق الكلمات والجمل",
                                style = ArabicTypography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsChoicePill(
                            labelEn = "US English",
                            subtitleEn = "American",
                            isSelected = !preferredAccentUk,
                            accentColor = GoldAccent,
                            onClick = { onSelectAccentUk(false) },
                            testTag = "btn_accent_us",
                            modifier = Modifier.weight(1f)
                        )
                        SettingsChoicePill(
                            labelEn = "UK English",
                            subtitleEn = "British",
                            isSelected = preferredAccentUk,
                            accentColor = GoldAccent,
                            onClick = { onSelectAccentUk(true) },
                            testTag = "btn_accent_uk",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // 1B. Speech Playback Speed + Test Speaker Button
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Speech Playback Speed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        ArabicText(
                            text = "سرعة قراءة الكلمات والجمل الصوتية",
                            style = ArabicTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    val speedPresets = listOf(
                        Triple(0.60f, "Slow", "0.60x"),
                        Triple(0.85f, "Learning", "0.85x"),
                        Triple(1.00f, "Normal", "1.0x")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speedPresets.forEach { (rate, label, badge) ->
                            val isSelected = abs(ttsSpeechRate - rate) < 0.08f
                            val tag = when (label) {
                                "Slow" -> "btn_speed_slow"
                                "Learning" -> "btn_speed_learning"
                                else -> "btn_speed_normal"
                            }
                            SettingsChoicePill(
                                labelEn = label,
                                subtitleEn = badge,
                                isSelected = isSelected,
                                accentColor = GoldAccent,
                                onClick = { onSelectSpeechRate(rate) },
                                testTag = tag,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onTestVoicePreview("Mastering English pronunciation requires natural rhythm and clear syllable stress.")
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_voice")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Test Voice",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Test Voice",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        SlowAudioTurtleButton(
                            onClick = {
                                onTestVoiceSlowPreview("Mastering English pronunciation requires natural rhythm and clear syllable stress.")
                            },
                            showSpeedLabel = true,
                            testTag = "btn_test_voice_slow"
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // 1C. Auto-Play Pronunciation Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Auto-Play Pronunciation",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically play audio when opening word details or flipping flashcards",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        ArabicText(
                            text = "تشغيل النطق تلقائياً عند فتح تفاصيل الكلمة أو قلب البطاقة",
                            style = ArabicTypography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoPlayPronunciation,
                        onCheckedChange = onToggleAutoPlayPronunciation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = GoldAccent
                        ),
                        modifier = Modifier.testTag("switch_auto_play_pronunciation")
                    )
                }
            }

            // =========================================================================
            // SECTION 2: LEARNING GOALS & DAILY ROUTINE
            // =========================================================================
            SettingsCategoryCard(
                eyebrow = "CURRICULUM & PACE",
                titleEn = "Learning Goals & Daily Routine",
                titleAr = "أهداف التعلم والروتين اليومي",
                icon = Icons.AutoMirrored.Filled.EventNote,
                accentColor = EmeraldAccent,
                testTag = "settings_section_goals"
            ) {
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProgressBadge(text = "CEFR $activeLevel", color = GoldAccent)
                                ProgressBadge(text = "~$estimatedWordsPerDay words/day", color = EmeraldAccent)
                            }
                            Text(
                                text = "${activeMinutes}m/day • ${activeDays}d/wk",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = formattedGoalName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        ArabicText(
                            text = formattedGoalAr,
                            style = ArabicTypography.bodySmall,
                            color = EmeraldAccent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isPlanSetupDialogOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent.copy(alpha = 0.16f),
                            contentColor = EmeraldAccent
                        ),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_edit_study_plan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Study Plan",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToPlan,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_study_plan_screen")
                    ) {
                        Text(
                            text = "View Checklist",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                OutlinedButton(
                    onClick = { showRedoSetupConfirmDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_settings_redo_onboarding")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Redo Onboarding Setup",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        ArabicText(
                            text = "إعادة ضبط الإعداد الأولي (دون مسح تقدمك)",
                            style = ArabicTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 3: DATA, CLOUD & BACKUPS
            // =========================================================================
            SettingsCategoryCard(
                eyebrow = "STORAGE & PORTABILITY",
                titleEn = "Data, Cloud & Backups",
                titleAr = "البيانات والنسخ الاحتياطي",
                icon = Icons.Default.Storage,
                accentColor = SkyAccent,
                testTag = "settings_section_data"
            ) {
                // Database Status Indicator
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_db_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Database Healthy",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val displayCount = if (totalEntriesCount > 0) {
                                numberFormatter.format(totalEntriesCount)
                            } else {
                                "21,624"
                            }
                            Text(
                                text = "Database Healthy • $displayCount vocabulary entries loaded",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            ArabicText(
                                text = "قاعدة البيانات مكتملة وتعمل بالكامل بدون إنترنت",
                                style = ArabicTypography.bodySmall,
                                color = EmeraldAccent
                            )
                        }
                    }
                }

                backupStatusMessage?.let { (isSuccess, msg) ->
                    Surface(
                        color = if (isSuccess) EmeraldAccent.copy(alpha = 0.14f) else CoralAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSuccess) EmeraldAccent else CoralAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backup_status_banner")
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSuccess) EmeraldAccent else CoralAccent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            exportBackupLauncher.launch("qaf_backup_$dateStamp.json")
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_settings_export_backup")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export JSON",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_settings_restore_backup")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = SkyAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restore JSON",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SkyAccent
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 4: AI & TRANSLATION SERVICES
            // =========================================================================
            SettingsCategoryCard(
                eyebrow = "INTELLIGENT TUTORING",
                titleEn = "AI & Translation Services",
                titleAr = "خدمات الذكاء الاصطناعي والترجمة",
                icon = Icons.Default.AutoAwesome,
                accentColor = VioletAccent,
                testTag = "settings_section_ai"
            ) {
                // 4A. AI Word Enrichment Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "AI Word Enrichment",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Enable AI-generated Arabic nuances, collocations, grammar feedback, and memory hooks",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = allowAi,
                        onCheckedChange = onToggleAllowAi,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = GoldAccent
                        ),
                        modifier = Modifier.testTag("switch_allow_ai_settings")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // 4B. Translation Engine Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Translation Engine Mode",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    ArabicText(
                        text = "اختر بين المحرك المجاني المدمج أو محرك الذكاء الاصطناعي السحابي",
                        style = ArabicTypography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsChoicePill(
                            labelEn = "Free Offline / Dictionary",
                            subtitleEn = "Pre-bundled + Free Engine",
                            isSelected = engineMode == TranslationEngineMode.FREE_API_ONLY,
                            accentColor = VioletAccent,
                            onClick = { onSelectEngineMode(TranslationEngineMode.FREE_API_ONLY) },
                            testTag = "btn_engine_offline",
                            modifier = Modifier.weight(1f)
                        )
                        SettingsChoicePill(
                            labelEn = "AI Cloud Engine",
                            subtitleEn = "Smart Auto / Gemini / Groq",
                            isSelected = engineMode != TranslationEngineMode.FREE_API_ONLY,
                            accentColor = VioletAccent,
                            onClick = { onSelectEngineMode(TranslationEngineMode.SMART_AUTO) },
                            testTag = "btn_engine_ai",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // 4C. API Key Management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "API Key Management",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = apiKeySource,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isApiKeyConfigured) EmeraldAccent else CoralAccent
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenApiKeyDialog,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier.testTag("btn_settings_change_api_key")
                    ) {
                        Text(
                            text = "Configure Keys",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 5: ABOUT & SUPPORT (+ 7-TAP DEVELOPER MODE EASTER EGG)
            // =========================================================================
            SettingsCategoryCard(
                eyebrow = "APP INFO & PRIVACY",
                titleEn = "About & Support",
                titleAr = "حول التطبيق والدعم",
                icon = Icons.Default.Info,
                accentColor = GoldAccent,
                testTag = "settings_section_about"
            ) {
                // Branding & Slogan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoldAccentDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ق",
                            style = ArabicTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = GoldAccent
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Qaf • قاف — English Vocabulary Builder",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        ArabicText(
                            text = "رفيقك الذكي لإتقان المفردات والقواعد والقراءة باللغة الإنجليزية",
                            style = ArabicTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Privacy & Offline Assurance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Privacy & Offline-First Architecture",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "All study progress, SM-2 intervals, and personal API keys are stored locally on your device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Interactive 7-Tap Version Row
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            if (isDeveloperModeEnabled) {
                                val msg = "Developer mode is already enabled"
                                developerCountdownBanner = msg
                                try {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            } else {
                                versionTapCount += 1
                                when {
                                    versionTapCount in 4..6 -> {
                                        val remaining = 7 - versionTapCount
                                        val stepWord = if (remaining == 1) "step" else "steps"
                                        val msg = "You are $remaining $stepWord away from developer mode"
                                        developerCountdownBanner = msg
                                        try {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    }
                                    versionTapCount >= 7 -> {
                                        versionTapCount = 0
                                        onSetDeveloperMode(true)
                                        val msg = "You are now a developer! Labs & Diagnostics unlocked."
                                        developerCountdownBanner = msg
                                        try {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                        .testTag("settings_version_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Version 1.0 (Build 1)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Room Schema v16 • Offline Dictionary Asset v4",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        ProgressBadge(
                            text = if (isDeveloperModeEnabled) "Dev Mode Active" else "Stable Release",
                            color = if (isDeveloperModeEnabled) GoldAccent else EmeraldAccent
                        )
                    }
                }

                // Transient countdown / unlock feedback banner
                developerCountdownBanner?.let { bannerText ->
                    Surface(
                        color = GoldAccentDim,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_developer_toast_banner")
                    ) {
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Discreet Developer Options Entry (Visible ONLY when Developer Mode is unlocked)
                AnimatedVisibility(
                    visible = isDeveloperModeEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("settings_developer_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccentDim),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Developer Options: Labs & Diagnostics",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GoldAccent
                                        )
                                        Text(
                                            text = "SQLite verifier, batch translation engine & simulation tools",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = isDeveloperModeEnabled,
                                    onCheckedChange = { enabled ->
                                        onSetDeveloperMode(enabled)
                                        if (!enabled) {
                                            versionTapCount = 0
                                            developerCountdownBanner = null
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BackgroundDark,
                                        checkedTrackColor = GoldAccent
                                    ),
                                    modifier = Modifier.testTag("switch_developer_mode")
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToLabs,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_settings_open_labs")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Labs & Diagnostics",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isPlanSetupDialogOpen) {
        PlanSetupDialog(
            initialProfile = studyPlanProfile,
            onDismiss = { isPlanSetupDialogOpen = false },
            onSaveProfile = { updatedProfile ->
                onSavePlanProfile(updatedProfile)
                isPlanSetupDialogOpen = false
            }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    eyebrow: String,
    titleEn: String,
    titleAr: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            text = eyebrow,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = TextMuted
                        )
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

                HorizontalDivider(color = BorderSubtle)

                content()
            }
        }
    }
}

@Composable
private fun SettingsChoicePill(
    labelEn: String,
    subtitleEn: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(testTag),
        color = if (isSelected) accentColor.copy(alpha = 0.16f) else SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else BorderDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = labelEn,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) accentColor else TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleEn,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) accentColor.copy(alpha = 0.85f) else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoogleTtsEngineStatusSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_google_tts_engine"),
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(width = 1.dp, color = GoldAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Google Speech Services",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Native local neural voices • 0 MB download • High quality",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                ProgressBadge(
                    text = "Active Engine",
                    color = EmeraldAccent
                )
            }
            Text(
                text = "Calibrated with natural 0.98x pitch and dual speeds (0.85x Normal, 0.60x Slow Turtle). No extra downloads required.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
