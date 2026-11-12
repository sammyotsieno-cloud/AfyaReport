package ke.co.afyareport.data.local

/**
 * Distinguishes a patient's first encounter (NEW) from a subsequent
 * encounter (RETURN) within the same reporting session.
 *
 * These values are counted separately in monthly summaries,
 * matching standard MOH register practice.
 */
enum class VisitType {
    NEW,
    RETURN
}
