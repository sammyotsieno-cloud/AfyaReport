package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which columns hold sex and age, and which values mean male and female. */
@Entity(
    tableName = "report_settings",
    foreignKeys = [
        ForeignKey(
            entity = RegisterDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["registerDefinitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReportSettingsEntity(
    @PrimaryKey val registerDefinitionId: Long,
    val sexColumn: String = "",
    val maleValue: String = "",
    val femaleValue: String = "",
    val ageColumn: String = ""
)

/** One line of the monthly summary, e.g. "Newly diagnosed diabetes cases". */
@Entity(
    tableName = "report_indicators",
    foreignKeys = [
        ForeignKey(
            entity = RegisterDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["registerDefinitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("registerDefinitionId")]
)
data class ReportIndicatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val registerDefinitionId: Long,
    val section: String = "",
    val label: String,
    val countMode: String = COUNT_PATIENTS,
    val matchAny: Boolean = false,
    val ageBands: String = "",
    val scope: String = SCOPE_PERIOD,
    val conditionsJson: String = "[]",
    val sortOrder: Int = 0
) {
    companion object {
        const val COUNT_PATIENTS = "PATIENTS"
        const val COUNT_VISITS = "VISITS"
        const val SCOPE_PERIOD = "PERIOD"
        const val SCOPE_CUMULATIVE = "CUMULATIVE"
    }
}
