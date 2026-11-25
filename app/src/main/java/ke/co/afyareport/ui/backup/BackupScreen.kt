package ke.co.afyareport.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.BackupManager
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var lastBackup by remember { mutableStateOf(BackupManager.lastBackup(context)) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = BackupManager.backupTo(context, uri)
            lastBackup = BackupManager.lastBackup(context)
            message = if (ok) {
                "Backup saved. Keep a copy somewhere safe, such as your email or Google Drive."
            } else {
                "The backup could not be saved. Try another location."
            }
            busy = false
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) restoreUri = uri }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Replace everything with this backup?") },
            text = {
                Text(
                    "All visits and patients on this phone will be replaced by the backup. " +
                        "A safety copy of the current data is kept inside the app, and the app will restart."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null
                    scope.launch {
                        busy = true
                        val err = BackupManager.restoreFrom(context, uri)
                        busy = false
                        if (err == null) BackupManager.restartApp(context) else message = err
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Backup and restore", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            message?.let { Notice(it) }
            if (busy) Text("Working...", style = MaterialTheme.typography.bodyMedium)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Back up your data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (lastBackup == 0L) {
                            "You have not made a backup yet. If this phone is lost or reset, everything on it is lost too."
                        } else {
                            "Last backup: ${DateFormat.getDateTimeInstance().format(Date(lastBackup))}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "The file holds every visit and every patient name, so keep it private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { backupLauncher.launch("AfyaReport-backup-${LocalDate.now()}.db") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save a backup file") }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restore from a backup file") }
                }
            }
        }
    }
}
