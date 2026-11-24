package ke.co.afyareport.ui.report

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.engine.EntryData
import ke.co.afyareport.engine.ReportEngine
import ke.co.afyareport.engine.ReportRow
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.NumeralStyle
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private enum class Period(val label: String) {
    TODAY("Today"), WEEK("This week"), MONTH("This month"), LAST_MONTH("Last month"), CUSTOM("Custom dates")
}

private class ReportInputs(
    val indicators: List<ReportIndicatorEntity>,
    val settings: ReportSettingsEntity,
    val variables: List<VariableDefinitionEntity>,
    val options: Map<Long, List<CodeOptionEntity>>,
    val entries: List<EntryData>
)

private val zone: ZoneId = ZoneId.systemDefault()
private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun startOf(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
private fun endOf(d: LocalDate): Long = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

private fun rangeFor(p: Period, today: LocalDate): Pair<LocalDate, LocalDate> = when (p) {
    Period.TODAY -> today to today
    Period.WEEK -> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        monday to monday.plusDays(6)
    }
    Period.MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
    Period.LAST_MONTH -> {
        val first = today.withDayOfMonth(1).minusMonths(1)
        first to first.with(TemporalAdjusters.lastDayOfMonth())
    }
    Period.CUSTOM -> today.withDayOfMonth(1) to today
}

/** Opens, reads every key and rule, and computes the whole report for the chosen dates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportResultScreen(
    sessionId: Long,
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
    onSetupReport: (Long) -> Unit
) {
    val context = LocalContext.current
    val reportRepo = remember { ReportRepository(AfyaReportDatabase.getInstance(context).reportDao()) }
    val today = remember { LocalDate.now() }
    var registerId by remember { mutableStateOf(0L) }
    var registerName by remember { mutableStateOf("") }
    var inputs by remember { mutableStateOf<ReportInputs?>(null) }
    var period by remember { mutableStateOf(Period.MONTH) }
    var fromDate by remember { mutableStateOf(rangeFor(Period.MONTH, today).first) }
    var toDate by remember { mutableStateOf(rangeFor(Period.MONTH, today).second) }
    var picking by remember { mutableStateOf(0) }

    LaunchedEffect(sessionId) {
        val session = sessionRepository.getSession(sessionId) ?: return@LaunchedEffect
        val reg = registerRepository.getRegister(session.registerDefinitionId) ?: return@LaunchedEffect
        registerId = reg.id
        registerName = reg.name
        val variables = registerRepository.getVariablesForVersion(reg.id, reg.version)
        val options = variables.filter { it.type == VariableType.CODED }
            .associate { it.id to registerRepository.getCodeOptions(it.id) }
        val allVariables = reportRepo.getAllVariables(reg.id)
        val entries = reportRepo.getEntriesForRegister(reg.id)
        val values = reportRepo.getValuesForRegister(reg.id).groupBy { it.patientEntryId }
        inputs = ReportInputs(
            indicators = reportRepo.getIndicators(reg.id),
            settings = reportRepo.getSettings(reg.id),
            variables = variables,
            options = options,
            entries = ReportEngine.buildEntries(entries, values, allVariables)
        )
    }

    val inp = inputs
    val result = remember(inp, fromDate, toDate) {
        inp?.let {
            ReportEngine.generate(
                it.indicators, it.settings, it.variables, it.options, it.entries,
                startOf(fromDate), endOf(toDate)
            )
        }
    }
    val periodText = "${fromDate.format(dateFormat)} – ${toDate.format(dateFormat)}"

    if (picking != 0) {
        DatePickDialog(
            initial = if (picking == 1) fromDate else toDate,
            onPick = { d ->
                if (picking == 1) {
                    fromDate = d
                    if (toDate.isBefore(d)) toDate = d
                } else {
                    toDate = d
                    if (fromDate.isAfter(d)) fromDate = d
                }
            },
            onDismiss = { picking = 0 }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(title = "Report", onBack = onBack, actions = {
                IconButton(onClick = {
                    val r = result ?: return@IconButton
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText("$registerName\n$periodText", r.rows))
                    }
                    context.startActivity(Intent.createChooser(send, "Share report"))
                }) { Icon(Icons.Default.Share, contentDescription = "Share report") }
                IconButton(onClick = { onSetupReport(registerId) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Report setup")
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(registerName, style = MaterialTheme.typography.titleLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = {
                            period = p
                            if (p != Period.CUSTOM) {
                                val r = rangeFor(p, today)
                                fromDate = r.first
                                toDate = r.second
                            }
                        },
                        label = { Text(p.label) }
                    )
                }
            }
            if (period == Period.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = 1 }, modifier = Modifier.weight(1f)) {
                        Text("From ${fromDate.format(dateFormat)}")
                    }
                    OutlinedButton(onClick = { picking = 2 }, modifier = Modifier.weight(1f)) {
                        Text("To ${toDate.format(dateFormat)}")
                    }
                }
            }
            Text(
                periodText + (result?.let { "  •  ${it.patients} patients, ${it.visits} visits" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val r = result
            when {
                r == null -> Text("Working it out…")
                inp != null && inp.indicators.isEmpty() -> EmptyState(
                    title = "No report lines yet",
                    body = "Tell the app what each row of your summary form counts, using your columns and keys. Then this report fills itself in.",
                    actionLabel = "Set up the report",
                    onAction = { onSetupReport(registerId) }
                )
                else -> {
                    if (r.visits == 0) {
                        Text(
                            "Nobody was recorded in these dates. Pick other dates above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small)
                                .padding(10.dp)
                        )
                    }
                    r.notes.forEach { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small)
                                .padding(10.dp)
                        )
                    }
                    ReportTable(r.rows)
                }
            }
        }
    }
}

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

@Composable
private fun ReportTable(rows: List<ReportRow>) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("", Modifier.weight(1f))
                HeaderCell("Male")
                HeaderCell("Female")
                HeaderCell("Total")
            }
            var lastSection = ""
            rows.forEach { row ->
                if (row.section.isNotBlank() && row.section != lastSection) {
                    Text(
                        row.section,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    lastSection = row.section
                }
                ReportLine(row)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.End,
        modifier = Modifier.width(52.dp)
    )
}

@Composable
private fun ReportLine(row: ReportRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (row.isBand) 28.dp else 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                row.label,
                style = if (row.isBand) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (row.rule.isNotBlank()) {
                Text(row.rule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            row.problem?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        NumberCell(row.male)
        NumberCell(row.female)
        NumberCell(row.total)
    }
}

@Composable
private fun NumberCell(value: Int) {
    Text(
        value.toString(),
        style = NumeralStyle,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = Modifier.width(52.dp)
    )
}

private fun reportText(title: String, rows: List<ReportRow>): String {
    val sb = StringBuilder()
    sb.appendLine(title)
    var last = ""
    rows.forEach { r ->
        if (r.section.isNotBlank() && r.section != last) {
            sb.appendLine()
            sb.appendLine(r.section.uppercase())
            last = r.section
        }
        val pad = if (r.isBand) "    " else "  "
        sb.appendLine("$pad${r.label}: Male ${r.male}, Female ${r.female}, Total ${r.total}")
    }
    return sb.toString()
}
