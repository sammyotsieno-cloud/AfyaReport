package ke.co.afyareport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.ReportingSessionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportingSessionDao {

    @Query("SELECT * FROM reporting_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReportingSessionEntity>>

    @Query("SELECT * FROM reporting_sessions WHERE registerDefinitionId = :registerId ORDER BY updatedAt DESC")
    fun observeForRegister(registerId: Long): Flow<List<ReportingSessionEntity>>

    @Query("SELECT * FROM reporting_sessions WHERE id = :id")
    suspend fun getById(id: Long): ReportingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReportingSessionEntity): Long

    @Update
    suspend fun update(session: ReportingSessionEntity)

    @Query("DELETE FROM reporting_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Patient entries ────────────────────────────────────────

    @Query("SELECT * FROM patient_entries WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    suspend fun getEntriesForSession(sessionId: Long): List<PatientEntryEntity>

    @Query("SELECT * FROM patient_entries WHERE sessionId = :sessionId AND patientKey = :patientKey")
    suspend fun findEntriesByPatientKey(sessionId: Long, patientKey: String): List<PatientEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PatientEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: PatientEntryEntity)

    @Query("DELETE FROM patient_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    // ── Variable values ────────────────────────────────────────

    @Query("SELECT * FROM variable_values WHERE patientEntryId = :entryId")
    suspend fun getValuesForEntry(entryId: Long): List<VariableValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValue(value: VariableValueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValues(values: List<VariableValueEntity>): List<Long>

    @Query("DELETE FROM variable_values WHERE patientEntryId = :entryId")
    suspend fun deleteValuesForEntry(entryId: Long)

    /**
     * Update a patient entry and replace its variable values atomically.
     */
    @Transaction
    suspend fun updateFullEntry(
        entry: PatientEntryEntity,
        values: List<VariableValueEntity>
    ) {
        updateEntry(entry)
        deleteValuesForEntry(entry.id)
        if (values.isNotEmpty()) {
            insertValues(values.map { it.copy(patientEntryId = entry.id) })
        }
    }

    /**
     * Insert a complete patient entry together with all its variable values.
     */
    @Transaction
    suspend fun insertFullEntry(
        entry: PatientEntryEntity,
        values: List<VariableValueEntity>
    ): Long {
        val entryId = insertEntry(entry)
        if (values.isNotEmpty()) {
            insertValues(values.map { it.copy(patientEntryId = entryId) })
        }
        return entryId
    }
}
