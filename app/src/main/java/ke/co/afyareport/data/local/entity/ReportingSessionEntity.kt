package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An independent data-entry workspace for a specific register definition.
 *
 * The user can give the session a free-text [customLabel] and return
 * later to edit it. Sessions never form a permanent patient database;
 * patient recognition (New vs Return) is scoped to the current session.
 */
@Entity(
    tableName = "reporting_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RegisterDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["registerDefinitionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("registerDefinitionId")]
)
data class ReportingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val registerDefinitionId: Long,

    /**
     * Snapshot of the register version at the moment the session was created.
     * Protects the session from later edits to the register definition.
     */
    val registerVersion: Int,

    /** User-chosen free-text label (e.g. "OPD May Week 1"). */
    val customLabel: String = "",

    /** DRAFT or COMPLETED. */
    val status: String = STATUS_DRAFT,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
