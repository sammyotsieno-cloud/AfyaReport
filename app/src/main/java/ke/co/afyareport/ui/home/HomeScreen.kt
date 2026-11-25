package ke.co.afyareport.ui.home

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.BackupManager
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.DraftStore
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.registerList
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.theme.Button
import ke.co.afyareport.ui.theme.Card
import ke.co.afyareport.ui.theme.HeroCard
import ke.co.afyareport.ui.theme.HeroStat
import ke.co.afyareport.ui.theme.MarginCard
import ke.co.afyareport.ui.theme.OutlinedButton
import ke.co.afyareport.ui.theme.SectionHeader
import java.text.DateFormat
import java.time.LocalTime
import java.time.YearMonth
import java.util.Date

@Composable
fun HomeScreen(
    repo: Repo,
    drafts: DraftStore,
    onContinueDraft: () -> Unit,
    onAddVisit: () -> Unit,
    onReport: () -> Unit,
    onBook: () -> Unit,
    onBackup: () -> Unit
) {
    val context = LocalContext.current
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    val draft by drafts.info.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    val lastBackup = remember { BackupManager.lastBackup(context) }
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val thisMonth = remember { YearMonth.now() }
    val monthVisits = remember(visits) { visits.count { YearMonth.from(it.visitDate.asDate()) == thisMonth } }
    val lastVisit = visits.maxOfOrNull { it.visitDate }?.asDate()?.format(dayFormat)
    val greeting = remember {
        val h = LocalTime.now().hour
        when {
            h < 12 -> "Good morning"
            h < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard the paused entry?") },
            text = { Text("Everything typed in it will be lost.") },
            confirmButton = {
                TextButton(onClick = { drafts.clear(); confirmDiscard = false }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep it") } }
        )
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "Diabetes and Hypertension",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "MOH 222 register" + (if (lastVisit != null) "  •  last visit $lastVisit" else ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    HeroStat(patients.size.toString(), "Patients", Modifier.weight(1f))
                    HeroStat(monthVisits.toString(), "Visits this month", Modifier.weight(1f))
                    HeroStat(visits.size.toString(), "All visits", Modifier.weight(1f))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAddVisit, modifier = Modifier.weight(1f)) { Text("Add a visit") }
                OutlinedButton(onClick = onReport, modifier = Modifier.weight(1f)) { Text("Monthly report") }
            }

            if (visits.isNotEmpty() && System.currentTimeMillis() - lastBackup > weekMs) {
                Notice(
                    if (lastBackup == 0L) "You have not backed up yet. Do it now so nothing is lost."
                    else "Your last backup is more than a week old.",
                    warning = true
                )
            }

            SectionHeader("Continue where you left off")
            val d = draft
            if (d != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Paused entry", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Patient ${d.patientNo.ifBlank { "(number not entered yet)" }}  •  saved " +
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(d.savedAt)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onContinueDraft, modifier = Modifier.weight(1f)) { Text("Continue") }
                            OutlinedButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f)) {
                                Text("Discard")
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Nothing is paused. Anything you leave half-finished while adding a visit waits for you here, even if you close the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader("Registers in use")
            registerList.forEach { r ->
                MarginCard(onClick = onBook) {
                    Text("${r.name} (${r.code})", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${patients.size} patients  •  ${visits.size} visits. Tap to open the register book.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionHeader("Safekeeping")
            MarginCard(onClick = onBackup) {
                Text("Backup and restore", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Keep a copy of your data safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
