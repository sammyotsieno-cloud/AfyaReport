package ke.co.afyareport.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM patients")
    fun observePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM visits")
    fun observeVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM patients WHERE patientKey = :key LIMIT 1")
    suspend fun findPatient(key: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatient(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPatient(p: PatientEntity): Long

    @Update
    suspend fun updatePatient(p: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatient(id: Long)

    @Insert
    suspend fun insertVisit(v: VisitEntity): Long

    @Update
    suspend fun updateVisit(v: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisit(id: Long)

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getVisit(id: Long): VisitEntity?

    @Query("SELECT * FROM visits")
    suspend fun allVisits(): List<VisitEntity>

    @Query("SELECT * FROM patients")
    suspend fun allPatients(): List<PatientEntity>

    @Query("SELECT * FROM month_counts WHERE id = :id")
    suspend fun getCounts(id: Int): MonthCountsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCounts(c: MonthCountsEntity)
}
