package ke.co.afyareport.ui.navigation

object Routes {
    const val HOME = "home"
    const val REGISTER_LIST = "registers"
    const val CREATE_REGISTER = "registers/create"
    const val SESSION_LIST = "sessions"
    const val CREATE_SESSION = "sessions/create"
    const val SESSION_DETAIL = "sessions/{sessionId}"

    fun sessionDetail(sessionId: Long) = "sessions/$sessionId"
}
