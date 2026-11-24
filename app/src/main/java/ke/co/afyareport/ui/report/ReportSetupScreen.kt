package ke.co.afyareport.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.engine.Clause
import ke.co.afyareport.engine.ClauseJson
import ke.co.afyareport.engine.OP_AT_LEAST
import ke.co.afyareport.engine.OP_AT_MOST
import ke.co.afyareport.engine.OP_BETWEEN
import ke.co.afyareport.engine.OP_FILLED
import ke.co.afyareport.engine.OP_NO
import ke.co.afyareport.engine.OP_ONE_OF
import ke.co.afyareport.engine.OP_YES
import ke.co.afyareport.engine.ReportEngine
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import ke.co.afyareport.ui.theme.MarginCard
import kotlinx.coroutines.launch

class DraftClause(column: String = "", op: String = "", vals: List<String> = emptyList()) {
    var column by mutableStateOf(column)
    var op by mutableStateOf(op)
    val values = mutableStateListOf<String>().also { it.addAll(vals) }
}

class IndicatorDraft(
    val id: Long = 0,
    val sortOrder: Int = 0,
    section: String = "",
    label: String = "",
    countMode: String = ReportIndicatorEntity.COUNT_PATIENTS,
    matchAny: Boolean = false,
    ageBands: String = "",
    scope: String = ReportIndicatorEntity.SCOPE_PERIOD
) {
    var section by mutableStateOf(section)
    var label by mutableStateOf(label)
    var countMode by mutableStateOf(countMode)
    var matchAny by mutableStateOf(matchAny)
    var ageBands by mutableStateOf(ageBands)
    var scope by mutableStateOf(scope)
    val clauses = mutableStateListOf<DraftClause>()
}

private fun ReportIndicatorEntity.toDraft(): IndicatorDraft =
    IndicatorDraft(id, sortOrder, section, label, countMode, matchAny, ageBands, scope).also { d ->
        ClauseJson.decode(conditionsJson).forEach { d.clauses.add(DraftClause(it.column, it.op, it.values)) }
    }

private fun IndicatorDraft.toEntity(registerId: Long) = ReportIndicatorEntity(
    id = id,
    registerDefinitionId = registerId,
    section = section.trim(),
    label = label.trim(),
    countMode = countMode,
    matchAny = matchAny,
    ageBands = ageBands.trim(),
    scope = scope,
    conditionsJson = ClauseJson.encode(clauses.map { Clause(it.column, it.op, it.values.map { v -> v.trim() }) }),
    sortOrder = sortOrder
)

private fun DraftClause.setAt(i: Int, v: String) {
    while (values.size <= i) values.add("")
    values[i] = v
}

