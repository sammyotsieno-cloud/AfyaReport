package ke.co.afyareport.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.ReportingSessionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
    onOpenSummary: (Long) -> Unit
) {
    var session by remember { mutableStateOf<ReportingSessionEntity?>(null) }
    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var variables by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var codeOptions by remember { mutableStateOf<Map<Long, List<CodeOptionEntity>>>(emptyMap()) }
    var entries by remember { mutableStateOf<List<PatientEntryEntity>>(emptyList()) }
    var showEntryForm by remember { mutableStateOf(false) }
    var viewingEntry by remember { mutableStateOf<PatientEntryEntity?>(null) }
    var viewingValues by remember { mutableStateOf<List<VariableValueEntity>>(emptyList()) }

    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            val currentSession = sessionRepository.getSession(sessionId) ?: return@launch
            session = currentSession
            register = registerRepository.getRegister(currentSession.registerDefinitionId)
            val currentVariables = registerRepository.getVariables(currentSession.registerDefinitionId)
            variables = currentVariables
            val optionsMap = mutableMapOf<Long, List<CodeOptionEntity>>()
            currentVariables.filter { it.type == VariableType.CODED }.forEach { variable ->
                optionsMap[variable.id] = registerRepository.getCodeOptions(variable.id)
            }
            codeOptions = optionsMap
            entries = sessionRepository.getEntries(sessionId)
        }
    }

    LaunchedEffect(sessionId) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.customLabel?.ifBlank { "Session" } ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenSummary(sessionId) }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Summary")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showEntryForm && viewingEntry == null &&
                session?.status != ReportingSessionEntity.STATUS_COMPLETED
            ) {
                FloatingActionButton(onClick = { showEntryForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add patient entry")
                }
            }
        }
    ) { padding ->
        when {
            showEntryForm -> {
                PatientEntryForm(
                    sessionId = sessionId,
                    variables = variables,
                    codeOptions = codeOptions,
                    sessionRepository = sessionRepository,
                    modifier = Modifier.padding(padding),
                    onCancel = { showEntryForm = false },
                    onSave = { visitType, patientKey, values ->
                        scope.launch {
                            sessionRepository.addPatientEntry(
                                sessionId = sessionId,
                                visitType = visitType,
                                patientKey = patientKey,
                                sortOrder = entries.size,
                                values = values
                            )
                            showEntryForm = false
                            reload()
                        }
                    }
                )
            }

            viewingEntry != null -> {
                EntryDetailView(
                    entry = viewingEntry!!,
                    values = viewingValues,
                    variables = variables,
                    codeOptions = codeOptions,
                    modifier = Modifier.padding(padding),
                    onClose = {
                        viewingEntry = null
                        viewingValues = emptyList()
                    }
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    register?.let {
                        Text(text = it.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "${entries.size} patient entries  •  ${session?.status ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (session?.status != ReportingSessionEntity.STATUS_COMPLETED) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    session?.let {
                                        sessionRepository.updateSession(
                                            it.copy(status = ReportingSessionEntity.STATUS_COMPLETED)
                                        )
                                        reload()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text("  Mark as Completed")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (entries.isEmpty()) {
                        Text(
                            text = "No entries yet. Tap + to add the first patient.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(entries, key = { it.id }) { entry ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                viewingValues = sessionRepository.getValuesForEntry(entry.id)
                                                viewingEntry = entry
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Entry #${entry.sortOrder + 1}  •  ${entry.visitType}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        entry.patientKey?.let {
                                            Text(
                                                text = "Key: $it",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryDetailView(
    entry: PatientEntryEntity,
    values: List<VariableValueEntity>,
    variables: List<VariableDefinitionEntity>,
    codeOptions: Map<Long, List<CodeOptionEntity>>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val valueMap = values.associateBy { it.variableDefinitionId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Entry #${entry.sortOrder + 1}", style = MaterialTheme.typography.titleLarge)
        Text("Visit type: ${entry.visitType}", style = MaterialTheme.typography.bodyLarge)
        entry.patientKey?.let {
            Text("Patient key: $it", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        variables.forEach { variable ->
            val raw = valueMap[variable.id]?.value
            val display = when (variable.type) {
                VariableType.CODED -> {
                    val option = codeOptions[variable.id]?.find { it.code == raw }
                    if (option != null) "${option.code} – ${option.label}" else raw ?: "—"
                }
                VariableType.BOOLEAN -> when (raw) {
                    "true" -> "Yes"
                    "false" -> "No"
                    else -> raw ?: "—"
                }
                VariableType.VISIT_TYPE -> raw ?: entry.visitType.name
                else -> raw ?: "—"
            }
            Column {
                Text(variable.name, style = MaterialTheme.typography.labelLarge)
                Text(display, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

@Composable
private fun PatientEntryForm(
    sessionId: Long,
    variables: List<VariableDefinitionEntity>,
    codeOptions: Map<Long, List<CodeOptionEntity>>,
    sessionRepository: SessionRepository,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSave: (VisitType, String?, List<VariableValueEntity>) -> Unit
) {
    var visitType by remember { mutableStateOf(VisitType.NEW) }
    var patientKey by remember { mutableStateOf("") }
    var possibleMatches by remember { mutableStateOf<List<PatientEntryEntity>>(emptyList()) }
    val values = remember { mutableStateMapOf<Long, String>() }

    LaunchedEffect(patientKey) {
        possibleMatches = if (patientKey.isBlank()) {
            emptyList()
        } else {
            sessionRepository.findPossibleMatches(sessionId, patientKey)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("New patient entry", style = MaterialTheme.typography.titleLarge)
        Text("Visit type *", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = visitType == VisitType.NEW,
                onClick = { visitType = VisitType.NEW },
                label = { Text("NEW") }
            )
            FilterChip(
                selected = visitType == VisitType.RETURN,
                onClick = { visitType = VisitType.RETURN },
                label = { Text("RETURN") }
            )
        }

        OutlinedTextField(
            value = patientKey,
            onValueChange = { patientKey = it },
            label = { Text("Patient key (optional – for matching returns)") },
            placeholder = { Text("e.g. name + age or local ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (possibleMatches.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Possible previous entries in this session:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    possibleMatches.forEach { match ->
                        Text(
                            text = "• Entry #${match.sortOrder + 1} (${match.visitType})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clickable { visitType = VisitType.RETURN }
                                .padding(vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Tap a match to set visit type to RETURN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        variables.filter { it.type != VariableType.VISIT_TYPE }.forEach { variable ->
            when (variable.type) {
                VariableType.CODED -> {
                    val options = codeOptions[variable.id].orEmpty()
                    Text(
                        variable.name + if (variable.isRequired) " *" else "",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (variable.measurementInstructions.isNotBlank()) {
                        Text(
                            variable.measurementInstructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    options.forEach { option ->
                        FilterChip(
                            selected = values[variable.id] == option.code,
                            onClick = { values[variable.id] = option.code },
                            label = { Text("${option.code} – ${option.label}") },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                VariableType.BOOLEAN -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = values[variable.id] == "true",
                            onClick = { values[variable.id] = "true" },
                            label = { Text("Yes") }
                        )
                        FilterChip(
                            selected = values[variable.id] == "false",
                            onClick = { values[variable.id] = "false" },
                            label = { Text("No") },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Text("  ${variable.name}", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = values[variable.id] ?: "",
                        onValueChange = { values[variable.id] = it },
                        label = { Text(variable.name + if (variable.isRequired) " *" else "") },
                        supportingText = {
                            if (variable.measurementInstructions.isNotBlank()) {
                                Text(variable.measurementInstructions)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = variable.type != VariableType.TEXT
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val valueEntities = values.map { (variableId, value) ->
                        VariableValueEntity(
                            patientEntryId = 0,
                            variableDefinitionId = variableId,
                            value = value
                        )
                    }
                    val visitTypeVariable = variables.find { it.type == VariableType.VISIT_TYPE }
                    val finalValues = if (visitTypeVariable != null) {
                        valueEntities + VariableValueEntity(
                            patientEntryId = 0,
                            variableDefinitionId = visitTypeVariable.id,
                            value = visitType.name
                        )
                    } else valueEntities
                    onSave(visitType, patientKey.ifBlank { null }, finalValues)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save entry")
            }
        }
    }
}
