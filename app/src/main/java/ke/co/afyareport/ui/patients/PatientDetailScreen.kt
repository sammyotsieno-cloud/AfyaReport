package ke.co.afyareport.ui.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.ui.book.VisitRow
import ke.co.afyareport.ui.theme.AfyaTopBar
import kotlinx.coroutines.launch

@Composable
fun PatientDetailScreen(
    patientId: Long,
    repo: Repo,
    onBack: () -> Unit,
    onOpenVisit: (Long) -> Unit,
    onAddVisit: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    val patient = patients.firstOrNull { it.id == patientId }
    var nameDraft by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(patient?.id) { nameDraft = patient?.name ?: "" }

    val mine = visits.filter { it.patientId == patientId }
        .sortedWith(compareByDescending<ke.co.afyareport.data.VisitEntity> { it.visitDate }.thenByDescending { it.id })

    if (confirmDelete && patient != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this patient?") },
            text = { Text("${patient.patientNo} and all ${mine.size} of their visits will be removed from the register and every report.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.dao.deletePatient(patient.id)
                        confirmDelete = false
                        onBack()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(title = patient?.patientNo ?: "Patient", onBack = onBack, actions = {
                if (patient != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete patient")
                    }
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (patient != null) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enrolled " + (mine.lastOrNull()?.visitDate?.asDate()?.format(dayFormat) ?: "—") +
                            "  •  ${mine.size} visit(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        label = { Text("Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { scope.launch { repo.dao.updatePatient(patient.copy(name = nameDraft.trim())) } },
                        enabled = nameDraft.trim() != patient.name,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save name") }
                    Button(onClick = { onAddVisit(patient.patientNo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add a visit for this patient")
                    }
                }
                Text(
                    "Visits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                mine.forEach { v ->
                    VisitRow(v, patient) { onOpenVisit(v.id) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
