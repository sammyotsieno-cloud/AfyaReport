package ke.co.afyareport.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.co.afyareport.data.MonthCountsEntity
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.monthFormat
import ke.co.afyareport.ui.common.NumberField
import ke.co.afyareport.ui.common.MonthSelector
import ke.co.afyareport.ui.theme.AfyaTopBar
import ke.co.afyareport.ui.theme.Card
import kotlinx.coroutines.launch
import java.time.YearMonth

private val countLines = listOf(
    "dka" to "Admitted with DKA",
    "hypo" to "Admitted with hypoglycemia",
    "stroke" to "Admitted with stroke",
    "htnEmerg" to "Admitted with hypertension urgency / emergency",
    "deathDm" to "Deaths due to diabetes complications",
    "deathHtn" to "Deaths due to hypertension complications"
)

/** The 740 lines the register cannot supply: typed in once a month, by sex. Saved as you type. */
@Composable
fun CountsScreen(repo: Repo, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val f = remember { mutableStateMapOf<String, String>() }

    fun n(k: String): Int = f[k]?.trim()?.toIntOrNull()?.coerceIn(0, 9999) ?: 0

    fun persist() {
        val e = MonthCountsEntity(
            id = month.year * 100 + month.monthValue,
            dkaM = n("dkaM"), dkaF = n("dkaF"),
            hypoM = n("hypoM"), hypoF = n("hypoF"),
            strokeM = n("strokeM"), strokeF = n("strokeF"),
            htnEmergM = n("htnEmergM"), htnEmergF = n("htnEmergF"),
            deathDmM = n("deathDmM"), deathDmF = n("deathDmF"),
            deathHtnM = n("deathHtnM"), deathHtnF = n("deathHtnF")
        )
        scope.launch { repo.dao.saveCounts(e) }
    }

    LaunchedEffect(month) {
        val c = repo.dao.getCounts(month.year * 100 + month.monthValue)
        f.clear()
        fun put(k: String, v: Int?) { if (v != null && v != 0) f[k] = v.toString() }
        if (c != null) {
            put("dkaM", c.dkaM); put("dkaF", c.dkaF)
            put("hypoM", c.hypoM); put("hypoF", c.hypoF)
            put("strokeM", c.strokeM); put("strokeF", c.strokeF)
            put("htnEmergM", c.htnEmergM); put("htnEmergF", c.htnEmergF)
            put("deathDmM", c.deathDmM); put("deathDmF", c.deathDmF)
            put("deathHtnM", c.deathHtnM); put("deathHtnF", c.deathHtnF)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AfyaTopBar(title = "Admissions and deaths", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "These lines come from your inpatient records, not from the register. Type the month's counts for males and females. Leave blank for none. It saves as you type.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MonthSelector(month = month, onChange = { month = it })
            countLines.forEach { (key, label) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberField(
                                label = "Male",
                                value = f[key + "M"] ?: "",
                                onChange = { f[key + "M"] = it.filter { ch -> ch.isDigit() }.take(4); persist() },
                                decimal = false,
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                label = "Female",
                                value = f[key + "F"] ?: "",
                                onChange = { f[key + "F"] = it.filter { ch -> ch.isDigit() }.take(4); persist() },
                                decimal = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "Total ${n(key + "M") + n(key + "F")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
