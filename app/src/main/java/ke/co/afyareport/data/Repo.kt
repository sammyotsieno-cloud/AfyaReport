package ke.co.afyareport.data

import ke.co.afyareport.domain.patientKey
import ke.co.afyareport.domain.properName
import java.time.LocalDate

class Repo(val dao: AppDao) {

    /** Finds the patient with this number or enrols a new one. A name is saved once, never overwritten. */
    suspend fun ensurePatient(patientNo: String, name: String): PatientEntity {
        val no = patientNo.trim()
        val key = patientKey(no)
        val existing = dao.findPatient(key)
        if (existing != null) {
            if (name.isNotBlank() && existing.name.isBlank()) {
                val updated = existing.copy(name = properName(name))
                dao.updatePatient(updated)
                return updated
            }
            return existing
        }
        val id = dao.insertPatient(PatientEntity(patientNo = no, patientKey = key, name = properName(name)))
        return dao.findPatient(key)
            ?: PatientEntity(id = id, patientNo = no, patientKey = key, name = properName(name))
    }

    /** Adds a patient to the list with no visit. Returns the patient and whether it was newly created. */
    suspend fun registerPatient(patientNo: String, name: String, sex: String, age: Int?): Pair<PatientEntity, Boolean> {
        val no = patientNo.trim()
        val key = patientKey(no)
        val existing = dao.findPatient(key)
        if (existing != null) return existing to false
        dao.insertPatient(
            PatientEntity(
                patientNo = no,
                patientKey = key,
                name = properName(name),
                sex = sex,
                age = age,
                ageAsOf = LocalDate.now().toEpochDay()
            )
        )
        val saved = dao.findPatient(key) ?: error("The patient could not be saved")
        return saved to true
    }

    suspend fun addVisit(patientNo: String, name: String, visit: VisitEntity): Long {
        val p = ensurePatient(patientNo, name)
        return dao.insertVisit(visit.copy(patientId = p.id))
    }

    suspend fun changeVisit(patientNo: String, name: String, visit: VisitEntity) {
        val p = ensurePatient(patientNo, name)
        dao.updateVisit(visit.copy(patientId = p.id, updatedAt = System.currentTimeMillis()))
    }
}
