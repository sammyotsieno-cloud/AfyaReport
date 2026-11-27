package ke.co.afyareport.ui.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.naturalCompare
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import java.time.Instant
import java.time.ZoneId

/** The enrolment list: one row per patient number, in order. Enrolled = date of the earliest visit. */
@Composable
fun PatientsScreen(
    repo: Repo,
    onBack: () -> Unit,
    onOpenPatient: (Long) -> Unit,
    onAdd: () -> Unit
) {
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val byPatient = remember(visits) { visits.groupBy { it.patientId } }
    val shown = remember(patients, query) {
        patients.filter {
            query.isBlank() || it.patientNo.contains(query.trim(), true) || it.name.contains(query.trim(), true)
        }.sortedWith(Comparator { a, b -> naturalCompare(a.patientNo, b.patientNo) })
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Patients", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add a patient") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${patients.size} patients enrolled, listed by patient number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Find a patient number or name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (patients.isEmpty()) {
                item {
                    EmptyState(
                        title = "No patients yet",
                        body = "Patients are added here automatically the first time you use their number on a visit, or you can add one yourself.",
                        actionLabel = "Add a patient",
                        onAction = onAdd
                    )
                }
            }
            items(shown, key = { it.id }) { p ->
                val list = byPatient[p.id].orEmpty()
                val first = list.minOfOrNull { it.visitDate }?.asDate()
                val last = list.maxOfOrNull { it.visitDate }?.asDate()
                val registered = Instant.ofEpochMilli(p.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { onOpenPatient(p.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        p.patientNo + if (p.name.isNotBlank()) "  –  ${p.name}" else "",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (list.isEmpty()) "No visits yet  •  registered ${registered.format(dayFormat)}"
                        else "${list.size} visit(s)  •  enrolled ${first?.format(dayFormat)}" +
                            (if (last != null && last != first) "  •  last ${last.format(dayFormat)}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
