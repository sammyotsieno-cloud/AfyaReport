package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ke.co.afyareport.data.local.VariableType

/** Immutable variable definition belonging to one register version. */
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
    indices = [
        Index("registerDefinitionId"),
        Index(value = ["registerDefinitionId", "registerVersion"])
    ]
)
data class VariableDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val registerDefinitionId: Long,
    val registerVersion: Int,
    val name: String,
    val type: VariableType,
    val measurementInstructions: String = "",
    val isRequired: Boolean = false,
    val sortOrder: Int = 0
)
