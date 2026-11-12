package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A configurable register template defined by the user.
 *
 * Once created, a register appears in the list of available registers
 * and can be used to start reporting sessions.
 *
 * Version is incremented when the definition is edited so that
 * existing sessions remain tied to the version they were created with.
 */
@Entity(tableName = "register_definitions")
data class RegisterDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val description: String = "",

    /** Monotonic version of this register definition. */
    val version: Int = 1,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
