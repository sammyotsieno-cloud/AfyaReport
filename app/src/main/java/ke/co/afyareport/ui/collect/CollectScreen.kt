@file:OptIn(ExperimentalLayoutApi::class)

package ke.co.afyareport.ui.collect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.registerList
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card

/** The registers currently in the app. */
@Composable
fun CollectScreen(
    repo: Repo,
    onAddVisit: () -> Unit,
    onBook: () -> Unit,
    onPatients: () -> Unit,
    onSettings: () -> Unit
) {
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Data collection") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Pick a register to add visits, browse what is recorded, or change its settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            registerList.forEach { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${r.name} (${r.code})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            r.about,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${patients.size} patients  •  ${visits.size} visits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onAddVisit, modifier = Modifier.fillMaxWidth()) { Text("Add a visit") }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBook) { Text("Register book") }
                            OutlinedButton(onClick = onPatients) { Text("Patients") }
                            OutlinedButton(onClick = onSettings) { Text("Compulsory fields") }
                        }
                    }
                }
            }
        }
    }
}
