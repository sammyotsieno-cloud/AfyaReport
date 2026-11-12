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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import kotlinx.coroutines.launch

/**
 * Minimal create-register screen.
 * For the first version we only collect name + description.
 * Variable editing UI will be added in a later batch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRegisterScreen(
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onRegisterCreated: (Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
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
                label = { Text("Register name") },
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

            Text(
                text = "You will be able to add variables (including coded fields and Visit Type) in the next step.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        // For now we create a register with one default VISIT_TYPE variable
                        // so that New/Return is always available.
                        val visitTypeVariable = VariableDefinitionEntity(
                            registerDefinitionId = 0, // will be set by the transaction
                            name = "Visit Type",
                            type = VariableType.VISIT_TYPE,
                            measurementInstructions = "Select whether this is a new patient visit or a return visit",
                            isRequired = true,
                            sortOrder = 0
                        )
                        val id = registerRepository.createRegister(
                            name = name,
                            description = description,
                            variablesWithOptions = listOf(
                                visitTypeVariable to emptyList<CodeOptionEntity>()
                            )
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
