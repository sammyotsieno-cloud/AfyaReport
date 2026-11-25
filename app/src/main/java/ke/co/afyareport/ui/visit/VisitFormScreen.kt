@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package ke.co.afyareport.ui.visit

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
import androidx.compose.material3.AssistChip
import ke.co.afyareport.ui.theme.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import ke.co.afyareport.ui.theme.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.DraftStore
import ke.co.afyareport.domain.FormField
import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.RegisterPrefs
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.bmiBand
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.labelOf
import ke.co.afyareport.domain.patientKey
import ke.co.afyareport.domain.similarPatients
import ke.co.afyareport.ui.common.DatePickDialog
import ke.co.afyareport.ui.common.MultiChoice
import ke.co.afyareport.ui.common.NumberField
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.theme.SectionHeader
import ke.co.afyareport.ui.common.SingleChoice
import ke.co.afyareport.ui.theme.AfyaTopBar
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun toggle(list: MutableList<String>, code: String) {
    if (!list.remove(code)) list.add(code)
}

/** Add a visit, or edit one (entryId = 0 means a new visit). */
@Composable
fun VisitFormScreen(
    entryId: Long,
    startPatientNo: String,
    repo: Repo,
    prefs: RegisterPrefs,
    drafts: DraftStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val form = remember { VisitForm() }
    val required = remember { prefs.required() }
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())

    var existing by remember { mutableStateOf<VisitEntity?>(null) }
    var visitNoTouched by remember { mutableStateOf(entryId != 0L) }
    var prefilledFor by remember { mutableStateOf(-1L) }
    var errors by remember { mutableStateOf<Map<FormField, String>>(emptyMap()) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showComp by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(entryId != 0L) }

    LaunchedEffect(entryId) {
        if (entryId != 0L) {
            val v = repo.dao.getVisit(entryId)
            if (v != null) {
                existing = v
                form.load(v, repo.dao.getPatient(v.patientId))
                if (form.complications.isNotEmpty()) showComp = true
            }
        } else {
            prefs.lastDate()?.let { form.date = LocalDate.ofEpochDay(it) }
            val draft = drafts.loadJson()
            if (draft != null) {
                runCatching { form.fromJson(draft) }
                restored = form.hasContent()
                visitNoTouched = form.visitNo.isNotBlank()
                if (form.complications.isNotEmpty()) showComp = true
            }
            if (startPatientNo.isNotBlank() && form.patientNo.isBlank()) form.patientNo = startPatientNo
            ready = true
        }
    }

    LaunchedEffect(ready) {
        if (entryId == 0L && ready) {
            snapshotFlow { if (form.hasContent()) form.toJson() else null }.collect { json ->
                if (json == null) drafts.clear() else drafts.save(json)
            }
        }
    }

    val key = patientKey(form.patientNo)
    val matched = patients.firstOrNull { it.patientKey == key && key.isNotEmpty() }
    val others = visits.filter { v -> matched != null && v.patientId == matched.id && v.id != (existing?.id ?: -1L) }
    val sameDay = others.any { it.visitDate == form.date.toEpochDay() }
    val monthCount = others.count {
        val d = it.visitDate.asDate()
        d.year == form.date.year && d.month == form.date.month
    }
    val similar = if (matched == null) similarPatients(key, patients) else emptyList()

    LaunchedEffect(key, monthCount, form.date) {
        if (!visitNoTouched && key.isNotEmpty()) form.visitNo = (monthCount + 1).toString()
    }

    LaunchedEffect(matched?.id, visits.size) {
        val m = matched
        if (existing == null && m != null && prefilledFor != m.id) {
            val last = visits.filter { it.patientId == m.id }
                .maxWithOrNull(compareBy<VisitEntity>({ it.visitDate }, { it.id }))
            if (last != null) {
                prefilledFor = m.id
                if (form.sex.isBlank()) form.sex = last.sex
                if (form.age.isBlank()) form.age = last.age?.toString() ?: ""
                if (form.condition.isBlank()) form.condition = last.condition
                if (form.diagnosis.isBlank()) form.diagnosis = last.diagnosis
                if (form.status.isBlank() && last.condition.isNotBlank()) form.status = "b"
                if (form.nhif.isBlank()) form.nhif = last.nhif
            }
        }
    }

    fun star(f: FormField) = if (f in required) " *" else ""

    fun save(addNext: Boolean) {
        if (saving) return
        val found = form.validate(required)
        errors = found
        if (found.isNotEmpty()) {
            scope.launch { scroll.animateScrollTo(0) }
            return
        }
        saving = true
        scope.launch {
            val base = existing
            val entity = form.toEntity(base)
            val label = form.patientNo.trim()
            val wasEnrolled = matched != null
            if (base == null) {
                repo.addVisit(form.patientNo, form.name, entity)
            } else {
                repo.changeVisit(form.patientNo, form.name, entity)
            }
            prefs.setLastDate(form.date.toEpochDay())
            drafts.clear()
            saving = false
            if (addNext && base == null) {
                form.reset()
                visitNoTouched = false
                prefilledFor = -1L
                errors = emptyMap()
                showComp = false
                savedNote = "Saved $label" + if (wasEnrolled) " (revisit)" else " (new patient)"
                scroll.scrollTo(0)
            } else {
                onBack()
            }
        }
    }

    if (pickDate) {
        DatePickDialog(form.date, { form.date = it }, { pickDate = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this visit?") },
            text = { Text("It will be removed from the register and from every report.") },
            confirmButton = {
                TextButton(onClick = {
                    val v = existing
                    scope.launch {
                        if (v != null) repo.dao.deleteVisit(v.id)
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (existing == null) {
                        OutlinedButton(
                            onClick = { save(false) },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) { Text("Save and close") }
                        Button(
                            onClick = { save(true) },
                            enabled = !saving,
                            modifier = Modifier.weight(1.4f)
                        ) { Text("Save and next") }
                    } else {
                        Button(
                            onClick = { save(false) },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save changes") }
                    }
                }
            }
        },
        topBar = {
            AfyaTopBar(
                title = if (existing != null) "Edit visit" else "Add a visit",
                onBack = onBack,
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete visit")
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
            savedNote?.let { Notice(it) }
            if (restored && existing == null) {
                Notice("Your unsaved entry was restored. You can leave any time. It stays here until you save or discard it.")
                OutlinedButton(
                    onClick = {
                        form.reset()
                        visitNoTouched = false
                        prefilledFor = -1L
                        showComp = false
                        restored = false
                        drafts.clear()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Discard this entry and start over") }
            }
            if (errors.isNotEmpty()) Notice("Please fix the marked fields.", warning = true)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Visit date: ${form.date.format(dayFormat)}")
                }
                errors[FormField.DATE]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = form.patientNo,
                onValueChange = { form.patientNo = it; savedNote = null },
                label = { Text("Patient number" + star(FormField.PATIENT_NO)) },
                isError = errors.containsKey(FormField.PATIENT_NO),
                supportingText = { errors[FormField.PATIENT_NO]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            if (matched != null) {
                val last = others.maxOfOrNull { it.visitDate }
                Notice(
                    "Already enrolled" +
                        (if (matched.name.isNotBlank()) ": ${matched.name}" else "") +
                        ". ${others.size} other visit(s) recorded" +
                        (if (last != null) ", last on ${last.asDate().format(dayFormat)}" else "") + "."
                )
                if (sameDay) {
                    Notice(
                        "This patient already has a visit on ${form.date.format(dayFormat)}. Check that this is not a duplicate.",
                        warning = true
                    )
                }
            } else if (key.isNotEmpty()) {
                Notice("New patient number. The patient is enrolled automatically when you save.")
            }
            if (similar.isNotEmpty()) {
                Notice("This number is close to one already enrolled. Is it the same patient?", warning = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    similar.forEach { s ->
                        AssistChip(
                            onClick = { form.patientNo = s.patientNo },
                            label = { Text(s.patientNo + if (s.name.isNotBlank()) " – ${s.name}" else "") }
                        )
                    }
                }
            }
            if (key.isNotEmpty() && (matched == null || matched.name.isBlank())) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { form.name = it },
                    label = { Text("Name (optional, saved once in the patient list)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionHeader("Patient")
            NumberField("Age (years)" + star(FormField.AGE), form.age, { form.age = it }, errors[FormField.AGE], decimal = false)
            SingleChoice("Sex" + star(FormField.SEX), Keys.sex, form.sex, { form.sex = it }, errors[FormField.SEX])
            NumberField(
                "Visit for the month" + star(FormField.VISIT_NO), form.visitNo,
                { form.visitNo = it; visitNoTouched = true }, errors[FormField.VISIT_NO], decimal = false
            )

            SectionHeader("Measurements")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Weight (kg)" + star(FormField.WEIGHT), form.weight, { form.weight = it }, errors[FormField.WEIGHT], modifier = Modifier.weight(1f))
                NumberField("Height (cm)" + star(FormField.HEIGHT), form.height, { form.height = it }, errors[FormField.HEIGHT], modifier = Modifier.weight(1f))
            }
            form.bmi?.let { b ->
                val band = bmiBand(b) ?: ""
                Text(
                    "BMI $b  •  Key 1: $band (${Keys.bmiBands.labelOf(band)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NumberField("Waist circumference (cm)" + star(FormField.WAIST), form.waist, { form.waist = it }, errors[FormField.WAIST])
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("BP top" + star(FormField.BP), form.sys, { form.sys = it }, errors[FormField.BP]?.let { " " }, decimal = false, modifier = Modifier.weight(1f))
                    NumberField("BP bottom", form.dia, { form.dia = it }, errors[FormField.BP]?.let { " " }, decimal = false, modifier = Modifier.weight(1f))
                }
                errors[FormField.BP]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            SectionHeader("Condition")
            SingleChoice(
                "Hypertension, diabetes or both" + star(FormField.CONDITION), Keys.condition, form.condition,
                { c ->
                    form.condition = c
                    if (form.diagnosis !in Keys.diagnosisFor(c).map { it.code }) form.diagnosis = ""
                },
                errors[FormField.CONDITION]
            )
            SingleChoice(
                "Newly diagnosed or previously known" + star(FormField.STATUS), Keys.status, form.status,
                { form.status = it }, errors[FormField.STATUS], showCode = true
            )
            SingleChoice(
                "Diagnosis" + star(FormField.DIAGNOSIS), Keys.diagnosisFor(form.condition), form.diagnosis,
                { form.diagnosis = it }, errors[FormField.DIAGNOSIS], showCode = true
            )

            SectionHeader("Tests")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("RBS (mmol/L)" + star(FormField.BLOOD_SUGAR), form.rbs, { form.rbs = it }, errors[FormField.BLOOD_SUGAR]?.let { " " }, modifier = Modifier.weight(1f))
                    NumberField("FBS (mmol/L)", form.fbs, { form.fbs = it }, errors[FormField.BLOOD_SUGAR]?.let { " " }, modifier = Modifier.weight(1f))
                }
                errors[FormField.BLOOD_SUGAR]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            NumberField("HbA1c (%)" + star(FormField.HBA1C), form.hba1c, { form.hba1c = it }, errors[FormField.HBA1C])

            SectionHeader("Treatment and complications")
            MultiChoice(
                "Treatment" + star(FormField.TREATMENT), Keys.treatment, form.treatment,
                { toggle(form.treatment, it) }, errors[FormField.TREATMENT], showCode = true
            )
            OutlinedButton(onClick = { showComp = !showComp }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showComp) "Hide complications" else "Complications (${form.complications.size} ticked)")
            }
            if (showComp) {
                Keys.complicationKinds.forEach { kind ->
                    val cur = form.complications[kind.code]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            kind.label + if (kind.code in Keys.addedByApp) " (added by the app)" else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = cur == "1",
                                onClick = { form.toggleComplication(kind.code, "1") },
                                label = { Text("${kind.code}1 New") }
                            )
                            FilterChip(
                                selected = cur == "2",
                                onClick = { form.toggleComplication(kind.code, "2") },
                                label = { Text("${kind.code}2 Pre-existing") }
                            )
                        }
                    }
                }
            }

            if (form.hasDiabetes) {
                SectionHeader("Diabetic foot")
                SingleChoice(
                    "Screened for diabetic foot" + star(FormField.FOOT_SCREENING), Keys.yesNo, form.footScreened,
                    { form.footScreened = it }, errors[FormField.FOOT_SCREENING]
                )
                if (form.footScreened == "Y") {
                    SingleChoice("Diabetic foot", Keys.diabeticFoot, form.diabeticFoot, { form.diabeticFoot = it })
                }
                SingleChoice(
                    "Foot risk (Key 6)" + star(FormField.FOOT_RISK), Keys.footRisk, form.footRisk,
                    { form.footRisk = it }, errors[FormField.FOOT_RISK]
                )
                if (form.footRisk == "B") {
                    MultiChoice("Which apply", Keys.footRiskFactors, form.footFactors, { toggle(form.footFactors, it) })
                }
                if (form.diabeticFoot == "NEW" || form.diabeticFoot == "KNOWN") {
                    SingleChoice("Footcare outcome (Key 7)", Keys.footOutcome, form.footOutcome, { form.footOutcome = it }, showCode = true)
                }
            }

            SectionHeader("TB and insurance")
            SingleChoice(
                "Screened for TB" + star(FormField.TB_SCREENING), Keys.yesNo, form.tbScreened,
                { form.tbScreened = it }, errors[FormField.TB_SCREENING]
            )
            if (form.tbScreened == "Y") {
                SingleChoice("TB status after screening (Key 8)", Keys.tbStatus, form.tbStatus, { form.tbStatus = it }, showCode = true)
            }
            SingleChoice("Active NHIF" + star(FormField.NHIF), Keys.yesNo, form.nhif, { form.nhif = it }, errors[FormField.NHIF])
            OutlinedTextField(
                value = form.remarks,
                onValueChange = { form.remarks = it },
                label = { Text("Remarks (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
