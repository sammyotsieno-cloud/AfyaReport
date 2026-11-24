package ke.co.afyareport.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.MarginCard
import ke.co.afyareport.ui.theme.StatusChip

@Composable
fun HomeScreen(
    registerRepository: RegisterRepository,
    onOpenRegister: (Long) -> Unit,
    onNewRegister: () -> Unit,
    onOlderSessions: () -> Unit,
    onTools: () -> Unit
) {
    val registers by registerRepository.observeAllRegisters().collectAsState(initial = emptyList())
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewRegister,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New register") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        "AfyaReport",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your registers. Open one to add patients or get the report.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (registers.isEmpty()) {
                item {
                    EmptyState(
                        title = "No registers yet",
                        body = "Create a register to match one of your paper registers, with its columns and keys.",
                        actionLabel = "Create a register",
                        onAction = onNewRegister
                    )
                }
            } else {
                items(registers, key = { it.id }) { r ->
                    MarginCard(onClick = { onOpenRegister(r.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusChip("v${r.version}", ChipTone.Neutral)
                        }
                        if (r.description.isNotBlank()) {
                            Text(
                                r.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Text(
                            "Add patients  •  Get the report",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            item {
                Column {
                    TextButton(onClick = onTools) { Text("Backup, restore and sharing") }
                    TextButton(onClick = onOlderSessions) { Text("Earlier batches (old screens)") }
                }
            }
        }
    }
}
