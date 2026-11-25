package ke.co.afyareport.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.Repo
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.monthFormat
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import java.time.YearMonth

/** The register as one continuous book, browsed month by month. */
@Composable
fun BookScreen(repo: Repo, onBack: () -> Unit, onOpenVisit: (Long) -> Unit, onAdd: () -> Unit) {
    val patients by repo.dao.observePatients().collectAsState(initial = emptyList())
    val visits by repo.dao.observeVisits().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var month by remember { mutableStateOf<YearMonth?>(null) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(visits.isEmpty()) {
        if (!initialized && visits.isNotEmpty()) {
            month = YearMonth.from(visits.maxOf { it.visitDate }.asDate())
            initialized = true
        }
    }

    val byId = remember(patients) { patients.associateBy { it.id } }
    val shown = remember(visits, byId, query, month) {
        visits.filter { v ->
            val ym = YearMonth.from(v.visitDate.asDate())
            (month == null || ym == month) &&
                (query.isBlank() || byId[v.patientId]?.let {
                    it.patientNo.contains(query.trim(), true) || it.name.contains(query.trim(), true)
                } == true)
        }.sortedWith(compareByDescending<VisitEntity> { it.visitDate }.thenByDescending { it.id })
    }
    val patientCount = remember(shown) { shown.map { it.patientId }.distinct().size }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Register book", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add a visit") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { month = (month ?: YearMonth.now()).minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            month?.format(monthFormat) ?: "All dates",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { month = (month ?: YearMonth.now()).plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${shown.size} visits  •  $patientCount patients",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { month = if (month == null) YearMonth.now() else null }) {
                            Text(if (month == null) "This month" else "Show all")
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Find a patient number or name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        title = "No visits here",
                        body = "Nothing recorded for these dates. Use the arrows to change month, or add a visit.",
                        actionLabel = "Add a visit",
                        onAction = onAdd
                    )
                }
            }
            items(shown, key = { it.id }) { v ->
                VisitRow(v, byId[v.patientId]) { onOpenVisit(v.id) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
