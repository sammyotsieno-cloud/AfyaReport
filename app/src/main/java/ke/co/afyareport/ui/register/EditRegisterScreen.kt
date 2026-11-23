package ke.co.afyareport.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import ke.co.afyareport.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRegisterScreen(
    registerId: Long,
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val variables = remember { mutableStateListOf<DraftVariable>() }
    var isSaving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(registerId) {
        val reg = registerRepository.getRegister(registerId)
        if (reg == null) {
            loadError = "Register not found."
            loaded = true
            return@LaunchedEffect
        }
        register = reg
        name = reg.name
        description = reg.description
        val vars = registerRepository.getLatestVariables(registerId)
        variables.clear()
        vars.forEach { variable ->
            val draft = DraftVariable(
                name = variable.name,
                type = variable.type,
                measurementInstructions = variable.measurementInstructions,
                isRequired = variable.isRequired
            )
            if (variable.type == VariableType.CODED) {
                draft.options.addAll(
                    registerRepository.getCodeOptions(variable.id).map {
                        DraftCodeOption(code = it.code, label = it.label)
                    }
                )
            }
            variables.add(draft)
        }
        if (variables.none { it.type == VariableType.VISIT_TYPE }) {
            variables.add(
                0,
                DraftVariable(
                    name = "Visit Type",
                    type = VariableType.VISIT_TYPE,
                    measurementInstructions = "Select whether this is a new patient visit or a return visit",
                    isRequired = true
                )
            )
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Register") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) { Text("Loading…") }
            return@Scaffold
        }

        if (loadError != null || register == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                Text(loadError ?: "Unable to load register.")
                Button(onClick = onBack) { Text("Back") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Saving creates version ${(register?.version ?: 1) + 1}. Existing sessions keep their previous version and variables.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    val currentRegister = register ?: return@Button
                    if (name.isBlank() || isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            val prepared = variables.map { draft ->
                                val entity = VariableDefinitionEntity(
                                    registerDefinitionId = currentRegister.id,
                                    registerVersion = currentRegister.version + 1,
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
                            registerRepository.createNewVersion(
                                register = currentRegister.copy(
                                    name = name.trim(),
                                    description = description.trim()
                                ),
                                variablesWithOptions = prepared
                            )
                            onSaved()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Save as new version")
            }
        }
    }
}
