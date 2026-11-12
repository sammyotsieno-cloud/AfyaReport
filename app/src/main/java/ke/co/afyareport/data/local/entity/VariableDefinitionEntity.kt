package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ke.co.afyareport.data.local.VariableType

/**
 * A single data field (variable) that belongs to a RegisterDefinition.
 *
 * Users define the name, type, measurement instructions and whether
 * the field is required. For CODED variables the actual options live
 * in CodeOptionEntity.
 */
@Entity(
    tableName = "variable_definitions",
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
data class VariableDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val registerDefinitionId: Long,

    val name: String,

    val type: VariableType,

    /** Free-text guidance shown to the data-entry user. */
    val measurementInstructions: String = "",

    val isRequired: Boolean = false,

    /** Display order inside the register. */
    val sortOrder: Int = 0
)
