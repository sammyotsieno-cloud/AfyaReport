package ke.co.afyareport.ui.tools

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.backup.BackupManager
import ke.co.afyareport.data.backup.RegisterTransfer
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun ToolsScreen(
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onImported: (Long) -> Unit
) {
    val context = LocalContext.current
    val reportRepo = remember { ReportRepository(AfyaReportDatabase.getInstance(context).reportDao()) }
    val scope = rememberCoroutineScope()
    val registers by registerRepository.observeAllRegisters().collectAsState(initial = emptyList())
    var message by remember { mutableStateOf<String?>(null) }
    var lastBackup by remember { mutableStateOf(BackupManager.lastBackup(context)) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var exportId by remember { mutableStateOf(0L) }
    var pasted by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    suspend fun doImport(text: String) {
        busy = true
        val r = RegisterTransfer.importJson(text, registerRepository, reportRepo)
        busy = false
        val newId = r.registerId
        if (newId != null) onImported(newId) else message = r.error
    }

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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val json = RegisterTransfer.toJson(exportId, registerRepository, reportRepo)
            val ok = json != null && withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
                }.getOrDefault(false)
            }
            message = if (ok) "Setup file saved." else "The file could not be saved."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) message = "That file could not be read." else doImport(text)
        }
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Replace everything with this backup?") },
            text = {
                Text(
                    "All registers and patients on this phone will be replaced by the backup. " +
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
        topBar = { AfyaTopBar(title = "Backup and sharing", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                        .padding(12.dp)
                )
            }
            if (busy) {
                Text("Working…", style = MaterialTheme.typography.bodyMedium)
            }

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

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Share a register's setup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Sends the columns, keys and report lines, never any patients. Someone else imports it and gets the same register.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (registers.isEmpty()) {
                        Text("No registers yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                    registers.forEach { r ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(r.name, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val json = RegisterTransfer.toJson(r.id, registerRepository, reportRepo)
                                        if (json == null) {
                                            message = "Could not read that register."
                                        } else {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "${r.name} setup")
                                                putExtra(Intent.EXTRA_TEXT, json)
                                            }
                                            context.startActivity(Intent.createChooser(send, "Share register setup"))
                                        }
                                    }
                                }, modifier = Modifier.weight(1f)) { Text("Share as text") }
                                OutlinedButton(onClick = {
                                    exportId = r.id
                                    val safe = r.name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "register" }
                                    exportLauncher.launch("$safe-setup.json")
                                }, modifier = Modifier.weight(1f)) { Text("Save as file") }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import a register setup", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose a setup file") }
                    OutlinedTextField(
                        value = pasted,
                        onValueChange = { pasted = it },
                        label = { Text("Or paste the setup text here") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { scope.launch { doImport(pasted) } },
                        enabled = pasted.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Import pasted setup") }
                }
            }
        }
    }
}
