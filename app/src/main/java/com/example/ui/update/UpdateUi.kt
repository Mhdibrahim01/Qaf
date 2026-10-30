package com.example.ui.update

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.update.UpdateInfo
import com.example.data.update.UpdateRepository
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUiState
    data class ReadyToInstall(val info: UpdateInfo, val apk: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/** Small self-contained state holder so no ViewModel / screen signature has to change. */
class UpdateController(
    private val repository: UpdateRepository,
    private val scope: CoroutineScope,
    private val onUpToDate: () -> Unit = {}
) {
    var state: UpdateUiState by mutableStateOf(UpdateUiState.Idle)
        private set

    /** [manual] = user pressed "Check for updates": ignore throttle/skip and report results. */
    fun check(manual: Boolean) {
        if (state is UpdateUiState.Checking || state is UpdateUiState.Downloading) return
        if (!manual && !repository.shouldAutoCheck()) return
        scope.launch {
            state = UpdateUiState.Checking
            val info = repository.fetchLatest()
            repository.markChecked()
            state = when {
                info == null -> if (manual) UpdateUiState.Error("Couldn't check for updates. Check your connection.") else UpdateUiState.Idle
                !repository.isNewer(info) -> {
                    if (manual) onUpToDate()
                    UpdateUiState.Idle
                }
                !manual && repository.isSkipped(info) -> UpdateUiState.Idle
                else -> UpdateUiState.Available(info)
            }
        }
    }

    fun startDownload() {
        val info = (state as? UpdateUiState.Available)?.info ?: return
        scope.launch {
            state = UpdateUiState.Downloading(info, 0f)
            val file = repository.download(info) { p -> state = UpdateUiState.Downloading(info, p) }
            state = if (file != null) {
                UpdateUiState.ReadyToInstall(info, file)
            } else {
                UpdateUiState.Error("Download failed or the file could not be verified. Please try again.")
            }
        }
    }

    /** Returns false if the user must first grant "Install unknown apps" (settings screen opened). */
    fun install(): Boolean {
        val ready = state as? UpdateUiState.ReadyToInstall ?: return true
        return repository.install(ready.apk)
    }

    fun later() {
        (state as? UpdateUiState.Available)?.let { repository.skip(it.info) }
        state = UpdateUiState.Idle
    }

    fun dismiss() {
        state = UpdateUiState.Idle
    }
}

@Composable
fun rememberUpdateController(): UpdateController {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember {
        UpdateController(
            repository = UpdateRepository(context),
            scope = scope,
            onUpToDate = { Toast.makeText(context, "You're on the latest version", Toast.LENGTH_SHORT).show() }
        )
    }
}

/** Renders the dialog for the controller's current state (nothing when Idle/Checking). */
@Composable
fun UpdateDialogHost(controller: UpdateController) {
    val context = LocalContext.current
    when (val s = controller.state) {
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = { controller.later() },
            containerColor = SurfaceDark,
            title = { Text("Update available", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version ${s.info.versionName} is ready to download.", color = TextSecondary)
                    if (s.info.notes.isNotBlank()) Text(s.info.notes, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = { controller.startDownload() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark)
                ) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { controller.later() }) { Text("Later", color = TextMuted) } }
        )

        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            containerColor = SurfaceDark,
            title = { Text("Downloading update…", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth(), color = GoldAccent)
                    Text("${(s.progress * 100).toInt()}%", color = TextSecondary)
                }
            },
            confirmButton = {}
        )

        is UpdateUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            containerColor = SurfaceDark,
            title = { Text("Ready to install", color = TextPrimary) },
            text = {
                Text(
                    "Version ${s.info.versionName} was downloaded and verified. Tap Install and confirm in the Android installer. " +
                        "If Android asks, allow installs from this app first, then tap Install again.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val launched = controller.install()
                        if (launched) controller.dismiss()
                        else Toast.makeText(context, "Allow installs from this app, then come back and tap Install again.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark)
                ) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { controller.dismiss() }) { Text("Cancel", color = TextMuted) } }
        )

        is UpdateUiState.Error -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            containerColor = SurfaceDark,
            title = { Text("Update", color = TextPrimary) },
            text = { Text(s.message, color = TextSecondary) },
            confirmButton = { TextButton(onClick = { controller.dismiss() }) { Text("OK", color = GoldAccent) } }
        )

        UpdateUiState.Idle, UpdateUiState.Checking -> Unit
    }
}

/** Settings row: manual "Check for updates" (owns its own controller + dialog). */
@Composable
fun UpdateSettingsRow(modifier: Modifier = Modifier) {
    val controller = rememberUpdateController()
    Surface(
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier
            .fillMaxWidth()
            .clickable { controller.check(manual = true) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Check for updates", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Downloads the newest version from GitHub", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Text(
                if (controller.state is UpdateUiState.Checking) "Checking…" else "Check",
                color = GoldAccent,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    UpdateDialogHost(controller)
}
