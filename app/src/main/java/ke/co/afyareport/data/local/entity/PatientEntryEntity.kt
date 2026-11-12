package ke.co.afyareport.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ke.co.afyareport.data.local.VisitType

/**
 * One patient row inside a ReportingSession.
 *
 * [visitType] records whether this is a NEW or RETURN visit.
 * Matching of the same patient is performed only within the
 * current session (no permanent patient master).
 *
 * [patientKey] is an optional helper string the user or system
 * can use to recognise repeats (e.g. name + age, or a local ID).
 */
@Entity(
    tableName = "patient_entries",
    foreignKeys = [
        ForeignKey(
            entity = ReportingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "patientKey"])
    ]
)
data class PatientEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: Long,

    /** NEW or RETURN – counted separately in monthly reports. */
    val visitType: VisitType = VisitType.NEW,

    /**
     * Optional lightweight key used only for matching inside this session.
     * Never treated as a permanent patient identifier.
     */
    val patientKey: String? = null,

    /** Order of entry within the session. */
    val sortOrder: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
