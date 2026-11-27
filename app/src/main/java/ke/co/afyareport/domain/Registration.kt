package ke.co.afyareport.domain

import ke.co.afyareport.data.PatientEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Brief info for a patient who was added without any visit yet. */
fun registrationBrief(p: PatientEntity?): List<Pair<String, String>> {
    if (p == null) return emptyList()
    val ageNow = p.age?.let {
        it + ChronoUnit.YEARS.between(LocalDate.ofEpochDay(p.ageAsOf), LocalDate.now()).toInt()
    }
    val text = listOfNotNull(
        ageNow?.let { "about $it years" },
        p.sex.takeIf { it.isNotBlank() }?.let { Keys.sex.labelOf(it) }
    ).joinToString("  •  ")
    return if (text.isEmpty()) emptyList() else listOf("Age and sex" to text)
}
