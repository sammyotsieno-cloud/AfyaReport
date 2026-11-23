package ke.co.afyareport.ui.register
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import ke.co.afyareport.ui.theme.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import ke.co.afyareport.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDetailScreen(
    registerId: Long,
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    var register by remember { mutableStateOf<RegisterDefinitionEntity?>(null) }
    var variables by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var codeOptions by remember { mutableStateOf<Map<Long, List<CodeOptionEntity>>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(registerId) {
        val currentRegister = registerRepository.getRegister(registerId)
        register = currentRegister
        val currentVariables = if (currentRegister == null) {
            emptyList()
        } else {
            registerRepository.getLatestVariables(registerId)
        }
        variables = currentVariables
        val options = mutableMapOf<Long, List<CodeOptionEntity>>()
        currentVariables.filter { it.type == VariableType.CODED }.forEach { variable ->
            options[variable.id] = registerRepository.getCodeOptions(variable.id)
        }
        codeOptions = options
        loaded = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(register?.name ?: "Register") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (register != null) {
                        IconButton(onClick = { onEdit(registerId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit register")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!loaded) {
                Text("Loading…")
            } else if (register == null) {
                Text("Register not found.")
                Button(onClick = onBack) { Text("Back") }
            } else {
                register?.let { current ->
                    Text("Version ${current.version}", style = MaterialTheme.typography.labelLarge)
                    if (current.description.isNotBlank()) {
                        Text(current.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Variables (current version)", style = MaterialTheme.typography.titleMedium)
                variables.forEach { variable ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = variable.name + if (variable.isRequired) " *" else "",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = variable.type.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (variable.measurementInstructions.isNotBlank()) {
                                Text(
                                    text = variable.measurementInstructions,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (variable.type == VariableType.CODED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                codeOptions[variable.id].orEmpty().forEach { option ->
                                    Text(
                                        text = "  ${option.code} – ${option.label}",
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
