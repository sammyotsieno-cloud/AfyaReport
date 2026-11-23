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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import kotlinx.coroutines.launch

/** Every field is observable state, so typing redraws the screen immediately. */
class DraftCodeOption(code: String = "", label: String = "") {
    var code by mutableStateOf(code)
    var label by mutableStateOf(label)
}

class DraftVariable(
    name: String = "",
    type: VariableType = VariableType.TEXT,
    measurementInstructions: String = "",
    isRequired: Boolean = false,
    val options: MutableList<DraftCodeOption> = mutableStateListOf()
) {
    var name by mutableStateOf(name)
    var type by mutableStateOf(type)
    var measurementInstructions by mutableStateOf(measurementInstructions)
    var isRequired by mutableStateOf(isRequired)
}

internal fun VariableType.friendlyName(): String = when (this) {
    VariableType.TEXT -> "Words"
    VariableType.NUMBER -> "Number"
    VariableType.DATE -> "Date"
    VariableType.BOOLEAN -> "Yes / No"
    VariableType.CODED -> "Uses a key (codes)"
    VariableType.VISIT_TYPE -> "First visit or revisit"
}

internal fun DraftVariable.hasNoValidChoices(): Boolean =
    type == VariableType.CODED &&
        options.none { it.code.isNotBlank() && it.label.isNotBlank() }

@Composable
fun CreateRegisterScreen(
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onRegisterCreated: (Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val variables = remember {
        mutableStateListOf(
            DraftVariable(
                name = "Visit Type",
                type = VariableType.VISIT_TYPE,
                measurementInstructions = "Select whether this is a new patient visit or a return visit",
                isRequired = true
            )
        )
    }
    var isSaving by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "New register", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Register name") },
                placeholder = { Text("e.g. Diabetes & Hypertension Daily Register") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text("Columns", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add one column for each thing you write down for every patient, in the same order as your paper register.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            variables.forEachIndexed { index, draft ->
                VariableEditorCard(
                    draft = draft,
                    canDelete = draft.type != VariableType.VISIT_TYPE,
                    onDelete = { variables.removeAt(index) },
                    showErrors = showErrors
                )
            }

            OutlinedButton(
                onClick = { variables.add(DraftVariable()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a column")
            }

            Button(
                onClick = {
                    if (name.isBlank() || isSaving) return@Button
                    if (variables.any { it.hasNoValidChoices() }) {
                        showErrors = true
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        val prepared = variables.map { draft ->
                            val entity = VariableDefinitionEntity(
                                registerDefinitionId = 0,
                                registerVersion = 1,
                                name = draft.name.trim().ifBlank { "Untitled" },
                                type = draft.type,
                                measurementInstructions = draft.measurementInstructions.trim(),
                                isRequired = draft.isRequired
                            )
                            val options = if (draft.type == VariableType.CODED) {
                                draft.options
                                    .filter { it.code.isNotBlank() && it.label.isNotBlank() }
                                    .map {
                                        CodeOptionEntity(
                                            variableDefinitionId = 0,
                                            code = it.code.trim(),
                                            label = it.label.trim()
                                        )
                                    }
                            } else emptyList()
                            entity to options
                        }
                        val id = registerRepository.createRegister(
                            name = name,
                            description = description,
                            variablesWithOptions = prepared
                        )
                        isSaving = false
                        onRegisterCreated(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Save register")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VariableEditorCard(
    draft: DraftVariable,
    canDelete: Boolean,
    onDelete: () -> Unit,
    showErrors: Boolean = false
) {
    if (draft.type == VariableType.VISIT_TYPE) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("First visit or revisit", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Added automatically, so patients who come back are counted as revisits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.name.ifBlank { "New column" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove this column")
                    }
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft.name = it },
                label = { Text("Column name") },
                placeholder = { Text("e.g. Weight (kg), Diagnosis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("What do you write in this column?", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    VariableType.TEXT,
                    VariableType.NUMBER,
                    VariableType.DATE,
                    VariableType.BOOLEAN,
                    VariableType.CODED
                ).forEach { kind ->
                    FilterChip(
                        selected = draft.type == kind,
                        onClick = {
                            draft.type = kind
                            if (kind == VariableType.CODED && draft.options.isEmpty()) {
                                draft.options.add(DraftCodeOption())
                            }
                        },
                        label = { Text(kind.friendlyName()) }
                    )
                }
            }

            if (draft.type == VariableType.CODED) {
                Text(
                    "The key lists each code your register uses and what it means, for example a = Newly diagnosed. When entering patients you pick a code, and the report reads it as its meaning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Key", style = MaterialTheme.typography.labelLarge)
                draft.options.forEachIndexed { optIndex, opt ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = opt.code,
                            onValueChange = { opt.code = it },
                            label = { Text("Code") },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = opt.label,
                            onValueChange = { opt.label = it },
                            label = { Text("What it means") },
                            modifier = Modifier.weight(0.65f),
                            singleLine = true
                        )
                        IconButton(onClick = { draft.options.removeAt(optIndex) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove this choice")
                        }
                    }
                }
                if (showErrors && draft.hasNoValidChoices()) {
                    Text(
                        "Add at least one code to the key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedButton(
                    onClick = { draft.options.add(DraftCodeOption()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add a code to the key")
                }
            }

            OutlinedTextField(
                value = draft.measurementInstructions,
                onValueChange = { draft.measurementInstructions = it },
                label = { Text("Hint for whoever enters the data (optional)") },
                placeholder = { Text("e.g. Measure in kilograms") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Must be filled in for every patient",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.isRequired,
                    onCheckedChange = { draft.isRequired = it }
                )
            }
        }
    }
}
