package ke.co.afyareport.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.engine.effectiveVisitTypes
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.StatusChip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/** The register as one continuous book: every patient row, newest first. */
@Composable
fun RegisterBookScreen(
    registerId: Long,
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onAddPatient: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onReport: () -> Unit,
    onReportSetup: () -> Unit,
    onEditRegister: () -> Unit
) {
    val context = LocalContext.current
    val reportRepo = remember { ReportRepository(AfyaReportDatabase.getInstance(context).reportDao()) }
    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var variables by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var allVars by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var entries by remember { mutableStateOf<List<PatientEntryEntity>>(emptyList()) }
    var valuesByEntry by remember { mutableStateOf<Map<Long, List<VariableValueEntity>>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(registerId) {
        val reg = registerRepository.getRegister(registerId) ?: return@LaunchedEffect
        register = reg
        variables = registerRepository.getVariablesForVersion(reg.id, reg.version)
        allVars = reportRepo.getAllVariables(reg.id)
        entries = reportRepo.getEntriesForRegister(reg.id)
        valuesByEntry = reportRepo.getValuesForRegister(reg.id).groupBy { it.patientEntryId }
        loaded = true
    }

    val visitTypes = remember(entries) { effectiveVisitTypes(entries) }
    val nameById = remember(allVars) { allVars.associate { it.id to it.name.trim().lowercase() } }
    val shownColumns = remember(variables) { variables.filter { it.type != VariableType.VISIT_TYPE } }
    val patientCount = remember(entries) {
        entries.map { e -> e.patientKey?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "#${e.id}" }
            .distinct().size
    }
    val rows = remember(entries, query) {
        entries
            .sortedWith(compareByDescending<PatientEntryEntity> { it.visitDate }.thenByDescending { it.id })
            .filter { query.isBlank() || (it.patientKey ?: "").contains(query.trim(), ignoreCase = true) }
    }

    fun detailsFor(e: PatientEntryEntity): String {
        val byName = valuesByEntry[e.id].orEmpty()
            .mapNotNull { v -> nameById[v.variableDefinitionId]?.let { it to v.value } }
            .toMap()
        return shownColumns
            .mapNotNull { col ->
                byName[col.name.trim().lowercase()]?.takeIf { it.isNotBlank() }?.let { "${col.name} $it" }
            }
            .take(3)
            .joinToString("  •  ")
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(title = register?.name ?: "Register", onBack = onBack, actions = {
                IconButton(onClick = onReport) { Icon(Icons.Default.Assessment, contentDescription = "Report") }
                IconButton(onClick = onReportSetup) { Icon(Icons.Default.Tune, contentDescription = "Report setup") }
                IconButton(onClick = onEditRegister) { Icon(Icons.Default.Edit, contentDescription = "Edit columns") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPatient,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add patient") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${entries.size} entries  •  $patientCount patients",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entries.isNotEmpty()) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Find a patient number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (loaded && entries.isEmpty()) {
                item {
                    EmptyState(
                        title = "No patients yet",
                        body = "Add patients from your paper register, one row at a time. Every row counts toward the report, whichever day you add it.",
                        actionLabel = "Add the first patient",
                        onAction = onAddPatient
                    )
                }
            } else if (loaded && rows.isEmpty()) {
                item {
                    Text(
                        "No patient number matches.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(rows, key = { it.id }) { e ->
                val date = Instant.ofEpochMilli(e.visitDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val first = (visitTypes[e.id] ?: e.visitType) == VisitType.NEW
                val details = detailsFor(e)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { onOpenEntry(e.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        date.format(dayFormat),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                e.patientKey ?: "No number",
                                style = MaterialTheme.typography.titleSmall
                            )
                            StatusChip(
                                if (first) "First visit" else "Revisit",
                                if (first) ChipTone.Done else ChipTone.Neutral
                            )
                        }
                        if (details.isNotBlank()) {
                            Text(
                                details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
