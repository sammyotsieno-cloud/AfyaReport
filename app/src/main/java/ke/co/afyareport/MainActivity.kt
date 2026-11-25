package ke.co.afyareport

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.DraftStore
import ke.co.afyareport.domain.RegisterPrefs
import ke.co.afyareport.ui.backup.BackupScreen
import ke.co.afyareport.ui.book.BookScreen
import ke.co.afyareport.ui.collect.CollectScreen
import ke.co.afyareport.ui.home.HomeScreen
import ke.co.afyareport.ui.patients.PatientDetailScreen
import ke.co.afyareport.ui.patients.PatientsScreen
import ke.co.afyareport.ui.report.CountsScreen
import ke.co.afyareport.ui.report.ReportScreen
import ke.co.afyareport.ui.reports.ReportsScreen
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
                    AppNav(app.repo, app.prefs, app.drafts)
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("collect", "Data collection", Icons.Default.Edit),
    Tab("reports", "Reporting", Icons.Default.Assessment)
)

/** Which tab each screen belongs to. Screens not listed here (forms, patient page) hide the bar. */
private val tabOf = mapOf(
    "home" to "home", "backup" to "home",
    "collect" to "collect", "book" to "collect", "patients" to "collect", "settings" to "collect",
    "reports" to "reports", "report" to "reports", "counts" to "reports"
)

private val roots = setOf("home", "collect", "reports")

private fun isRootSwitch(a: String?, b: String?) = a in roots && b in roots

private fun visitRoute(entryId: Long, patientNo: String = "") =
    "visit/$entryId?patientNo=${Uri.encode(patientNo)}"

@Composable
private fun AppNav(repo: Repo, prefs: RegisterPrefs, drafts: DraftStore) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val selected = tabOf[route]
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selected != null) {
                Column {
                    HorizontalDivider(color = scheme.outlineVariant)
                    NavigationBar(containerColor = scheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = selected == t.route,
                                onClick = {
                                    if (selected == t.route) {
                                        if (route != t.route) nav.popBackStack(t.route, false)
                                    } else {
                                        nav.navigate(t.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = scheme.primary,
                                    selectedTextColor = scheme.primary,
                                    indicatorColor = scheme.primaryContainer,
                                    unselectedIconColor = scheme.onSurfaceVariant,
                                    unselectedTextColor = scheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
            enterTransition = {
                if (isRootSwitch(initialState.destination.route, targetState.destination.route)) {
                    fadeIn(tween(160))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(260)) +
                        fadeIn(tween(260))
                }
            },
            exitTransition = {
                if (isRootSwitch(initialState.destination.route, targetState.destination.route)) {
                    fadeOut(tween(100))
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left, tween(260),
                        targetOffset = { it / 5 }
                    ) + fadeOut(tween(200))
                }
            },
            popEnterTransition = {
                if (isRootSwitch(initialState.destination.route, targetState.destination.route)) {
                    fadeIn(tween(160))
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right, tween(260),
                        initialOffset = { it / 5 }
                    ) + fadeIn(tween(260))
                }
            },
            popExitTransition = {
                if (isRootSwitch(initialState.destination.route, targetState.destination.route)) {
                    fadeOut(tween(100))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(260)) +
                        fadeOut(tween(200))
                }
            }
        ) {
            composable("home") {
                HomeScreen(
                    repo = repo,
                    drafts = drafts,
                    onContinueDraft = { nav.navigate(visitRoute(0L)) },
                    onAddVisit = { nav.navigate(visitRoute(0L)) },
                    onReport = { nav.navigate("report") },
                    onBook = { nav.navigate("book") },
                    onBackup = { nav.navigate("backup") }
                )
            }
            composable("collect") {
                CollectScreen(
                    repo = repo,
                    onAddVisit = { nav.navigate(visitRoute(0L)) },
                    onBook = { nav.navigate("book") },
                    onPatients = { nav.navigate("patients") },
                    onSettings = { nav.navigate("settings") }
                )
            }
            composable("reports") {
                ReportsScreen(onOpen = { r -> nav.navigate(r) })
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
                    drafts = drafts,
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
            composable("report") {
                ReportScreen(
                    repo = repo,
                    onBack = { nav.popBackStack() },
                    onCounts = { nav.navigate("counts") }
                )
            }
            composable("counts") {
                CountsScreen(repo = repo, onBack = { nav.popBackStack() })
            }
            composable("backup") {
                BackupScreen(onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(prefs = prefs, onBack = { nav.popBackStack() })
            }
        }
    }
}
