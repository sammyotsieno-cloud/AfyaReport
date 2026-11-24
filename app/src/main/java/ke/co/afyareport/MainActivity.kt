package ke.co.afyareport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import ke.co.afyareport.ui.register.EditRegisterScreen
import ke.co.afyareport.ui.register.PatientEntryScreen
import ke.co.afyareport.ui.register.RegisterBookScreen
import ke.co.afyareport.ui.register.RegisterDetailScreen
import ke.co.afyareport.ui.register.RegisterListScreen
import ke.co.afyareport.ui.report.RegisterReportRoute
import ke.co.afyareport.ui.report.ReportResultScreen
import ke.co.afyareport.ui.report.ReportSetupScreen
import ke.co.afyareport.ui.session.CreateSessionScreen
import ke.co.afyareport.ui.session.SessionDetailScreen
import ke.co.afyareport.ui.session.SessionListScreen
import ke.co.afyareport.ui.session.SessionSummaryScreen
import ke.co.afyareport.ui.theme.AfyaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AfyaReportApp
        val container = app.container
        setContent {
            AfyaTheme {
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
    val registerArg = listOf(navArgument("registerId") { type = NavType.LongType })
    val sessionArg = listOf(navArgument("sessionId") { type = NavType.LongType })

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                registerRepository = registerRepository,
                onOpenRegister = { id -> navController.navigate(Routes.registerBook(id)) },
                onNewRegister = { navController.navigate(Routes.CREATE_REGISTER) },
                onOlderSessions = { navController.navigate(Routes.SESSION_LIST) }
            )
        }
        composable(Routes.REGISTER_LIST) {
            RegisterListScreen(
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onCreateRegister = { navController.navigate(Routes.CREATE_REGISTER) },
                onRegisterClick = { id -> navController.navigate(Routes.registerBook(id)) }
            )
        }
        composable(Routes.CREATE_REGISTER) {
            CreateRegisterScreen(
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onRegisterCreated = { id ->
                    navController.popBackStack()
                    navController.navigate(Routes.registerBook(id))
                }
            )
        }
        composable(route = Routes.REGISTER_BOOK, arguments = registerArg) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            RegisterBookScreen(
                registerId = registerId,
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onAddPatient = { navController.navigate(Routes.entry(registerId, 0L)) },
                onOpenEntry = { entryId -> navController.navigate(Routes.entry(registerId, entryId)) },
                onReport = { navController.navigate(Routes.registerReport(registerId)) },
                onReportSetup = { navController.navigate(Routes.reportSetup(registerId)) },
                onEditRegister = { navController.navigate(Routes.editRegister(registerId)) }
            )
        }
        composable(
            route = Routes.ENTRY,
            arguments = listOf(
                navArgument("registerId") { type = NavType.LongType },
                navArgument("entryId") { type = NavType.LongType }
            )
        ) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            val entryId = be.arguments?.getLong("entryId") ?: 0L
            PatientEntryScreen(
                registerId = registerId,
                entryId = entryId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Routes.REGISTER_REPORT, arguments = registerArg) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            RegisterReportRoute(
                registerId = registerId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onSetupReport = { id -> navController.navigate(Routes.reportSetup(id)) }
            )
        }
        composable(route = Routes.REPORT_SETUP, arguments = registerArg) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            ReportSetupScreen(
                registerId = registerId,
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Routes.REGISTER_DETAIL, arguments = registerArg) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            RegisterDetailScreen(
                registerId = registerId,
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.editRegister(id)) }
            )
        }
        composable(route = Routes.EDIT_REGISTER, arguments = registerArg) { be ->
            val registerId = be.arguments?.getLong("registerId") ?: return@composable
            EditRegisterScreen(
                registerId = registerId,
                registerRepository = registerRepository,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Routes.SESSION_LIST) {
            SessionListScreen(
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onCreateSession = { navController.navigate(Routes.CREATE_SESSION) },
                onSessionClick = { id -> navController.navigate(Routes.sessionDetail(id)) }
            )
        }
        composable(Routes.CREATE_SESSION) {
            CreateSessionScreen(
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onSessionCreated = { id ->
                    navController.popBackStack()
                    navController.navigate(Routes.sessionDetail(id))
                }
            )
        }
        composable(route = Routes.SESSION_DETAIL, arguments = sessionArg) { be ->
            val sessionId = be.arguments?.getLong("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onOpenSummary = { id -> navController.navigate(Routes.sessionReport(id)) }
            )
        }
        composable(route = Routes.SESSION_SUMMARY, arguments = sessionArg) { be ->
            val sessionId = be.arguments?.getLong("sessionId") ?: return@composable
            SessionSummaryScreen(
                sessionId = sessionId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Routes.SESSION_REPORT, arguments = sessionArg) { be ->
            val sessionId = be.arguments?.getLong("sessionId") ?: return@composable
            ReportResultScreen(
                sessionId = sessionId,
                registerRepository = registerRepository,
                sessionRepository = sessionRepository,
                onBack = { navController.popBackStack() },
                onSetupReport = { id -> navController.navigate(Routes.reportSetup(id)) }
            )
        }
    }
}
