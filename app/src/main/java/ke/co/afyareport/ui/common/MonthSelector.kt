@file:OptIn(ExperimentalLayoutApi::class)

package ke.co.afyareport.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val headerFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

/**
 * A collapsible month calendar. Collapsed it shows the month with arrows; tap it to open a 12-month grid,
 * and tap the year to pick any other year from a drop-down list. Months and years that have data show a dot.
 */
@Composable
fun MonthSelector(
    month: YearMonth,
    onChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    monthsWithData: Set<YearMonth> = emptySet(),
    label: String? = null
) {
    val now = remember { YearMonth.now() }
    var open by remember { mutableStateOf(false) }
    var yearMode by remember { mutableStateOf(false) }
    var shownYear by remember { mutableStateOf(month.year) }

    val minYear = minOf(2015, monthsWithData.minOfOrNull { it.year } ?: 2015, month.year)
    val years = (minYear..now.year).toList().reversed()
    val yearsWithData = monthsWithData.map { it.year }.toSet()
    val outline = MaterialTheme.colorScheme.outlineVariant

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, outline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            open = !open
                            if (open) {
                                shownYear = month.year
                                yearMode = false
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label ?: month.format(headerFormat), style = MaterialTheme.typography.titleMedium)
                    Icon(
                        if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (open) "Close calendar" else "Open calendar"
                    )
                }
                IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = month.isBefore(now)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }

        AnimatedVisibility(visible = open) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, outline)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { shownYear -= 1 }, enabled = shownYear > minYear) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                        }
                        TextButton(onClick = { yearMode = !yearMode }) {
                            Text(shownYear.toString(), style = MaterialTheme.typography.titleMedium)
                            Icon(
                                if (yearMode) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Choose a year"
                            )
                        }
                        IconButton(onClick = { shownYear += 1 }, enabled = shownYear < now.year) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
                        }
                    }
                    HorizontalDivider(color = outline)

                    if (yearMode) {
                        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                years.forEach { y ->
                                    FilterChip(
                                        selected = y == shownYear,
                                        onClick = { shownYear = y; yearMode = false },
                                        label = { Text(y.toString() + if (y in yearsWithData) " •" else "") }
                                    )
                                }
                            }
                        }
                    } else {
                        for (r in 0 until 4) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in 0 until 3) {
                                    val m = r * 3 + c + 1
                                    val ym = YearMonth.of(shownYear, m)
                                    val name = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                    FilterChip(
                                        selected = ym == month,
                                        enabled = !ym.isAfter(now),
                                        onClick = { onChange(ym); open = false },
                                        label = { Text(name + if (ym in monthsWithData) " •" else "") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "• has visits recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onChange(now); open = false }) { Text("This month") }
                        }
                    }
                }
            }
        }
    }
}
