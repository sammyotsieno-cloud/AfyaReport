package ke.co.afyareport.ui.register

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val VISIT_DATE_TARGET = -1L
private val zone: ZoneId = ZoneId.systemDefault()
private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun startOf(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

private fun prettyDate(s: String): String =
    try { LocalDate.parse(s).format(dateFormat) } catch (e: Exception) { s }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = state) }
}

/** Add a patient to the register, or edit an existing row (entryId = 0 means a new patient). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientEntryScreen(
    registerId: Long,
    entryId: Long,
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AfyaReportDatabase.getInstance(context) }
    val reportRepo = remember { ReportRepository(db.reportDao()) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val today = remember { LocalDate.now() }

    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var latest by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var options by remember { mutableStateOf<Map<Long, List<CodeOptionEntity>>>(emptyMap()) }
    var entries by remember { mutableStateOf<List<PatientEntryEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<PatientEntryEntity?>(null) }

    var date by remember { mutableStateOf(today) }
    var patientKey by remember { mutableStateOf("") }
    var override by remember { mutableStateOf(false) }
    var manualType by remember { mutableStateOf(VisitType.NEW) }
    val values = remember { mutableStateMapOf<Long, String>() }
    var showErrors by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    var dateTarget by remember { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(registerId, entryId) {
        val reg = registerRepository.getRegister(registerId) ?: return@LaunchedEffect
        register = reg
        val vars = registerRepository.getVariablesForVersion(reg.id, reg.version)
        latest = vars
        options = vars.filter { it.type == VariableType.CODED }
            .associate { it.id to registerRepository.getCodeOptions(it.id) }
        entries = reportRepo.getEntriesForRegister(reg.id)
        if (entryId != 0L) {
            val e = entries.firstOrNull { it.id == entryId }
            editing = e
            if (e != null) {
                date = Instant.ofEpochMilli(e.visitDate).atZone(zone).toLocalDate()
                patientKey = e.patientKey.orEmpty()
                override = e.visitTypeLocked
                manualType = e.visitType
                val nameById = reportRepo.getAllVariables(reg.id).associate { it.id to it.name.trim().lowercase() }
                val byName = sessionRepository.getValuesForEntry(e.id)
                    .mapNotNull { v -> nameById[v.variableDefinitionId]?.let { it to v.value } }
                    .toMap()
                vars.forEach { col ->
                    val v = byName[col.name.trim().lowercase()]
                    if (v != null) values[col.id] = v
                }
            }
        }
    }

    val fields = latest.filter { it.type != VariableType.VISIT_TYPE }
    val keyNorm = patientKey.trim().lowercase()
    val dateMillis = startOf(date)
    val earlier = if (keyNorm.isEmpty()) 0 else entries.count {
        it.id != (editing?.id ?: -1L) &&
            it.patientKey?.trim()?.lowercase() == keyNorm &&
            it.visitDate <= dateMillis
    }
    val autoType = if (earlier > 0) VisitType.RETURN else VisitType.NEW
    val finalType = if (override) manualType else autoType
    val missing = fields.filter { it.isRequired && values[it.id].isNullOrBlank() }

    fun save(addNext: Boolean) {
        if (saving) return
        if (missing.isNotEmpty()) {
            showErrors = true
            return
        }
        val reg = register ?: return
        saving = true
        scope.launch {
            val visitVar = latest.firstOrNull { it.type == VariableType.VISIT_TYPE }
            val list = values
                .filter { (id, v) -> v.isNotBlank() && fields.any { it.id == id } }
                .map { (id, v) -> VariableValueEntity(patientEntryId = 0, variableDefinitionId = id, value = v.trim()) }
                .toMutableList()
            if (visitVar != null) {
                list.add(VariableValueEntity(patientEntryId = 0, variableDefinitionId = visitVar.id, value = finalType.name))
            }
            val key = patientKey.trim().ifBlank { null }
            val e = editing
            if (e != null) {
                db.reportingSessionDao().updateFullEntry(
                    e.copy(
                        visitType = finalType,
                        patientKey = key,
                        visitDate = dateMillis,
                        visitTypeLocked = override,
                        updatedAt = System.currentTimeMillis()
                    ),
                    list
                )
            } else {
                val sessions = sessionRepository.observeSessionsForRegister(reg.id).first()
                val sid = sessions.firstOrNull { it.registerVersion == reg.version }?.id
                    ?: sessionRepository.createSession(reg.id, reg.version, "Register entries")
                db.reportingSessionDao().insertFullEntry(
                    PatientEntryEntity(
                        sessionId = sid,
                        visitType = finalType,
                        patientKey = key,
                        sortOrder = entries.size,
                        visitDate = dateMillis,
                        visitTypeLocked = override
                    ),
                    list
                )
            }
            saving = false
            if (addNext && e == null) {
                entries = reportRepo.getEntriesForRegister(reg.id)
                savedNote = "Saved ${key ?: "patient"} (${if (finalType == VisitType.NEW) "first visit" else "revisit"})"
                patientKey = ""
                values.clear()
                override = false
                showErrors = false
                scroll.scrollTo(0)
            } else {
                onBack()
            }
        }
    }

    val target = dateTarget
    if (target != null) {
        DatePickDialog(
            initial = if (target == VISIT_DATE_TARGET) date
            else values[target]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today,
            onPick = { d ->
                if (target == VISIT_DATE_TARGET) {
                    date = d
                } else {
                    values[target] = d.toString()
                }
            },
            onDismiss = { dateTarget = null }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("It will be removed from the register and from every report.") },
            confirmButton = {
                TextButton(onClick = {
                    val e = editing
                    scope.launch {
                        if (e != null) sessionRepository.deletePatientEntry(e.id)
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
            AfyaTopBar(
                title = if (editing != null) "Edit entry" else "Add patient",
                onBack = onBack,
                actions = {
                    if (editing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            savedNote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp)
                        .then(Modifier.padding(0.dp))
                )
            }

            OutlinedButton(onClick = { dateTarget = VISIT_DATE_TARGET }, modifier = Modifier.fillMaxWidth()) {
                Text("Visit date: ${date.format(dateFormat)}")
            }

            OutlinedTextField(
                value = patientKey,
                onValueChange = { patientKey = it; savedNote = null },
                label = { Text("Patient number or initials") },
                placeholder = { Text("e.g. 0123 or J.K.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            if (keyNorm.isEmpty()) {
                Text(
                    "Without a patient number, every row counts as a different patient.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("First visit or revisit", style = MaterialTheme.typography.titleSmall)
                    if (!override) {
                        Text(
                            if (autoType == VisitType.NEW) "First visit: no earlier entry has this patient number."
                            else "Revisit: this patient number is already in the register ($earlier earlier ${if (earlier == 1) "entry" else "entries"}).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = manualType == VisitType.NEW,
                                onClick = { manualType = VisitType.NEW },
                                label = { Text("First visit") }
                            )
                            FilterChip(
                                selected = manualType == VisitType.RETURN,
                                onClick = { manualType = VisitType.RETURN },
                                label = { Text("Revisit") }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set it by hand", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = override,
                            onCheckedChange = {
                                override = it
                                if (it) manualType = autoType
                            }
                        )
                    }
                }
            }

            fields.forEach { col ->
                val err = showErrors && col.isRequired && values[col.id].isNullOrBlank()
                val title = col.name + if (col.isRequired) " *" else ""
                when (col.type) {
                    VariableType.CODED -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (err) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (col.measurementInstructions.isNotBlank()) {
                                Text(
                                    col.measurementInstructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options[col.id].orEmpty().forEach { o ->
                                    FilterChip(
                                        selected = values[col.id] == o.code,
                                        onClick = {
                                            if (values[col.id] == o.code) {
                                                values.remove(col.id)
                                            } else {
                                                values[col.id] = o.code
                                            }
                                        },
                                        label = { Text("${o.code} – ${o.label}") }
                                    )
                                }
                            }
                            if (err) {
                                Text("Required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    VariableType.BOOLEAN -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (err) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = values[col.id] == "true",
                                    onClick = { values[col.id] = "true" },
                                    label = { Text("Yes") }
                                )
                                FilterChip(
                                    selected = values[col.id] == "false",
                                    onClick = { values[col.id] = "false" },
                                    label = { Text("No") }
                                )
                            }
                            if (err) {
                                Text("Required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    VariableType.DATE -> {
                        OutlinedButton(onClick = { dateTarget = col.id }, modifier = Modifier.fillMaxWidth()) {
                            val v = values[col.id]
                            Text(if (v.isNullOrBlank()) "$title: pick a date" else "${col.name}: ${prettyDate(v)}")
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = values[col.id] ?: "",
                            onValueChange = { values[col.id] = it; savedNote = null },
                            label = { Text(title) },
                            supportingText = {
                                if (err) {
                                    Text("Required")
                                } else if (col.measurementInstructions.isNotBlank()) {
                                    Text(col.measurementInstructions)
                                }
                            },
                            isError = err,
                            singleLine = col.type != VariableType.TEXT,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (col.type == VariableType.NUMBER) KeyboardType.Decimal else KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (editing == null) {
                Button(onClick = { save(true) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Save and add next")
                }
                OutlinedButton(onClick = { save(false) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Save and close")
                }
            } else {
                Button(onClick = { save(false) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Save changes")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
