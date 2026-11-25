@file:OptIn(ExperimentalLayoutApi::class)

package ke.co.afyareport.ui.report

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.Report740
import ke.co.afyareport.domain.Row740
import ke.co.afyareport.domain.RowKind
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.buildReport740
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.monthFormat
import ke.co.afyareport.ui.common.DatePickDialog
import ke.co.afyareport.ui.common.Notice
import ke.co.afyareport.ui.common.MonthSelector
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.NumeralStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

private const val REPORT_TITLE = "MOH 740 Diabetes and Hypertension Comprehensive Care - Monthly Summary"

@Composable
fun ReportScreen(repo: Repo, onBack: () -> Unit, onCounts: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    val counts by repo.dao.observeCounts().collectAsState(initial = emptyList())

    var month by remember { mutableStateOf(YearMonth.now()) }
    var custom by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var toDate by remember { mutableStateOf(LocalDate.now()) }
    var picking by remember { mutableStateOf(0) }
    var initialized by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visits.isEmpty()) {
        if (!initialized && visits.isNotEmpty()) {
            month = YearMonth.from(visits.maxOf { it.visitDate }.asDate())
            initialized = true
        }
    }

    val from = if (custom) fromDate else month.atDay(1)
    val to = if (custom) toDate else month.atEndOfMonth()
    val report = remember(visits, counts, from, to) {
        buildReport740(visits, from.toEpochDay(), to.toEpochDay(), counts)
    }
    val periodText = "${from.format(dayFormat)} - ${to.format(dayFormat)}"

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { ReportPdf.write(context, uri, REPORT_TITLE, periodText, report) }
            message = if (ok) "PDF saved." else "The PDF could not be saved."
        }
    }

    if (picking == 1) {
        DatePickDialog(
            initial = fromDate,
            onPick = { fromDate = it; if (toDate.isBefore(it)) toDate = it },
            onDismiss = { picking = 0 }
        )
    }
    if (picking == 2) {
        DatePickDialog(
            initial = toDate,
            onPick = { toDate = it; if (fromDate.isAfter(it)) fromDate = it },
            onDismiss = { picking = 0 }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Monthly report (740)", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            message?.let { Notice(it) }

            val visitMonths = remember(visits) { visits.map { YearMonth.from(it.visitDate.asDate()) }.toSet() }
            MonthSelector(
                month = month,
                onChange = { month = it; custom = false },
                monthsWithData = visitMonths,
                label = if (custom) "Custom dates" else null
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !custom,
                    onClick = { custom = false },
                    label = { Text("Whole month") }
                )
                FilterChip(
                    selected = custom,
                    onClick = {
                        custom = true
                        fromDate = month.atDay(1)
                        toDate = month.atEndOfMonth()
                    },
                    label = { Text("Custom dates") }
                )
            }
            if (custom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = 1 }, modifier = Modifier.weight(1f)) {
                        Text("From ${fromDate.format(dayFormat)}")
                    }
                    OutlinedButton(onClick = { picking = 2 }, modifier = Modifier.weight(1f)) {
                        Text("To ${toDate.format(dayFormat)}")
                    }
                }
            }
            Text(
                "$periodText  •  ${report.patients} patients, ${report.visits} visits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCounts) { Text("Admissions and deaths") }
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText(periodText, report))
                    }
                    context.startActivity(Intent.createChooser(send, "Share report"))
                }) { Text("Share as text") }
                OutlinedButton(onClick = {
                    pdfLauncher.launch("MOH740-${from}-to-${to}.pdf")
                }) { Text("Save as PDF") }
            }

            if (visits.isEmpty()) {
                EmptyState(
                    title = "Nothing to report yet",
                    body = "Add visits to the register first. The report fills itself in from them."
                )
            } else {
                if (report.visits == 0) {
                    Notice("Nobody was recorded in these dates. Use the arrows to pick another month.", warning = true)
                }
                report.notes.forEach { Notice(it, warning = true) }
                Table740(report.rows)
            }
        }
    }
}

@Composable
private fun Table740(rows: List<Row740>) {
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
            rows.forEach { r ->
                Line740(r)
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
private fun Line740(r: Row740) {
    when (r.kind) {
        RowKind.SECTION -> Text(
            r.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        RowKind.SUBHEAD -> Text(
            r.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        else -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (r.kind == RowKind.BAND) 28.dp else 12.dp,
                    end = 12.dp, top = 8.dp, bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                r.label,
                style = if (r.kind == RowKind.BAND) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            NumberCell(r.male)
            NumberCell(r.female)
            NumberCell(r.total)
        }
    }
}

@Composable
private fun NumberCell(value: String) {
    Text(
        value,
        style = NumeralStyle,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = Modifier.width(52.dp)
    )
}

private fun reportText(period: String, report: Report740): String {
    val sb = StringBuilder()
    sb.appendLine(REPORT_TITLE)
    sb.appendLine(period)
    report.rows.forEach { r ->
        when (r.kind) {
            RowKind.SECTION -> { sb.appendLine(); sb.appendLine(r.label) }
            RowKind.SUBHEAD -> sb.appendLine("  ${r.label}")
            RowKind.BAND -> sb.appendLine("      ${r.label}: M ${r.male}, F ${r.female}, T ${r.total}")
            RowKind.LINE -> sb.appendLine("  ${r.label}: M ${r.male}, F ${r.female}, T ${r.total}")
        }
    }
    return sb.toString()
}
