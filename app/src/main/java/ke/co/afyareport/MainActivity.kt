package ke.co.afyareport

import android.net.Uri
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
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.RegisterPrefs
import ke.co.afyareport.ui.book.BookScreen
import ke.co.afyareport.ui.home.HomeScreen
import ke.co.afyareport.ui.patients.PatientDetailScreen
import ke.co.afyareport.ui.patients.PatientsScreen
import ke.co.afyareport.ui.settings.SettingsScreen
import ke.co.afyareport.ui.theme.AfyaTheme
import ke.co.afyareport.ui.visit.VisitFormScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AfyaReportApp
        setContent {
            AfyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav(app.repo, app.prefs)
                }
            }
        }
    }
}

private fun visitRoute(entryId: Long, patientNo: String = "") =
    "visit/$entryId?patientNo=${Uri.encode(patientNo)}"

@Composable
private fun AppNav(repo: Repo, prefs: RegisterPrefs) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                repo = repo,
                onAddVisit = { nav.navigate(visitRoute(0L)) },
                onBook = { nav.navigate("book") },
                onPatients = { nav.navigate("patients") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("book") {
            BookScreen(
                repo = repo,
                onBack = { nav.popBackStack() },
                onOpenVisit = { id -> nav.navigate(visitRoute(id)) },
                onAdd = { nav.navigate(visitRoute(0L)) }
            )
        }
        composable(
            route = "visit/{entryId}?patientNo={patientNo}",
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType },
                navArgument("patientNo") { type = NavType.StringType; defaultValue = "" }
            )
        ) { be ->
            VisitFormScreen(
                entryId = be.arguments?.getLong("entryId") ?: 0L,
                startPatientNo = be.arguments?.getString("patientNo") ?: "",
                repo = repo,
                prefs = prefs,
                onBack = { nav.popBackStack() }
            )
        }
        composable("patients") {
            PatientsScreen(
                repo = repo,
                onBack = { nav.popBackStack() },
                onOpenPatient = { id -> nav.navigate("patient/$id") }
            )
        }
        composable(
            route = "patient/{patientId}",
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { be ->
            PatientDetailScreen(
                patientId = be.arguments?.getLong("patientId") ?: 0L,
                repo = repo,
                onBack = { nav.popBackStack() },
                onOpenVisit = { id -> nav.navigate(visitRoute(id)) },
                onAddVisit = { no -> nav.navigate(visitRoute(0L, no)) }
            )
        }
        composable("settings") {
            SettingsScreen(prefs = prefs, onBack = { nav.popBackStack() })
        }
    }
}
