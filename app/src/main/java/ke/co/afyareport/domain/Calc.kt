package ke.co.afyareport.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
val shortDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

fun Long.asDate(): LocalDate = LocalDate.ofEpochDay(this)

fun num(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

fun fmtNum(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

/** BMI rounded to one decimal. */
fun bmiOf(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val m = heightCm / 100.0
    return Math.round(weightKg / (m * m) * 10.0) / 10.0
}

/** Key 1 letter for a BMI. */
fun bmiBand(bmi: Double?): String? = when {
    bmi == null -> null
    bmi <= 18.4 -> "a"
    bmi <= 24.9 -> "b"
    bmi <= 29.9 -> "c"
    else -> "d"
}

fun csvSet(s: String): Set<String> = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

fun toCsv(items: Collection<String>): String = items.toSortedSet().joinToString(",")
