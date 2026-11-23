package ke.co.afyareport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity

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
}
