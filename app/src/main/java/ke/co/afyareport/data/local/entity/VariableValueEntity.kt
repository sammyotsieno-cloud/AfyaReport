package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The concrete value entered for one variable on one patient entry.
 *
 * For CODED and VISIT_TYPE variables the [value] stores the short code
 * (or "NEW" / "RETURN"). For other types it stores the textual / numeric
 * representation of the answer.
 */
@Entity(
    tableName = "variable_values",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VariableDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["variableDefinitionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("patientEntryId"),
        Index("variableDefinitionId"),
        Index(value = ["patientEntryId", "variableDefinitionId"], unique = true)
    ]
)
data class VariableValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val patientEntryId: Long,

    val variableDefinitionId: Long,

    /**
     * Stored value.
     * - TEXT / NUMBER / DATE / BOOLEAN → string representation
     * - CODED → the selected CodeOption.code
     * - VISIT_TYPE → "NEW" or "RETURN"
     */
    val value: String
)
