package ke.co.afyareport.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.MarginCard
import ke.co.afyareport.ui.theme.StatusChip

@Composable
fun RegisterListScreen(
    registerRepository: RegisterRepository,
    onBack: () -> Unit,
    onCreateRegister: () -> Unit,
    onRegisterClick: (Long) -> Unit = {}
) {
    val registers by registerRepository.observeAllRegisters().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { AfyaTopBar(title = "Registers", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRegister,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New register") }
            )
        }
    ) { padding ->
        if (registers.isEmpty()) {
            EmptyState(
                title = "No registers yet",
                body = "A register is the list of columns you record for each patient, such as age, BP and diagnosis code.",
                actionLabel = "Create your first register",
                onAction = onCreateRegister,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(registers, key = { it.id }) { register ->
                    RegisterCard(register) { onRegisterClick(register.id) }
                }
            }
        }
    }
}

@Composable
private fun RegisterCard(register: RegisterDefinitionEntity, onClick: () -> Unit) {
    MarginCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = register.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            StatusChip("v${register.version}", ChipTone.Neutral)
        }
        if (register.description.isNotBlank()) {
            Text(
                text = register.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
