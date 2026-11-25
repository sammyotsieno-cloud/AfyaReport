package ke.co.afyareport.ui.patients

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.labelOf
import ke.co.afyareport.domain.visitSections
import ke.co.afyareport.ui.common.InfoCard
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.StatusChip
import java.time.format.DateTimeFormatter

private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

/** One visit, read-only. The pencil at the top right opens it for editing. */
@Composable
fun VisitSummaryScreen(
    entryId: Long,
    repo: Repo,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val visits by repo.dao.observeVisits().collectAsState<List<VisitEntity>, List<VisitEntity>?>(initial = null)
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visit = visits?.firstOrNull { it.id == entryId }
    val patient = patients.firstOrNull { it.id == visit?.patientId }
    val sections = remember(visit) { if (visit != null) visitSections(visit) else emptyList() }

    // If the visit was deleted from the edit screen, step back out of this summary too.
    LaunchedEffect(visits != null, visit != null) {
        if (visits != null && visit == null) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(title = "Visit summary", onBack = onBack, actions = {
                if (visit != null) {
                    IconButton(onClick = { onEdit(visit.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit this visit")
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
            if (visit != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(visit.visitDate.asDate().format(longDate), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Patient ${patient?.patientNo ?: "?"}" +
                                (if (!patient?.name.isNullOrBlank()) "  •  ${patient?.name}" else ""),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (visit.condition.isNotBlank()) {
                                StatusChip(Keys.condition.labelOf(visit.condition), ChipTone.Neutral)
                            }
                            if (visit.status.isNotBlank()) {
                                StatusChip(
                                    Keys.status.labelOf(visit.status),
                                    if (visit.status == "a") ChipTone.Done else ChipTone.Neutral
                                )
                            }
                        }
                    }
                }
                sections.forEach { InfoCard(it.title, it.rows) }
            }
        }
    }
}
