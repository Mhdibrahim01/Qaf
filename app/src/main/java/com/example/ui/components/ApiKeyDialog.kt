package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiKeyProvider
import com.example.data.ai.ApiKeyTestResult
import com.example.data.ai.FreeDictionaryAndTranslationEngine
import com.example.data.ai.GeminiApiKeyManager
import com.example.data.ai.TranslationEngineMode
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ApiKeyDialog(
    initialKey: String,
    keySource: String,
    onSaveKey: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedEngineMode by remember {
        mutableStateOf(GeminiApiKeyManager.getEngineMode(context))
    }
    var apiKeyText by remember { mutableStateOf(initialKey) }
    var isPasswordVisible by remember { mutableStateOf(true) }
    var isTestingKey by remember { mutableStateOf(false) }
    var isTestingFreeApi by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ApiKeyTestResult?>(null) }
    var copyUrlFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val parsedCandidateKeys = remember(apiKeyText) {
        GeminiApiKeyManager.parseKeyList(apiKeyText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = GoldAccentDim,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "AI & Translation Settings",
                        tint = GoldAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI & Translation Engine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Gemini • Groq • Free Dictionary & Translation API",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current source status chip
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Engine:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text = keySource,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (keySource.contains("Not", ignoreCase = true)) CoralAccent else EmeraldAccent
                        )
                    }
                }

                // 1. Engine Mode Selector Pills (Smart Auto | Free API | AI Keys Only)
                Text(
                    text = "Translation & Dictionary Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Surface(
                    color = BackgroundDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TranslationEngineMode.entries.forEach { mode ->
                            val isSelected = selectedEngineMode == mode
                            Surface(
                                color = if (isSelected) GoldAccentDim else BackgroundDark.copy(alpha = 0f),
                                border = if (isSelected) BorderStroke(1.dp, GoldAccentBorder) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedEngineMode = mode
                                        GeminiApiKeyManager.setEngineMode(context, mode)
                                        testResult = null
                                    }
                                    .testTag("engine_mode_${mode.id}")
                            ) {
                                Text(
                                    text = mode.shortBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) GoldAccent else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = selectedEngineMode.descriptionEn,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = TextSecondary
                )

                // 2. Multi-Provider Custom Keys Input
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        testResult = null
                    },
                    label = { Text("API Keys (AIzaSy... / gsk_... / sk-...)") },
                    placeholder = {
                        Text(
                            "Paste Gemini (AIzaSy...), Groq (gsk_...), or OpenRouter (sk-or-...) keys (comma-separated)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    apiKeyText = clip.trim()
                                    testResult = null
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide key" else "Show key",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_gemini_api_key")
                )

                // Live key format detection badges
                if (parsedCandidateKeys.isNotEmpty()) {
                    val invalidCount = parsedCandidateKeys.count {
                        GeminiApiKeyManager.detectProvider(it) == AiKeyProvider.OAUTH_OR_INVALID
                    }
                    val validProviders = parsedCandidateKeys
                        .map { GeminiApiKeyManager.detectProvider(it) }
                        .filter { it != AiKeyProvider.OAUTH_OR_INVALID }
                    if (invalidCount > 0) {
                        Surface(
                            color = CoralAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Warning: Token starting with '${parsedCandidateKeys.first().take(4)}...' is not a valid API key. Gemini keys start with 'AIzaSy...' or 'AQ...', and Groq keys start with 'gsk_...'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralAccent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (validProviders.isNotEmpty()) {
                        Text(
                            text = "Detected ${validProviders.size} valid key(s): ${validProviders.joinToString(", ") { it.displayName }} (+ system fallback key)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent
                        )
                    }
                }

                // 3. Action Buttons Row: Test Key(s), Test Free API, Get Free Key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Key(s) button
                    OutlinedButton(
                        onClick = {
                            if (apiKeyText.isBlank()) {
                                testResult = ApiKeyTestResult.Error("Please enter a key to test, or tap 'Test Free API'.")
                                return@OutlinedButton
                            }
                            isTestingKey = true
                            testResult = null
                            coroutineScope.launch {
                                val res = GeminiApiKeyManager.testApiKey(apiKeyText)
                                testResult = res
                                isTestingKey = false
                            }
                        },
                        enabled = !isTestingKey && !isTestingFreeApi && apiKeyText.isNotBlank(),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_gemini_api_key")
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(
                                color = GoldAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Test Key", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                        }
                    }

                    // Test Free API button
                    OutlinedButton(
                        onClick = {
                            isTestingFreeApi = true
                            testResult = null
                            coroutineScope.launch {
                                val freeRes = FreeDictionaryAndTranslationEngine.testFreeApiConnection()
                                testResult = if (freeRes.isSuccess) {
                                    ApiKeyTestResult.Success(freeRes.getOrThrow())
                                } else {
                                    ApiKeyTestResult.Error(freeRes.exceptionOrNull()?.message ?: "Free API test failed")
                                }
                                isTestingFreeApi = false
                            }
                        },
                        enabled = !isTestingKey && !isTestingFreeApi,
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_free_api")
                    ) {
                        if (isTestingFreeApi) {
                            CircularProgressIndicator(
                                color = EmeraldAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent)
                        } else {
                            Icon(Icons.Default.Public, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Free API", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldAccent)
                        }
                    }
                }

                // Get Free Key URL helper
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("https://aistudio.google.com/apikey"))
                        copyUrlFeedback = true
                    },
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copyUrlFeedback) "Copied https://aistudio.google.com/apikey!" else "Copy Google AI Studio Key URL (AIzaSy...)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copyUrlFeedback) EmeraldAccent else TextSecondary
                    )
                }

                // Test result banner
                testResult?.let { result ->
                    val (bgColor, textColor, icon) = when (result) {
                        is ApiKeyTestResult.Success -> Triple(EmeraldAccent.copy(alpha = 0.15f), EmeraldAccent, Icons.Default.CheckCircle)
                        is ApiKeyTestResult.RateLimited -> Triple(GoldAccent.copy(alpha = 0.15f), GoldAccent, Icons.Default.AutoAwesome)
                        is ApiKeyTestResult.Error -> Triple(CoralAccent.copy(alpha = 0.15f), CoralAccent, Icons.Default.Warning)
                    }
                    val msg = when (result) {
                        is ApiKeyTestResult.Success -> result.message
                        is ApiKeyTestResult.RateLimited -> result.message
                        is ApiKeyTestResult.Error -> result.message
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = textColor)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    GeminiApiKeyManager.setEngineMode(context, selectedEngineMode)
                    onSaveKey(apiKeyText.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = BackgroundDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_gemini_api_key")
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (apiKeyText.isNotBlank() || keySource.contains("Custom", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = {
                            onResetToDefault()
                            GeminiApiKeyManager.setEngineMode(context, TranslationEngineMode.SMART_AUTO)
                            selectedEngineMode = TranslationEngineMode.SMART_AUTO
                            apiKeyText = ""
                            testResult = null
                        },
                        border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_reset_gemini_api_key")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}
