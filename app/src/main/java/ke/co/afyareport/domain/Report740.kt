package ke.co.afyareport.domain

import ke.co.afyareport.data.MonthCountsEntity
import ke.co.afyareport.data.VisitEntity
import java.time.LocalDate
import java.time.YearMonth

enum class RowKind { SECTION, SUBHEAD, LINE, BAND }

data class Row740(
    val label: String,
    val male: String = "",
    val female: String = "",
    val total: String = "",
    val kind: RowKind = RowKind.LINE
)

data class Report740(val rows: List<Row740>, val notes: List<String>, val visits: Int, val patients: Int)

private data class AgeBand(val label: String, val lo: Int, val hi: Int)

private val type1Bands = listOf(
    AgeBand("0-5 years", 0, 5), AgeBand("6-9 years", 6, 9), AgeBand("10-19 years", 10, 19),
    AgeBand("20-35 years", 20, 35), AgeBand("36 years and above", 36, 200)
)
private val type2Bands = listOf(
    AgeBand("0-18 years", 0, 18), AgeBand("19-35 years", 19, 35),
    AgeBand("36-60 years", 36, 60), AgeBand("Over 60 years", 61, 200)
)
private val htnBands = listOf(
    AgeBand("0-9 years", 0, 9), AgeBand("10-19 years", 10, 19), AgeBand("20-35 years", 20, 35),
    AgeBand("36-60 years", 36, 60), AgeBand("Over 60 years", 61, 200)
)

private val visitOrder = compareBy<VisitEntity>({ it.visitDate }, { it.id })
private fun List<VisitEntity>.latest(): VisitEntity = maxWith(visitOrder)

