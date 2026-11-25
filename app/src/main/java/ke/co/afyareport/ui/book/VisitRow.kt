package ke.co.afyareport.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.PatientEntity
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.asDate
import ke.co.afyareport.domain.bmiOf
import ke.co.afyareport.domain.labelOf
import ke.co.afyareport.domain.shortDayFormat
import ke.co.afyareport.ui.theme.ChipTone
import ke.co.afyareport.ui.theme.StatusChip

internal fun visitDetails(v: VisitEntity): String = listOfNotNull(
    v.age?.let { "Age $it" },
    v.sex.takeIf { it.isNotBlank() }?.let { Keys.sex.labelOf(it) },
    if (v.bpSys != null && v.bpDia != null) "BP ${v.bpSys}/${v.bpDia}" else null,
    bmiOf(v.weightKg, v.heightCm)?.let { "BMI $it" },
    v.hba1c?.let { "HbA1c $it" }
).joinToString("  •  ")

@Composable
internal fun VisitRow(v: VisitEntity, patient: PatientEntity?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            v.visitDate.asDate().format(shortDayFormat),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(patient?.patientNo ?: "?", style = MaterialTheme.typography.titleSmall)
                if (v.condition.isNotBlank()) {
                    StatusChip(Keys.condition.labelOf(v.condition), ChipTone.Neutral)
                }
                if (v.status.isNotBlank()) {
                    StatusChip(
                        if (v.status == "a") "New" else "Known",
                        if (v.status == "a") ChipTone.Done else ChipTone.Neutral
                    )
                }
            }
            if (patient != null && patient.name.isNotBlank()) {
                Text(patient.name, style = MaterialTheme.typography.bodySmall)
            }
            val d = visitDetails(v)
            if (d.isNotBlank()) {
                Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
