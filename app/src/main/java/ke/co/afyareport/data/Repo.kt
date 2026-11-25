package ke.co.afyareport.data

import ke.co.afyareport.domain.patientKey

class Repo(val dao: AppDao) {

    /** Finds the patient with this number or enrols a new one. A name is saved once, never overwritten. */
    suspend fun ensurePatient(patientNo: String, name: String): PatientEntity {
        val no = patientNo.trim()
        val key = patientKey(no)
        val existing = dao.findPatient(key)
        if (existing != null) {
            if (name.isNotBlank() && existing.name.isBlank()) {
                val updated = existing.copy(name = name.trim())
                dao.updatePatient(updated)
                return updated
            }
            return existing
        }
        val id = dao.insertPatient(PatientEntity(patientNo = no, patientKey = key, name = name.trim()))
        return dao.findPatient(key) ?: PatientEntity(id = id, patientNo = no, patientKey = key, name = name.trim())
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
