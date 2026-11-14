package ke.co.afyareport.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import kotlinx.coroutines.launch

data class DraftCodeOption(
    var code: String = "",
    var label: String = ""
)

data class DraftVariable(
    var name: String = "",
    var type: VariableType = VariableType.TEXT,
    var measurementInstructions: String = "",
    var isRequired: Boolean = false,
    val options: MutableList<DraftCodeOption> = mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Register") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Register name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Text("Variables", style = MaterialTheme.typography.titleMedium)

            variables.forEachIndexed { index, draft ->
                VariableEditorCard(
                    draft = draft,
                    canDelete = draft.type != VariableType.VISIT_TYPE,
                    onDelete = { variables.removeAt(index) }
                )
            }

            Button(
                onClick = { variables.add(DraftVariable()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Add variable")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isBlank() || isSaving) return@Button
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
                Text(if (isSaving) "Saving…" else "Create Register")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariableEditorCard(
    draft: DraftVariable,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (draft.type == VariableType.VISIT_TYPE) "Visit Type (system)" else "Variable",
                    style = MaterialTheme.typography.titleSmall
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove variable")
                    }
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft.name = it },
                label = { Text("Variable name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = draft.type != VariableType.VISIT_TYPE
            )

            if (draft.type != VariableType.VISIT_TYPE) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = draft.type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        VariableType.entries
                            .filter { it != VariableType.VISIT_TYPE }
                            .forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        draft.type = type
                                        typeExpanded = false
                                    }
                                )
                            }
                    }
                }
            }

            OutlinedTextField(
                value = draft.measurementInstructions,
                onValueChange = { draft.measurementInstructions = it },
                label = { Text("Measurement instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = draft.isRequired,
                    onCheckedChange = { draft.isRequired = it },
                    enabled = draft.type != VariableType.VISIT_TYPE
                )
                Text("Required")
            }

            if (draft.type == VariableType.CODED) {
                Text("Code options", style = MaterialTheme.typography.labelLarge)
                draft.options.forEachIndexed { optIndex, opt ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = opt.code,
                            onValueChange = { opt.code = it },
                            label = { Text("Code") },
                            modifier = Modifier.weight(0.3f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = opt.label,
                            onValueChange = { opt.label = it },
                            label = { Text("Label") },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                        IconButton(onClick = { draft.options.removeAt(optIndex) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove option")
                        }
                    }
                }
                Button(
                    onClick = { draft.options.add(DraftCodeOption()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add code option")
                }
            }
        }
    }
}
