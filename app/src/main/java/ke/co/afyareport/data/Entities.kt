package ke.co.afyareport.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per enrolled patient, created automatically the first time a patient number is used. */
@Entity(tableName = "patients", indices = [Index(value = ["patientKey"], unique = true)])
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientNo: String,
    val patientKey: String,
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** One row of the MOH 222 register. [visitDate] is an epoch day (LocalDate.toEpochDay). */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId"), Index("visitDate")]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val visitDate: Long,
    val age: Int? = null,
    val sex: String = "",
    val visitNo: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val waistCm: Double? = null,
    val bpSys: Int? = null,
    val bpDia: Int? = null,
    val condition: String = "",
    val status: String = "",
    val rbs: Double? = null,
    val fbs: Double? = null,
    val hba1c: Double? = null,
    val diagnosis: String = "",
    val complications: String = "",
    val treatment: String = "",
    val footScreened: String = "",
    val diabeticFoot: String = "",
    val footRisk: String = "",
    val footRiskFactors: String = "",
    val footOutcome: String = "",
    val tbScreened: String = "",
    val tbStatus: String = "",
    val nhif: String = "",
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Monthly counts typed in for the 740 admissions and deaths lines. id = year * 100 + month. */
@Entity(tableName = "month_counts")
data class MonthCountsEntity(
    @PrimaryKey val id: Int,
    val dkaM: Int = 0, val dkaF: Int = 0,
    val hypoM: Int = 0, val hypoF: Int = 0,
    val strokeM: Int = 0, val strokeF: Int = 0,
    val htnEmergM: Int = 0, val htnEmergF: Int = 0,
    val deathDmM: Int = 0, val deathDmF: Int = 0,
    val deathHtnM: Int = 0, val deathHtnF: Int = 0
)
