package ke.co.afyareport.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.MatchLevel
import ke.co.afyareport.domain.Suggestion
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.dayFormat
import ke.co.afyareport.domain.labelOf
import ke.co.afyareport.ui.theme.Card
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.StatusChip
import java.time.LocalDate

/** Existing patients this entry may belong to. Tap one to use it. */
@Composable
fun SuggestionPanel(
    suggestions: List<Suggestion>,
    onPick: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit,
    pickLabel: String = "Use this patient"
) {
    if (suggestions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Could this be a patient already in the system?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        suggestions.forEach { s ->
            val p = s.facts.patient
            val age = s.facts.ageOn(LocalDate.now())
            val details = listOfNotNull(
                s.facts.sex.takeIf { it.isNotBlank() }?.let { Keys.sex.labelOf(it) },
                age?.let { "about $it years" },
                "${s.facts.visitCount} visit(s)",
                s.facts.lastVisitDay?.let { "last ${it.asDate().format(dayFormat)}" }
            ).joinToString("  •  ")
            Card(onClick = { onPick(s) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            p.patientNo + if (p.name.isNotBlank()) "  –  ${p.name}" else "",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(
                            if (s.level == MatchLevel.LIKELY) "Likely" else "Possible",
                            if (s.level == MatchLevel.LIKELY) ChipTone.Done else ChipTone.Draft
                        )
                    }
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Because: " + s.reasons.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onPick(s) }) { Text(pickLabel) }
                        TextButton(onClick = { onDismiss(s) }) { Text("Not them") }
                    }
                }
            }
        }
    }
}
