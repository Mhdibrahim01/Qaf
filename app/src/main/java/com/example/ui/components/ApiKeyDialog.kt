package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiKeyProvider
import com.example.data.ai.ApiKeyTestResult
import com.example.data.ai.ConfiguredApiKey
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

private fun getProviderColor(provider: AiKeyProvider): Color = when (provider) {
    AiKeyProvider.GEMINI -> Color(0xFF38BDF8)       // Sky Cyan
    AiKeyProvider.GROQ -> Color(0xFFFB923C)         // Warm Orange
    AiKeyProvider.OPENROUTER -> Color(0xFFA855F7)   // Purple
    AiKeyProvider.OPENAI -> Color(0xFF10B981)       // Emerald Green
    AiKeyProvider.OAUTH_OR_INVALID -> CoralAccent
}

private fun getProviderBgColor(provider: AiKeyProvider): Color = when (provider) {
    AiKeyProvider.GEMINI -> Color(0xFF0369A1).copy(alpha = 0.25f)
    AiKeyProvider.GROQ -> Color(0xFFC2410C).copy(alpha = 0.25f)
    AiKeyProvider.OPENROUTER -> Color(0xFF6B21A8).copy(alpha = 0.25f)
    AiKeyProvider.OPENAI -> Color(0xFF047857).copy(alpha = 0.25f)
    AiKeyProvider.OAUTH_OR_INVALID -> CoralAccent.copy(alpha = 0.15f)
}

