package ke.co.afyareport.ui.visit

import ke.co.afyareport.domain.Keys
import ke.co.afyareport.domain.Recurring
import ke.co.afyareport.domain.fmtNum

/** Fills the blank fields of the form with what this patient usually has. Never overwrites what was typed. */
fun VisitForm.applyProfile(r: Recurring) {
    if (sex.isBlank()) sex = r.sex
    val a = r.age
    if (age.isBlank() && a != null && a in 0..120) age = a.toString()
    if (condition.isBlank()) condition = r.condition
    if (diagnosis.isBlank() && r.diagnosis in Keys.diagnosisFor(condition).map { it.code }) diagnosis = r.diagnosis
    if (status.isBlank() && r.condition.isNotBlank()) status = "b"
    if (treatment.isEmpty()) treatment.addAll(r.treatment)
    if (nhif.isBlank()) nhif = r.nhif
    val h = r.heightCm
    if (height.isBlank() && h != null) height = fmtNum(h)
}
