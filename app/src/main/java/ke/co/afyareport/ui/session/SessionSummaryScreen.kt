package ke.co.afyareport.ui.session

import android.content.Intent
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf<List<PatientEntryEntity>>(emptyList()) }
    var variables by remember { mutableStateOf<List<VariableDefinitionEntity>>(emptyList()) }
    var codeOptions by remember { mutableStateOf<Map<Long, List<CodeOptionEntity>>>(emptyMap()) }
    var allValues by remember { mutableStateOf<List<VariableValueEntity>>(emptyList()) }
    var registerName by remember { mutableStateOf("") }
    var sessionLabel by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        val currentSession = sessionRepository.getSession(sessionId) ?: return@LaunchedEffect
        sessionLabel = currentSession.customLabel
        registerName = registerRepository.getRegister(currentSession.registerDefinitionId)?.name.orEmpty()
        entries = sessionRepository.getEntries(sessionId)
        variables = registerRepository.getVariablesForVersion(
            currentSession.registerDefinitionId,
            currentSession.registerVersion
        )

        val optionsMap = mutableMapOf<Long, List<CodeOptionEntity>>()
        variables.filter { it.type == VariableType.CODED }.forEach { variable ->
            optionsMap[variable.id] = registerRepository.getCodeOptions(variable.id)
        }
        codeOptions = optionsMap

        val values = mutableListOf<VariableValueEntity>()
        entries.forEach { entry ->
            values.addAll(sessionRepository.getValuesForEntry(entry.id))
        }
        allValues = values
    }

    val newCount = entries.count { it.visitType == VisitType.NEW }
    val returnCount = entries.count { it.visitType == VisitType.RETURN }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val newCount = entries.count { it.visitType == VisitType.NEW }
                        val returnCount = entries.count { it.visitType == VisitType.RETURN }
                        val text = buildString {
                            appendLine("AfyaReport – Session Summary")
                            appendLine("Register: $registerName")
                            appendLine("Label: ${sessionLabel.ifBlank { "Untitled" }}")
                            appendLine("Total entries: ${entries.size}")
                            appendLine()
                            appendLine("Visit Type")
                            appendLine("  NEW: $newCount")
                            appendLine("  RETURN: $returnCount")
                            appendLine()
                            variables.filter { it.type == VariableType.CODED }.forEach { variable ->
                                appendLine(variable.name)
                                codeOptions[variable.id].orEmpty().forEach { option ->
                                    val count = allValues.count {
                                        it.variableDefinitionId == variable.id && it.value == option.code
                                    }
                                    appendLine("  ${option.code} – ${option.label}: $count")
                                }
                                appendLine()
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share session summary"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share summary")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val currentSession = sessionRepository.getSession(sessionId) ?: return@launch
                            val currentEntries = sessionRepository.getEntries(sessionId)
                            val currentVariables = registerRepository.getVariablesForVersion(
                                currentSession.registerDefinitionId,
                                currentSession.registerVersion
                            )
                            val optionsMap = currentVariables
                                .filter { it.type == VariableType.CODED }
                                .associate { variable ->
                                    variable.id to registerRepository.getCodeOptions(variable.id)
                                }
                            val valuesByEntry = currentEntries.associate { entry ->
                                entry.id to sessionRepository.getValuesForEntry(entry.id)
                            }
                            val csv = buildSessionCsv(currentEntries, currentVariables, optionsMap, valuesByEntry)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export CSV"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Visit Type", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("NEW: $newCount", style = MaterialTheme.typography.bodyLarge)
                    Text("RETURN: $returnCount", style = MaterialTheme.typography.bodyLarge)
                    Text("Total entries: ${entries.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            variables.filter { it.type == VariableType.CODED }.forEach { variable ->
                val options = codeOptions[variable.id].orEmpty()
                val counts = options.associateWith { option ->
                    allValues.count {
                        it.variableDefinitionId == variable.id && it.value == option.code
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(variable.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        counts.forEach { (option, count) ->
                            Text(
                                text = "${option.code} – ${option.label}: $count",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (variables.none { it.type == VariableType.CODED }) {
                Text(
                    text = "No coded variables in this register to summarise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
