package ke.co.afyareport.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.BackupManager
import ke.co.afyareport.data.Repo
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.theme.MarginCard

@Composable
fun HomeScreen(
    repo: Repo,
    onAddVisit: () -> Unit,
    onBook: () -> Unit,
    onPatients: () -> Unit,
    onReport: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    val lastBackup = remember { BackupManager.lastBackup(context) }
    val weekMs = 7L * 24 * 60 * 60 * 1000
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AfyaReport", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Diabetes and Hypertension register (MOH 222)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${patients.size} patients enrolled  •  ${visits.size} visits recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (visits.isNotEmpty() && System.currentTimeMillis() - lastBackup > weekMs) {
                Notice(
                    if (lastBackup == 0L) "You have not backed up yet. Do it now so nothing is lost."
                    else "Your last backup is more than a week old.",
                    warning = true
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAddVisit, modifier = Modifier.fillMaxWidth()) { Text("Add a visit") }
            MarginCard(onClick = onReport) {
                Text("Monthly report (740)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The summary form, worked out from the register.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MarginCard(onClick = onBook) {
                Text("Register book", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Every visit, month by month. Open one to change it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MarginCard(onClick = onPatients) {
                Text("Patients", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Everyone enrolled, in patient number order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MarginCard(onClick = onBackup) {
                Text("Backup and restore", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Keep a copy of your data safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MarginCard(onClick = onSettings) {
                Text("Compulsory fields", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose which columns must be filled in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
