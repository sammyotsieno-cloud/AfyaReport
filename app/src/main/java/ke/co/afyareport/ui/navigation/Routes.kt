package ke.co.afyareport.ui.navigation

object Routes {
    const val HOME = "home"
    const val REGISTER_LIST = "registers"
    const val CREATE_REGISTER = "registers/create"
    const val SESSION_LIST = "sessions"
    const val CREATE_SESSION = "sessions/create"
    const val SESSION_DETAIL = "sessions/{sessionId}"
    const val SESSION_SUMMARY = "sessions/{sessionId}/summary"

    fun sessionDetail(sessionId: Long) = "sessions/$sessionId"
    fun sessionSummary(sessionId: Long) = "sessions/$sessionId/summary"
}