@Composable
fun ApiKeyDialog(
    initialKey: String,
    keySource: String,
    onSaveKey: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Engine Mode State
    var selectedEngineMode by remember {
        mutableStateOf(GeminiApiKeyManager.getEngineMode(context))
    }

    // Configured Keys List State
    val configuredKeys = remember {
        mutableStateListOf<ConfiguredApiKey>().apply {
            addAll(GeminiApiKeyManager.getConfiguredKeyItems(context))
        }
    }

    // Fallback: If list is empty but initialKey is present
    LaunchedEffect(Unit) {
        if (configuredKeys.isEmpty() && initialKey.isNotBlank()) {
            val p = GeminiApiKeyManager.detectProvider(initialKey)
            if (p != AiKeyProvider.OAUTH_OR_INVALID) {
                configuredKeys.add(
                    ConfiguredApiKey(
                        key = initialKey.trim(),
                        providerName = p.name,
                        label = "${p.displayName} Key 1",
                        isActive = true
                    )
                )
            }
        }
    }

    // Model Selection State
    val initialModel = remember { GeminiApiKeyManager.getSelectedModel(context) }
    val isInitiallyCustom = remember(initialModel) {
        GeminiApiKeyManager.POPULAR_MODEL_PRESETS.none { it.first == initialModel } && initialModel != "auto"
    }

    var selectedModel by remember { mutableStateOf(initialModel) }
    var isCustomModelSelected by remember { mutableStateOf(isInitiallyCustom) }
    var customModelInput by remember { mutableStateOf(if (isInitiallyCustom) initialModel else "") }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    // Add New Key Form State
    var newKeyInput by remember { mutableStateOf("") }
    var newKeyLabelInput by remember { mutableStateOf("") }
    var isNewKeyPasswordVisible by remember { mutableStateOf(false) }

    // Individual Key Testing State
    val keyTestingMap = remember { mutableStateMapOf<String, Boolean>() }
    val keyTestResults = remember { mutableStateMapOf<String, ApiKeyTestResult>() }

    // Free API Testing State
    var isTestingFreeApi by remember { mutableStateOf(false) }
    var freeApiTestResult by remember { mutableStateOf<ApiKeyTestResult?>(null) }
    var copyUrlFeedback by remember { mutableStateOf<String?>(null) }

    val sanitizedNewKey = remember(newKeyInput) { GeminiApiKeyManager.sanitizeKey(newKeyInput) }
    val detectedNewKeyProvider = remember(sanitizedNewKey) { GeminiApiKeyManager.detectProvider(sanitizedNewKey) }

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
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "AI & Translation Settings",
                        tint = GoldAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI & Translation Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Multi-Key Switcher • Custom Model • Smart Fallback",
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
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---------------------------------------------------------------------------------
                // 1. Current Active Source & Engine Status Card
                // ---------------------------------------------------------------------------------
                val activeKeyItem = configuredKeys.firstOrNull { it.isActive }
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                        if (activeKeyItem != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Key:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${activeKeyItem.provider.displayName} (${activeKeyItem.maskedKey})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }

                // ---------------------------------------------------------------------------------
                // 2. Engine Mode Selector (Smart Auto | Free API | AI Keys Only)
                // ---------------------------------------------------------------------------------
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
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = TextSecondary
                )

                HorizontalDivider(color = BorderDark, thickness = 1.dp)

                // ---------------------------------------------------------------------------------
                // 3. AI Model Selector (Preset Dropdown + Custom Model Input)
                // ---------------------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AI Model Selection",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    val currentModelLabel = remember(selectedModel, isCustomModelSelected, customModelInput) {
                        if (isCustomModelSelected) {
                            if (customModelInput.isNotBlank()) "Custom: $customModelInput" else "✏️ Custom Model (Type below)"
                        } else {
                            GeminiApiKeyManager.POPULAR_MODEL_PRESETS.firstOrNull { it.first == selectedModel }?.second
                                ?: if (selectedModel.isNotBlank()) "Custom: $selectedModel" else "Auto (Smart Default)"
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = BackgroundDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isCustomModelSelected) GoldAccent else BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isModelDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isCustomModelSelected) GoldAccent else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = currentModelLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Model",
                                    tint = GoldAccent
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceElevatedDark)
                        ) {
                            GeminiApiKeyManager.POPULAR_MODEL_PRESETS.forEach { (modelId, label) ->
                                val isSelected = !isCustomModelSelected && selectedModel == modelId
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) GoldAccent else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedModel = modelId
                                        isCustomModelSelected = false
                                        isModelDropdownExpanded = false
                                    }
                                )
                            }

                            HorizontalDivider(color = BorderDark)

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✏️ Custom Model (Type your own)...",
                                            color = if (isCustomModelSelected) GoldAccent else TextPrimary,
                                            fontWeight = if (isCustomModelSelected) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (isCustomModelSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isCustomModelSelected = true
                                    isModelDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Custom model text input if selected
                    if (isCustomModelSelected) {
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = {
                                customModelInput = it
                                selectedModel = it.trim()
                            },
                            label = { Text("Custom Model ID", style = MaterialTheme.typography.bodySmall) },
                            placeholder = {
                                Text("e.g. llama-3.3-70b-versatile, deepseek-chat, gpt-4o", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (customModelInput.isNotBlank()) {
                                    IconButton(onClick = {
                                        customModelInput = ""
                                        selectedModel = "auto"
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "This model ID will be passed directly to your active AI provider.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                HorizontalDivider(color = BorderDark, thickness = 1.dp)

                // ---------------------------------------------------------------------------------
                // 4. Configured API Keys List (Switch, Test, Delete)
                // ---------------------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configured API Keys (${configuredKeys.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (configuredKeys.size > 1) {
                            Text(
                                text = "Tap key to activate",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                        }
                    }

                    if (configuredKeys.isEmpty()) {
                        Surface(
                            color = BackgroundDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No custom API keys added yet. Add a Gemini, Groq, or OpenRouter key below, or use the unlimited Free API mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        configuredKeys.forEachIndexed { index, item ->
                            val isTesting = keyTestingMap[item.id] == true
                            val testRes = keyTestResults[item.id]
                            val provider = item.provider

                            Surface(
                                color = if (item.isActive) GoldAccentDim.copy(alpha = 0.2f) else SurfaceElevatedDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isActive) GoldAccentBorder else BorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Switch active key
                                        val updated = configuredKeys.map { it.copy(isActive = it.id == item.id) }
                                        configuredKeys.clear()
                                        configuredKeys.addAll(updated)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Active Radio / Check Indicator
                                        Icon(
                                            imageVector = if (item.isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = if (item.isActive) "Active key" else "Inactive key",
                                            tint = if (item.isActive) GoldAccent else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        // Provider Badge
                                        Surface(
                                            color = getProviderBgColor(provider),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, getProviderColor(provider).copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = provider.displayName.substringBefore(" "),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = getProviderColor(provider),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Key Label or masked snippet
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.label.ifBlank { "${provider.displayName} Key ${index + 1}" },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (item.isActive) GoldAccent else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.maskedKey,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = TextSecondary
                                            )
                                        }

                                        // Test Single Key Button
                                        OutlinedButton(
                                            onClick = {
                                                keyTestingMap[item.id] = true
                                                keyTestResults.remove(item.id)
                                                coroutineScope.launch {
                                                    val res = GeminiApiKeyManager.testApiKey(item.key)
                                                    keyTestResults[item.id] = res
                                                    keyTestingMap[item.id] = false
                                                }
                                            },
                                            enabled = !isTesting,
                                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            if (isTesting) {
                                                CircularProgressIndicator(
                                                    color = GoldAccent,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Test",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    "Test",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = GoldAccent
                                                )
                                            }
                                        }

                                        // Delete Key Button
                                        IconButton(
                                            onClick = {
                                                val wasActive = item.isActive
                                                configuredKeys.remove(item)
                                                if (wasActive && configuredKeys.isNotEmpty()) {
                                                    val first = configuredKeys[0]
                                                    configuredKeys[0] = first.copy(isActive = true)
                                                }
                                                keyTestResults.remove(item.id)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete key",
                                                tint = CoralAccent,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    // Inline single-key test result banner
                                    if (testRes != null) {
                                        val (bgColor, textColor, icon) = when (testRes) {
                                            is ApiKeyTestResult.Success -> Triple(EmeraldAccent.copy(alpha = 0.15f), EmeraldAccent, Icons.Default.CheckCircle)
                                            is ApiKeyTestResult.RateLimited -> Triple(GoldAccent.copy(alpha = 0.15f), GoldAccent, Icons.Default.AutoAwesome)
                                            is ApiKeyTestResult.Error -> Triple(CoralAccent.copy(alpha = 0.15f), CoralAccent, Icons.Default.Warning)
                                        }
                                        val message = when (testRes) {
                                            is ApiKeyTestResult.Success -> testRes.message
                                            is ApiKeyTestResult.RateLimited -> testRes.message
                                            is ApiKeyTestResult.Error -> testRes.message
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bgColor, RoundedCornerShape(6.dp))
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = message,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDark, thickness = 1.dp)

                // ---------------------------------------------------------------------------------
                // 5. Add New Key Form
                // ---------------------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Add New API Key",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = newKeyInput,
                            onValueChange = { newKeyInput = it },
                            label = { Text("API Key Token", style = MaterialTheme.typography.bodySmall) },
                            placeholder = {
                                Text(
                                    "Paste Gemini (AIzaSy.../AQ...), Groq (gsk_...), or OpenRouter (sk-or-...) key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                textDirection = TextDirection.Ltr,
                                textAlign = TextAlign.Left
                            ),
                            singleLine = true,
                            visualTransformation = if (isNewKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            newKeyInput = clip.trim()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { isNewKeyPasswordVisible = !isNewKeyPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isNewKeyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isNewKeyPasswordVisible) "Hide key" else "Show key",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_gemini_api_key")
                        )
                    }

                    // Live auto-detection chip for new key
                    if (sanitizedNewKey.isNotBlank()) {
                        when (detectedNewKeyProvider) {
                            AiKeyProvider.OAUTH_OR_INVALID -> {
                                Surface(
                                    color = CoralAccent.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Unrecognized key format. Expected 'AIzaSy...' or 'AQ...' (Gemini), 'gsk_...' (Groq), or 'sk-...' (OpenRouter/OpenAI).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralAccent,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    color = getProviderBgColor(detectedNewKeyProvider),
                                    border = BorderStroke(1.dp, getProviderColor(detectedNewKeyProvider).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = getProviderColor(detectedNewKeyProvider),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Detected ${detectedNewKeyProvider.displayName} API Key",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = getProviderColor(detectedNewKeyProvider)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Key Label & Add Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newKeyLabelInput,
                            onValueChange = { newKeyLabelInput = it },
                            placeholder = {
                                Text("Optional Label (e.g. Work Groq)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        val isNewKeyValid = sanitizedNewKey.isNotBlank() && detectedNewKeyProvider != AiKeyProvider.OAUTH_OR_INVALID
                        Button(
                            onClick = {
                                if (isNewKeyValid) {
                                    val isFirst = configuredKeys.isEmpty()
                                    val countSameProvider = configuredKeys.count { it.provider == detectedNewKeyProvider } + 1
                                    val finalLabel = newKeyLabelInput.trim().ifBlank {
                                        "${detectedNewKeyProvider.displayName.substringBefore(" ")} Key $countSameProvider"
                                    }
                                    val newItem = ConfiguredApiKey(
                                        id = java.util.UUID.randomUUID().toString(),
                                        key = sanitizedNewKey,
                                        providerName = detectedNewKeyProvider.name,
                                        label = finalLabel,
                                        isActive = isFirst
                                    )
                                    configuredKeys.add(newItem)
                                    newKeyInput = ""
                                    newKeyLabelInput = ""
                                }
                            },
                            enabled = isNewKeyValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = BackgroundDark,
                                disabledContainerColor = BorderDark,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                HorizontalDivider(color = BorderDark, thickness = 1.dp)

                // ---------------------------------------------------------------------------------
                // 6. Free API Connectivity Test & Provider Portals
                // ---------------------------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Free API button
                    OutlinedButton(
                        onClick = {
                            isTestingFreeApi = true
                            freeApiTestResult = null
                            coroutineScope.launch {
                                val freeRes = FreeDictionaryAndTranslationEngine.testFreeApiConnection()
                                freeApiTestResult = if (freeRes.isSuccess) {
                                    ApiKeyTestResult.Success(freeRes.getOrThrow())
                                } else {
                                    ApiKeyTestResult.Error(freeRes.exceptionOrNull()?.message ?: "Free API test failed")
                                }
                                isTestingFreeApi = false
                            }
                        },
                        enabled = !isTestingFreeApi,
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

                    // Copy Gemini Portal URL
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://aistudio.google.com/apikey"))
                            copyUrlFeedback = "Copied Google AI Studio URL!"
                        },
                        border = BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gemini Portal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    // Copy Groq Portal URL
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://console.groq.com/keys"))
                            copyUrlFeedback = "Copied Groq Console URL!"
                        },
                        border = BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Groq Portal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                if (copyUrlFeedback != null) {
                    Text(
                        text = copyUrlFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent
                    )
                }

                // Free API Test Result Banner
                freeApiTestResult?.let { result ->
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
                    // Auto-add pending key if entered in text field
                    if (sanitizedNewKey.isNotBlank() && detectedNewKeyProvider != AiKeyProvider.OAUTH_OR_INVALID) {
                        if (configuredKeys.none { it.key == sanitizedNewKey }) {
                            val isFirst = configuredKeys.isEmpty()
                            val countSame = configuredKeys.count { it.provider == detectedNewKeyProvider } + 1
                            val finalLabel = newKeyLabelInput.trim().ifBlank {
                                "${detectedNewKeyProvider.displayName.substringBefore(" ")} Key $countSame"
                            }
                            configuredKeys.add(
                                ConfiguredApiKey(
                                    key = sanitizedNewKey,
                                    providerName = detectedNewKeyProvider.name,
                                    label = finalLabel,
                                    isActive = isFirst
                                )
                            )
                        }
                    }

                    // Calculate selected model
                    val finalModel = if (isCustomModelSelected) {
                        customModelInput.trim().ifBlank { "auto" }
                    } else {
                        selectedModel.trim().ifBlank { "auto" }
                    }

                    // Save all configurations
                    GeminiApiKeyManager.setEngineMode(context, selectedEngineMode)
                    GeminiApiKeyManager.setSelectedModel(context, finalModel)
                    GeminiApiKeyManager.saveConfiguredKeyItems(context, configuredKeys.toList())

                    val activeKey = configuredKeys.firstOrNull { it.isActive }?.key
                        ?: configuredKeys.firstOrNull()?.key.orEmpty()

                    onSaveKey(activeKey)
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
                if (configuredKeys.isNotEmpty() || keySource.contains("Custom", ignoreCase = true) || selectedModel != "auto") {
                    OutlinedButton(
                        onClick = {
                            GeminiApiKeyManager.clearCustomApiKey(context)
                            GeminiApiKeyManager.setSelectedModel(context, "auto")
                            GeminiApiKeyManager.setEngineMode(context, TranslationEngineMode.SMART_AUTO)
                            configuredKeys.clear()
                            selectedModel = "auto"
                            isCustomModelSelected = false
                            customModelInput = ""
                            newKeyInput = ""
                            newKeyLabelInput = ""
                            selectedEngineMode = TranslationEngineMode.SMART_AUTO
                            freeApiTestResult = null
                            onResetToDefault()
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
                        Text("Reset All", style = MaterialTheme.typography.labelMedium)
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
