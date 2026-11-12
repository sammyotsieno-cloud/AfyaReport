package ke.co.afyareport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.ui.home.HomeScreen
import ke.co.afyareport.ui.navigation.Routes
import ke.co.afyareport.ui.register.CreateRegisterScreen
import ke.co.afyareport.ui.register.RegisterListScreen
import ke.co.afyareport.ui.session.CreateSessionScreen
import ke.co.afyareport.ui.session.SessionDetailScreen
import ke.co.afyareport.ui.session.SessionListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AfyaReportApp
        val container = app.container
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AfyaReportNavHost(
                        registerRepository = container.registerRepository,
                        sessionRepository = container.sessionRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun AfyaReportNavHost(
    registerRepository: RegisterRepository,
    sessionRepository: SessionRepository
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToRegisters = { navController.navigate(Routes.REGISTER_LIST) },
                onNavigateToSessions = { navController.navigate(Routes.SESSION_LIST) }
            )
        }
        composable(Routes.REGISTER_LIST) {
            RegisterListScreen(
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onCreateRegister = { navController.navigate(Routes.CREATE_REGISTER) }
            )
        }
        composable(Routes.CREATE_REGISTER) {
            CreateRegisterScreen(
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onRegisterCreated = { navController.popBackStack() }
            )
        }
        composable(Routes.SESSION_LIST) {
            SessionListScreen(
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onCreateSession = { navController.navigate(Routes.CREATE_SESSION) },
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.sessionDetail(sessionId))
                }
            )
        }
        composable(Routes.CREATE_SESSION) {
            CreateSessionScreen(
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onSessionCreated = { sessionId ->
                    navController.popBackStack()
                    navController.navigate(Routes.sessionDetail(sessionId))
                }
            )
        }
        composable(
            route = Routes.SESSION_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
                ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
