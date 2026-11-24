package ke.co.afyareport.ui.report

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.EmptyState
import kotlinx.coroutines.flow.first

/** Opens the report for a whole register (all its entries, whichever batch they were keyed in). */
@Composable
fun RegisterReportRoute(
    registerId: Long,
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
    onSetupReport: (Long) -> Unit
) {
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var none by remember { mutableStateOf(false) }
    LaunchedEffect(registerId) {
        val list = sessionRepository.observeSessionsForRegister(registerId).first()
        if (list.isEmpty()) none = true else sessionId = list.first().id
    }
    val id = sessionId
    when {
        id != null -> ReportResultScreen(id, registerRepository, sessionRepository, onBack, onSetupReport)
        none -> Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { AfyaTopBar(title = "Report", onBack = onBack) }
        ) { padding ->
            EmptyState(
                title = "Nothing to report yet",
                body = "Add patients to this register first. Then the report fills itself in.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
        else -> Text("Loading…", Modifier.padding(24.dp))
    }
}
