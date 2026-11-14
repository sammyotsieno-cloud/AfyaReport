package ke.co.afyareport.data.repository

import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.dao.ReportingSessionDao
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.ReportingSessionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for reporting sessions, patient entries and variable values.
 *
 * Patient matching (New vs Return) is scoped to the current session only.
 */
class SessionRepository(
    private val dao: ReportingSessionDao
) {

    fun observeAllSessions(): Flow<List<ReportingSessionEntity>> =
        dao.observeAll()

    fun observeSessionsForRegister(registerId: Long): Flow<List<ReportingSessionEntity>> =
        dao.observeForRegister(registerId)

    suspend fun getSession(id: Long): ReportingSessionEntity? =
        dao.getById(id)

    /**
     * Starts a new reporting session for the given register definition.
     */
    suspend fun createSession(
        registerDefinitionId: Long,
        registerVersion: Int,
        customLabel: String = ""
    ): Long {
        val session = ReportingSessionEntity(
            registerDefinitionId = registerDefinitionId,
            registerVersion = registerVersion,
            customLabel = customLabel.trim()
        )
        return dao.insert(session)
    }

    suspend fun updateSession(session: ReportingSessionEntity) {
        dao.update(
            session.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteById(id)
    }

    // ── Patient entries ────────────────────────────────────────

    suspend fun getEntries(sessionId: Long): List<PatientEntryEntity> =
        dao.getEntriesForSession(sessionId)

    /**
     * Finds existing entries in this session that share the same patientKey.
     * Used to help the user decide New vs Return.
     */
    suspend fun findPossibleMatches(sessionId: Long, patientKey: String): List<PatientEntryEntity> {
        if (patientKey.isBlank()) return emptyList()
        return dao.findEntriesByPatientKey(sessionId, patientKey.trim())
    }

    /**
     * Inserts a full patient entry (with visit type) and all its variable values.
     */
    suspend fun addPatientEntry(
        sessionId: Long,
        visitType: VisitType,
        patientKey: String?,
        sortOrder: Int,
        values: List<VariableValueEntity>
    ): Long {
        val entry = PatientEntryEntity(
            sessionId = sessionId,
            visitType = visitType,
            patientKey = patientKey?.trim()?.takeIf { it.isNotEmpty() },
            sortOrder = sortOrder
        )
        return dao.insertFullEntry(entry, values)
    }

    /** Updates an entry and replaces all its values in one database transaction. */
    suspend fun updatePatientEntryFull(
        entry: PatientEntryEntity,
        values: List<VariableValueEntity>
    ) {
        dao.updateFullEntry(
            entry.copy(updatedAt = System.currentTimeMillis()),
            values
        )
    }

    suspend fun updatePatientEntry(entry: PatientEntryEntity) {
        dao.updateEntry(
            entry.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deletePatientEntry(id: Long) {
        dao.deleteEntryById(id)
    }

    suspend fun getValuesForEntry(entryId: Long): List<VariableValueEntity> =
        dao.getValuesForEntry(entryId)
}