private fun DraftClause.isValid(): Boolean = column.isNotBlank() && when (op) {
    OP_ONE_OF -> values.any { it.isNotBlank() }
    OP_AT_LEAST, OP_AT_MOST -> values.getOrNull(0)?.trim()?.toDoubleOrNull() != null
    OP_BETWEEN -> values.getOrNull(0)?.trim()?.toDoubleOrNull() != null &&
        values.getOrNull(1)?.trim()?.toDoubleOrNull() != null
    OP_YES, OP_NO, OP_FILLED -> true
    else -> false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    options: List<String>,
    selected: String,
    allowNone: Boolean = false,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowNone) {
                DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(""); open = false })
            }
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeChooser(label: String, options: List<CodeOptionEntity>, current: String, onPick: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = current.equals(o.code, true),
                onClick = { onPick(o.code) },
                label = { Text("${o.code} – ${o.label}") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportSetupScreen(
    registerId: Long,
    registerRepository: RegisterRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reportRepo = remember { ReportRepository(AfyaReportDatabase.getInstance(context).reportDao()) }
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var variables by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var codeOptions by remember { mutableStateOf<Map<Long, List<CodeOptionEntity>>>(emptyMap()) }
    var settings by remember { mutableStateOf(ReportSettingsEntity(registerDefinitionId = registerId)) }
    var indicators by remember { mutableStateOf<List<ReportIndicatorEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<IndicatorDraft?>(null) }
    var toDelete by remember { mutableStateOf<ReportIndicatorEntity?>(null) }

    fun reload() {
        scope.launch {
            val reg = registerRepository.getRegister(registerId) ?: return@launch
            register = reg
            val vars = registerRepository.getVariablesForVersion(reg.id, reg.version)
            variables = vars
            codeOptions = vars.filter { it.type == VariableType.CODED }
                .associate { it.id to registerRepository.getCodeOptions(it.id) }
            settings = reportRepo.getSettings(registerId)
            indicators = reportRepo.getIndicators(registerId)
        }
    }
    LaunchedEffect(registerId) { reload() }

    fun updateSettings(s: ReportSettingsEntity) {
        settings = s
        scope.launch { reportRepo.saveSettings(s) }
    }

    toDelete?.let { ind ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this line?") },
            text = { Text(ind.label) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        reportRepo.deleteIndicator(ind.id)
                        toDelete = null
                        reload()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(
                title = if (editing == null) "Report setup" else "Report line",
                onBack = { if (editing != null) editing = null else onBack() }
            )
        }
    ) { padding ->
        val d = editing
        if (d != null) {
            IndicatorEditor(
                draft = d,
                variables = variables,
                codeOptions = codeOptions,
                hasAgeColumn = settings.ageColumn.isNotBlank(),
                modifier = Modifier.padding(padding),
                onCancel = { editing = null },
                onSave = {
                    scope.launch {
                        reportRepo.saveIndicator(d.toEntity(registerId))
                        editing = null
                        reload()
                    }
                }
            )
        } else {
            val names = variables.filter { it.type != VariableType.VISIT_TYPE }.map { it.name }
            val numberNames = variables.filter { it.type == VariableType.NUMBER }.map { it.name }
            val sexVar = variables.firstOrNull { it.name.equals(settings.sexColumn, true) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(register?.name ?: "", style = MaterialTheme.typography.titleLarge)

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Male and female", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Every report line is split into Male, Female and Total. Say which column records sex.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PickerField("Sex column", names, settings.sexColumn) {
                            updateSettings(settings.copy(sexColumn = it, maleValue = "", femaleValue = ""))
                        }
                        if (sexVar != null) {
                            if (sexVar.type == VariableType.CODED) {
                                val opts = codeOptions[sexVar.id].orEmpty()
                                CodeChooser("Which code means Male?", opts, settings.maleValue) {
                                    updateSettings(settings.copy(maleValue = it))
                                }
                                CodeChooser("Which code means Female?", opts, settings.femaleValue) {
                                    updateSettings(settings.copy(femaleValue = it))
                                }
                            } else {
                                OutlinedTextField(
                                    value = settings.maleValue,
                                    onValueChange = { updateSettings(settings.copy(maleValue = it)) },
                                    label = { Text("What do you write for Male? e.g. M") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = settings.femaleValue,
                                    onValueChange = { updateSettings(settings.copy(femaleValue = it)) },
                                    label = { Text("What do you write for Female? e.g. F") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        PickerField("Age column (only if you split lines by age)", numberNames, settings.ageColumn, allowNone = true) {
                            updateSettings(settings.copy(ageColumn = it))
                        }
                    }
                }

                Text("Report lines", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Each line is one row of your monthly summary form, for example \"Newly diagnosed diabetes cases\". Say what to count, using your columns and their keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                indicators.forEach { ind ->
                    MarginCard(onClick = { editing = ind.toDraft() }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                if (ind.section.isNotBlank()) {
                                    Text(ind.section, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(ind.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    ReportEngine.describeRule(ind, variables, codeOptions),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { toDelete = ind }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete this line")
                            }
                        }
                    }
                }

                Button(
                    onClick = { editing = IndicatorDraft(sortOrder = indicators.size) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add a report line")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndicatorEditor(
    draft: IndicatorDraft,
    variables: List<VariableDefinitionEntity>,
    codeOptions: Map<Long, List<CodeOptionEntity>>,
    hasAgeColumn: Boolean,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val canSave = draft.label.isNotBlank() && draft.clauses.all { it.isValid() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = draft.section,
            onValueChange = { draft.section = it },
            label = { Text("Heading (optional)") },
            placeholder = { Text("e.g. DIABETES") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.label,
            onValueChange = { draft.label = it },
            label = { Text("Name of this line") },
            placeholder = { Text("e.g. Newly diagnosed diabetes cases") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("What do you count?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.countMode == ReportIndicatorEntity.COUNT_PATIENTS,
                onClick = { draft.countMode = ReportIndicatorEntity.COUNT_PATIENTS },
                label = { Text("Patients (each once)") }
            )
            FilterChip(
                selected = draft.countMode == ReportIndicatorEntity.COUNT_VISITS,
                onClick = { draft.countMode = ReportIndicatorEntity.COUNT_VISITS },
                label = { Text("Visits (every entry)") }
            )
        }

        Text("Which patients?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.scope == ReportIndicatorEntity.SCOPE_PERIOD,
                onClick = { draft.scope = ReportIndicatorEntity.SCOPE_PERIOD },
                label = { Text("Seen in the report dates") }
            )
            FilterChip(
                selected = draft.scope == ReportIndicatorEntity.SCOPE_CUMULATIVE,
                onClick = { draft.scope = ReportIndicatorEntity.SCOPE_CUMULATIVE },
                label = { Text("Everyone up to the end date") }
            )
        }

        Text("Count only when", style = MaterialTheme.typography.titleMedium)
        if (draft.clauses.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !draft.matchAny, onClick = { draft.matchAny = false }, label = { Text("All of these are true") })
                FilterChip(selected = draft.matchAny, onClick = { draft.matchAny = true }, label = { Text("Any one of these is true") })
            }
        }
        if (draft.clauses.isEmpty()) {
            Text(
                "No conditions yet, so every patient in the session is counted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        draft.clauses.forEachIndexed { i, c ->
            ClauseEditor(c, variables, codeOptions) { draft.clauses.removeAt(i) }
        }
        OutlinedButton(onClick = { draft.clauses.add(DraftClause()) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add a condition")
        }

        if (hasAgeColumn) {
            OutlinedTextField(
                value = draft.ageBands,
                onValueChange = { draft.ageBands = it },
                label = { Text("Split by age (optional)") },
                placeholder = { Text("e.g. 0-5, 6-9, 10-19, 20-35, 36+") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save line") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClauseEditor(
    clause: DraftClause,
    variables: List<VariableDefinitionEntity>,
    codeOptions: Map<Long, List<CodeOptionEntity>>,
    onDelete: () -> Unit
) {
    val variable = variables.firstOrNull { it.name.equals(clause.column, true) }

    fun toggle(code: String) {
        val idx = clause.values.indexOfFirst { it.equals(code, true) }
        if (idx >= 0) clause.values.removeAt(idx) else clause.values.add(code)
        clause.op = OP_ONE_OF
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Condition", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove this condition") }
            }
            PickerField("Column", variables.map { it.name }, clause.column) { picked ->
                if (!picked.equals(clause.column, true)) {
                    clause.column = picked
                    clause.values.clear()
                    clause.op = when (variables.firstOrNull { it.name == picked }?.type) {
                        VariableType.CODED, VariableType.VISIT_TYPE -> OP_ONE_OF
                        VariableType.BOOLEAN -> OP_YES
                        VariableType.NUMBER -> OP_AT_LEAST
                        else -> OP_FILLED
                    }
                }
            }
            if (variable != null) {
                when (variable.type) {
                    VariableType.CODED -> {
                        Text("Key: tick every code that counts", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            codeOptions[variable.id].orEmpty().forEach { o ->
                                FilterChip(
                                    selected = clause.values.any { it.equals(o.code, true) },
                                    onClick = { toggle(o.code) },
                                    label = { Text("${o.code} – ${o.label}") }
                                )
                            }
                        }
                    }
                    VariableType.VISIT_TYPE -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = clause.values.any { it == "NEW" }, onClick = { toggle("NEW") }, label = { Text("First visit") })
                            FilterChip(selected = clause.values.any { it == "RETURN" }, onClick = { toggle("RETURN") }, label = { Text("Revisit") })
                        }
                    }
                    VariableType.BOOLEAN -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = clause.op == OP_YES, onClick = { clause.op = OP_YES }, label = { Text("Is Yes") })
                            FilterChip(selected = clause.op == OP_NO, onClick = { clause.op = OP_NO }, label = { Text("Is No") })
                        }
                    }
                    VariableType.NUMBER -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = clause.op == OP_AT_LEAST, onClick = { clause.op = OP_AT_LEAST }, label = { Text("At least") })
                            FilterChip(selected = clause.op == OP_AT_MOST, onClick = { clause.op = OP_AT_MOST }, label = { Text("At most") })
                            FilterChip(selected = clause.op == OP_BETWEEN, onClick = { clause.op = OP_BETWEEN }, label = { Text("Between") })
                        }
                        OutlinedTextField(
                            value = clause.values.getOrNull(0) ?: "",
                            onValueChange = { clause.setAt(0, it) },
                            label = { Text(if (clause.op == OP_BETWEEN) "From" else "Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (clause.op == OP_BETWEEN) {
                            OutlinedTextField(
                                value = clause.values.getOrNull(1) ?: "",
                                onValueChange = { clause.setAt(1, it) },
                                label = { Text("To") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = clause.op == OP_FILLED, onClick = { clause.op = OP_FILLED; clause.values.clear() }, label = { Text("Is filled in") })
                            FilterChip(selected = clause.op == OP_ONE_OF, onClick = { clause.op = OP_ONE_OF }, label = { Text("Is exactly") })
                        }
                        if (clause.op == OP_ONE_OF) {
                            OutlinedTextField(
                                value = clause.values.getOrNull(0) ?: "",
                                onValueChange = { clause.setAt(0, it) },
                                label = { Text("Value") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
