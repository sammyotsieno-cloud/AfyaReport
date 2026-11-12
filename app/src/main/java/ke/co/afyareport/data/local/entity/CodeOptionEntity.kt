package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single selectable option for a CODED variable.
 *
 * The short [code] (e.g. "a", "b", "12.3") is what is stored in
 * VariableValue and later used for counting in monthly reports.
 * [label] is the human-readable text shown during data entry.
 */
@Entity(
    tableName = "code_options",
    foreignKeys = [
        ForeignKey(
            entity = VariableDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["variableDefinitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("variableDefinitionId")]
)
data class CodeOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val variableDefinitionId: Long,

    /** Short code used for storage and reporting (e.g. "a", "b"). */
    val code: String,

    /** Human-readable label shown to the user. */
    val label: String,

    val sortOrder: Int = 0
)
