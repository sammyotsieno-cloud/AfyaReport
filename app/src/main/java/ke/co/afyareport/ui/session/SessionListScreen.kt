package ke.co.afyareport.ui.session
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing

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
import ke.co.afyareport.data.local.entity.ReportingSessionEntity
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.EmptyState
import ke.co.afyareport.ui.theme.MarginCard
import ke.co.afyareport.ui.theme.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionListScreen(
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
    onCreateSession: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    val sessions by sessionRepository.observeAllSessions().collectAsState(initial = emptyList())
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Reporting sessions", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateSession,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New session") }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(
                title = "No sessions yet",
                body = "A session is one batch of patient entries from a register, usually one month.",
                actionLabel = "Start a session",
                onAction = onCreateSession,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onClick = { onSessionClick(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: ReportingSessionEntity, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val done = session.status == ReportingSessionEntity.STATUS_COMPLETED
    MarginCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.customLabel.ifBlank { "Untitled session" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                text = if (done) "Completed" else "Draft",
                tone = if (done) ChipTone.Done else ChipTone.Draft
            )
        }
        Text(
            text = "Updated ${dateFormat.format(Date(session.updatedAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
