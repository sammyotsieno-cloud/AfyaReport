package ke.co.afyareport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository
import ke.co.afyareport.ui.home.HomeScreen
import ke.co.afyareport.ui.navigation.Routes
import ke.co.afyareport.ui.register.CreateRegisterScreen
import ke.co.afyareport.ui.register.RegisterListScreen
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

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
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
                onRegisterCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SESSION_LIST) {
            SessionListScreen(
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onCreateSession = {
                    // Will be wired in a later batch (select register → start session)
                }
            )
        }
    }
}