/** Builds the MOH 740 for the dates [from]..[to] (epoch days, inclusive). Every line counts each patient once. */
fun buildReport740(
    visits: List<VisitEntity>,
    from: Long,
    to: Long,
    counts: List<MonthCountsEntity>
): Report740 {
    val upToEnd = visits.filter { it.visitDate <= to }
    val inPeriod = upToEnd.filter { it.visitDate >= from }

    val sexOf = upToEnd.filter { it.sex.isNotBlank() }
        .groupBy { it.patientId }.mapValues { it.value.latest().sex }
    val ageOf = upToEnd.filter { it.age != null }
        .groupBy { it.patientId }.mapValues { it.value.latest().age ?: 0 }
    val currentCondition = upToEnd.filter { it.condition.isNotBlank() }
        .groupBy { it.patientId }.mapValues { it.value.latest().condition }
    val latestInPeriod = inPeriod.groupBy { it.patientId }.mapValues { it.value.latest() }
    val treatmentOf = latestInPeriod.mapValues { csvSet(it.value.treatment) }

    val rows = mutableListOf<Row740>()
    val notes = mutableListOf<String>()
    val missingAge = mutableSetOf<Long>()

    fun pts(pred: (VisitEntity) -> Boolean): Set<Long> =
        inPeriod.filter(pred).map { it.patientId }.toSet()

    fun split(ids: Collection<Long>): Triple<Int, Int, Int> {
        var m = 0
        var f = 0
        ids.forEach { id ->
            when (sexOf[id]) {
                "M" -> m++
                "F" -> f++
                else -> Unit
            }
        }
        return Triple(m, f, ids.size)
    }

    fun line(label: String, ids: Collection<Long>, kind: RowKind = RowKind.LINE) {
        val (m, f, t) = split(ids)
        rows += Row740(label, m.toString(), f.toString(), t.toString(), kind)
    }

    fun section(label: String) { rows += Row740(label, kind = RowKind.SECTION) }
    fun head(label: String) { rows += Row740(label, kind = RowKind.SUBHEAD) }
    fun manual(label: String, m: Int, f: Int) {
        rows += Row740(label, m.toString(), f.toString(), (m + f).toString())
    }

    fun groupLines(cond: String, cumulative: String, newly: String, known: String) {
        val newP = pts { it.condition == cond && it.status == "a" }
        val knownP = pts { it.condition == cond && it.status == "b" } - newP
        line(cumulative, currentCondition.filterValues { it == cond }.keys)
        line(newly, newP)
        line(known, knownP)
    }

    fun bandLines(cond: String, dx: String, bands: List<AgeBand>) {
        val reps = inPeriod.filter { it.condition == cond && it.diagnosis == dx }
            .groupBy { it.patientId }.mapValues { it.value.latest() }
        val ages = reps.mapValues { (id, v) -> v.age ?: ageOf[id] }
        ages.forEach { (id, a) -> if (a == null) missingAge.add(id) }
        bands.forEach { b ->
            val ids = ages.filterValues { a -> a != null && a in b.lo..b.hi }.keys
            line(b.label, ids, RowKind.BAND)
        }
    }

    fun hasComp(v: VisitEntity, code: String) = code in csvSet(v.complications)

    section("DIABETES")
    groupLines(
        "DM",
        "Cumulative no. of diabetes patients in care",
        "No. of newly diagnosed diabetes cases",
        "Revisits to clinic / known diabetes"
    )
    head("Total no. with Type 1")
    bandLines("DM", "a", type1Bands)
    head("Total no. with Type 2")
    bandLines("DM", "b", type2Bands)
    val gdm = pts { it.condition == "DM" && it.diagnosis == "c" }
    val (gm, gf, gt) = split(gdm)
    if (gm > 0) notes += "$gm male patient(s) are recorded with gestational diabetes. Check those entries."
    rows += Row740("No. diagnosed for gestational diabetes", "N/A", gf.toString(), gt.toString())
    line("No. of diabetes secondary to other causes", pts { it.condition == "DM" && it.diagnosis == "d" })

    section("HYPERTENSION")
    groupLines(
        "HTN",
        "Cumulative no. of hypertension patients in care",
        "No. of newly diagnosed hypertension cases",
        "Revisits to clinic / known hypertension"
    )
    head("No. with hypertension")
    bandLines("HTN", "e", htnBands)

    section("CO-MORBID WITH BOTH DIABETES AND HYPERTENSION")
    groupLines(
        "BOTH",
        "Cumulative no. of co-morbid patients in care",
        "Newly diagnosed co-morbid cases",
        "Revisits to clinic / known co-morbid"
    )

    section("DIABETES TREATMENT AND FOLLOW UP")
    val both = treatmentOf.filterValues { "c" in it || ("b" in it && "d" in it) }.keys
    line(
        "No. of patients on insulin",
        treatmentOf.filterValues { "d" in it && "c" !in it && "b" !in it }.keys
    )
    line(
        "No. of patients on OGLAs",
        treatmentOf.filterValues { "b" in it && "c" !in it && "d" !in it }.keys
    )
    line("No. of patients on both (insulin and OGLAs)", both)
    line(
        "No. of patients on diet and exercise only",
        treatmentOf.filterValues { "a" in it && "b" !in it && "c" !in it && "d" !in it && "e" !in it }.keys
    )
    line("No. of patients done HbA1c", pts { it.hba1c != null })
    val metTarget = inPeriod.filter { it.hba1c != null }
        .groupBy { it.patientId }.mapValues { it.value.latest().hba1c ?: 99.0 }
        .filterValues { it < 7.0 }.keys
    line("No. that met HbA1c target (below 7%)", metTarget)

    section("HYPERTENSION TREATMENT")
    line("No. of patients on antihypertensives", treatmentOf.filterValues { "e" in it }.keys)
    line(
        "No. with high BP (140/90 or more) at clinic visit",
        pts { (it.bpSys ?: 0) >= 140 || (it.bpDia ?: 0) >= 90 }
    )

    section("NEW DIAGNOSIS OF COMPLICATIONS / CO-MORBIDITIES")
    line("Stroke", pts { hasComp(it, "d1") })
    line("Ischaemic heart disease", pts { hasComp(it, "e1") })
    line("Heart failure", pts { hasComp(it, "f1") })

    section("DIABETIC FOOT, TB AND NHIF")
    line("No. of patients screened for diabetic foot", pts { it.footScreened == "Y" })
    line("No. of patients with diabetic foot (new diagnosis)", pts { it.diabeticFoot == "NEW" })
    line("No. of amputations due to diabetic foot", pts { it.footOutcome == "c" })
    line("No. with kidney complications (new diagnosis)", pts { hasComp(it, "c1") })
    line("No. with diabetic retinopathy (new diagnosis)", pts { hasComp(it, "a1") })
    line("No. of patients with neuropathies (new diagnosis)", pts { hasComp(it, "b1") })
    line("No. screened for tuberculosis", pts { it.tbScreened == "Y" })
    line("No. screened positive for tuberculosis", pts { it.tbStatus == "1" })
    val nhifYes = inPeriod.filter { it.nhif.isNotBlank() }
        .groupBy { it.patientId }.mapValues { it.value.latest().nhif }
        .filterValues { it == "Y" }.keys
    line("No. enrolled with NHIF", nhifYes)

    section("ADMISSIONS AND DEATHS (typed in monthly)")
    val monthIds = mutableSetOf<Int>()
    var ym = YearMonth.from(LocalDate.ofEpochDay(from))
    val endYm = YearMonth.from(LocalDate.ofEpochDay(to))
    while (!ym.isAfter(endYm)) {
        monthIds.add(ym.year * 100 + ym.monthValue)
        ym = ym.plusMonths(1)
    }
    val c = counts.filter { it.id in monthIds }
    manual("No. admitted with DKA", c.sumOf { it.dkaM }, c.sumOf { it.dkaF })
    manual("No. admitted with hypoglycemia", c.sumOf { it.hypoM }, c.sumOf { it.hypoF })
    manual("No. admitted with stroke", c.sumOf { it.strokeM }, c.sumOf { it.strokeF })
    manual("No. admitted with hypertension urgency / emergency", c.sumOf { it.htnEmergM }, c.sumOf { it.htnEmergF })
    manual("Total deaths due to diabetes complications", c.sumOf { it.deathDmM }, c.sumOf { it.deathDmF })
    manual("Total deaths due to hypertension complications", c.sumOf { it.deathHtnM }, c.sumOf { it.deathHtnF })
    if (c.isEmpty()) {
        notes += "Nothing is typed in yet for admissions and deaths in these dates. Use the Admissions and deaths button."
    }

    val noSex = latestInPeriod.keys.count { sexOf[it] != "M" && sexOf[it] != "F" }
    if (noSex > 0) notes += "$noSex patient(s) have no male or female sex recorded, so they appear only in the Totals."
    if (missingAge.isNotEmpty()) {
        notes += "${missingAge.size} patient(s) have no age recorded, so they are missing from the age lines."
    }
    return Report740(rows, notes.distinct(), inPeriod.size, latestInPeriod.size)
}
