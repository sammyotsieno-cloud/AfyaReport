package ke.co.afyareport.engine

import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.PatientEntryEntity

/** First visit or revisit for every row, worked out from patient numbers across the whole register. */
fun effectiveVisitTypes(entries: List<PatientEntryEntity>): Map<Long, VisitType> {
    val seen = mutableSetOf<String>()
    val out = mutableMapOf<Long, VisitType>()
    entries.sortedWith(compareBy<PatientEntryEntity>({ it.visitDate }, { it.id })).forEach { e ->
        val key = e.patientKey?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val firstTime = key != null && seen.add(key)
        out[e.id] = when {
            e.visitTypeLocked || key == null -> e.visitType
            firstTime -> VisitType.NEW
            else -> VisitType.RETURN
        }
    }
    return out
}
