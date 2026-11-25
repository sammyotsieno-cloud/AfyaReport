package ke.co.afyareport.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.patientBrief
import ke.co.afyareport.domain.properName
import ke.co.afyareport.ui.book.VisitRow
import ke.co.afyareport.ui.common.InfoRow
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Button
import ke.co.afyareport.ui.theme.Card
import ke.co.afyareport.ui.theme.HeroCard
import ke.co.afyareport.ui.theme.SectionHeader
import kotlinx.coroutines.launch

/** A patient at a glance: brief info first, then every visit. Tap a visit for its summary. */
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
    var editName by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val mine = remember(visits, patientId) {
        visits.filter { it.patientId == patientId }
            .sortedWith(compareByDescending<VisitEntity> { it.visitDate }.thenByDescending { it.id })
    }
    val enrolled = mine.minOfOrNull { it.visitDate }?.asDate()
    val last = mine.maxOfOrNull { it.visitDate }?.asDate()
    val brief = remember(mine) { patientBrief(mine) }
    val scheme = MaterialTheme.colorScheme

    if (editName && patient != null) {
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Patient name") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.dao.updatePatient(patient.copy(name = properName(nameDraft)))
                        editName = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } }
        )
    }

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
            AfyaTopBar(title = "Patient summary", onBack = onBack, actions = {
                if (patient != null) {
                    IconButton(onClick = { nameDraft = patient.name; editName = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit name")
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (patient != null) {
                HeroCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Patient number",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(patient.patientNo, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(
                            if (patient.name.isNotBlank()) patient.name else "No name recorded",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Enrolled " + (enrolled?.format(dayFormat) ?: "—") + "  •  ${mine.size} visit(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        if (last != null && last != enrolled) {
                            Text(
                                "Last visit ${last.format(dayFormat)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                if (brief.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Brief info", style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                            brief.forEachIndexed { i, (k, v) ->
                                InfoRow(k, v)
                                if (i < brief.lastIndex) HorizontalDivider(color = scheme.outlineVariant)
                            }
                        }
                    }
                }

                Button(onClick = { onAddVisit(patient.patientNo) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add a visit for this patient")
                }

                SectionHeader("Visits")
                if (mine.isEmpty()) {
                    Text(
                        "No visits are recorded for this patient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = scheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, scheme.outlineVariant)
                    ) {
                        Column {
                            mine.forEachIndexed { i, v ->
                                VisitRow(v, patient) { onOpenVisit(v.id) }
                                if (i < mine.lastIndex) HorizontalDivider(color = scheme.outlineVariant)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete this patient") }
            }
        }
    }
}
