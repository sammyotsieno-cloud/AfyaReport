package ke.co.afyareport.ui.navigation

object Routes {
    const val HOME = "home"
    const val REGISTER_LIST = "registers"
    const val CREATE_REGISTER = "registers/create"
    const val REGISTER_DETAIL = "registers/{registerId}"
    const val EDIT_REGISTER = "registers/{registerId}/edit"
    const val SESSION_LIST = "sessions"
    const val CREATE_SESSION = "sessions/create"
    const val SESSION_DETAIL = "sessions/{sessionId}"
    const val SESSION_SUMMARY = "sessions/{sessionId}/summary"
    const val SESSION_REPORT = "sessions/{sessionId}/report"
    const val REPORT_SETUP = "registers/{registerId}/report-setup"

    fun registerDetail(registerId: Long) = "registers/$registerId"
    fun editRegister(registerId: Long) = "registers/$registerId/edit"
    fun sessionDetail(sessionId: Long) = "sessions/$sessionId"
    fun sessionSummary(sessionId: Long) = "sessions/$sessionId/summary"
    fun sessionReport(sessionId: Long) = "sessions/$sessionId/report"
    fun reportSetup(registerId: Long) = "registers/$registerId/report-setup"
}
