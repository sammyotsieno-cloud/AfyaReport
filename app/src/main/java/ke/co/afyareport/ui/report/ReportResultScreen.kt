package ke.co.afyareport.ui.report

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.engine.ReportEngine
import ke.co.afyareport.engine.ReportResult
import ke.co.afyareport.engine.ReportRow
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.NumeralStyle

/** Opens, reads every key and rule, and computes the whole report straight away. */
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
    var registerId by remember { mutableStateOf(0L) }
    var registerName by remember { mutableStateOf("") }
    var sessionLabel by remember { mutableStateOf("") }
    var entryCount by remember { mutableStateOf(0) }
    var hasLines by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ReportResult?>(null) }

    LaunchedEffect(sessionId) {
        val session = sessionRepository.getSession(sessionId) ?: return@LaunchedEffect
        registerId = session.registerDefinitionId
        registerName = registerRepository.getRegister(session.registerDefinitionId)?.name ?: ""
        sessionLabel = session.customLabel.ifBlank { "Untitled session" }
        val variables = registerRepository.getVariablesForVersion(session.registerDefinitionId, session.registerVersion)
        val options = variables.filter { it.type == VariableType.CODED }
            .associate { it.id to registerRepository.getCodeOptions(it.id) }
        val entries = sessionRepository.getEntries(sessionId)
        entryCount = entries.size
        val valuesByEntry = entries.associate { it.id to sessionRepository.getValuesForEntry(it.id) }
        val indicators = reportRepo.getIndicators(session.registerDefinitionId)
        hasLines = indicators.isNotEmpty()
        val settings = reportRepo.getSettings(session.registerDefinitionId)
        val data = ReportEngine.buildEntries(entries, valuesByEntry, variables)
        result = ReportEngine.generate(indicators, settings, variables, options, data)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AfyaTopBar(title = "Monthly report", onBack = onBack, actions = {
                IconButton(onClick = {
                    val r = result ?: return@IconButton
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText("$registerName – $sessionLabel", r.rows))
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
            Text(
                "$sessionLabel  •  $entryCount patients entered",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val r = result
            when {
                r == null -> Text("Working it out…")
                !hasLines -> EmptyState(
                    title = "No report lines yet",
                    body = "Tell the app what each row of your monthly summary counts, using your columns and keys. Then this report fills itself in.",
                    actionLabel = "Set up the report",
                    onAction = { onSetupReport(registerId) }
                )
                else -> {
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
