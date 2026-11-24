package ke.co.afyareport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity

@Dao
interface ReportDao {
    @Query("SELECT * FROM report_indicators WHERE registerDefinitionId = :registerId ORDER BY sortOrder, id")
    suspend fun getIndicators(registerId: Long): List<ReportIndicatorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndicator(indicator: ReportIndicatorEntity): Long

    @Query("DELETE FROM report_indicators WHERE id = :id")
    suspend fun deleteIndicator(id: Long)

    @Query("SELECT * FROM report_settings WHERE registerDefinitionId = :registerId")
    suspend fun getSettings(registerId: Long): ReportSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ReportSettingsEntity)

    /** Every column the register has ever had, across all its versions. */
    @Query("SELECT * FROM variable_definitions WHERE registerDefinitionId = :registerId")
    suspend fun getAllVariables(registerId: Long): List<VariableDefinitionEntity>

    /** Every patient row of the register, whichever batch it was keyed in. */
    @Query(
        "SELECT e.* FROM patient_entries e INNER JOIN reporting_sessions s ON e.sessionId = s.id " +
            "WHERE s.registerDefinitionId = :registerId"
    )
    suspend fun getEntriesForRegister(registerId: Long): List<PatientEntryEntity>

    @Query(
        "SELECT v.* FROM variable_values v " +
            "INNER JOIN patient_entries e ON v.patientEntryId = e.id " +
            "INNER JOIN reporting_sessions s ON e.sessionId = s.id " +
            "WHERE s.registerDefinitionId = :registerId"
    )
    suspend fun getValuesForRegister(registerId: Long): List<VariableValueEntity>
}
