package ke.co.afyareport.ui.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.buildFacts
import ke.co.afyareport.domain.patientKey
import ke.co.afyareport.domain.suggestPatients
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.common.NumberField
import ke.co.afyareport.ui.common.SingleChoice
import ke.co.afyareport.ui.common.SuggestionPanel
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Button
import ke.co.afyareport.ui.theme.OutlinedButton
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Adds a patient to the list without a visit. Warns about numbers or people already in the system. */
@Composable
fun AddPatientScreen(repo: Repo, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    var no by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val today = remember { LocalDate.now() }
    val facts = remember(patients, visits) { buildFacts(patients, visits) }
    val key = patientKey(no)
    val exists = patients.firstOrNull { it.patientKey == key && key.isNotEmpty() }
    val ageNum = age.trim().toIntOrNull()
    val suggestions = remember(facts, no, name, ageNum, sex) {
        suggestPatients(facts, no, name, ageNum, sex, today)
    }.filter { it.facts.patient.id !in dismissed }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Add a patient", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Enrol a patient now and add their visits later. The patient joins the list by number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error?.let { Notice(it, warning = true) }

            OutlinedTextField(
                value = no,
                onValueChange = { no = it; error = null },
                label = { Text("Patient number *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            if (exists != null) {
                Notice(
                    "Number ${exists.patientNo} is already enrolled" +
                        (if (exists.name.isNotBlank()) " as ${exists.name}" else "") + ".",
                    warning = true
                )
                OutlinedButton(onClick = { onSaved(exists.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open that patient")
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            SingleChoice("Sex", Keys.sex, sex, { sex = it })
            NumberField("Age (years)", age, { age = it }, decimal = false)

            SuggestionPanel(
                suggestions = suggestions,
                onPick = { onSaved(it.facts.patient.id) },
                onDismiss = { dismissed = dismissed + it.facts.patient.id },
                pickLabel = "Open this patient"
            )

            Button(
                onClick = {
                    if (saving) return@Button
                    when {
                        key.isEmpty() -> error = "Enter the patient number."
                        exists != null -> error = "That number is already enrolled."
                        age.isNotBlank() && ageNum?.let { it in 0..120 } != true -> error = "Age must be between 0 and 120."
                        else -> {
                            saving = true
                            scope.launch {
                                val (p, _) = repo.registerPatient(no, name, sex, ageNum)
                                saving = false
                                onSaved(p.id)
                            }
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save patient") }
        }
    }
}
