package ke.co.afyareport.engine

import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.VisitType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity
import org.json.JSONArray
import org.json.JSONObject

const val OP_ONE_OF = "ONE_OF"
const val OP_AT_LEAST = "AT_LEAST"
const val OP_AT_MOST = "AT_MOST"
const val OP_BETWEEN = "BETWEEN"
const val OP_YES = "YES"
const val OP_NO = "NO"
const val OP_FILLED = "FILLED"

/** One condition: a column, what to test, and the codes/numbers to test against. */
data class Clause(val column: String, val op: String, val values: List<String> = emptyList())

object ClauseJson {
    fun encode(list: List<Clause>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().put("c", c.column).put("o", c.op).put("v", JSONArray(c.values)))
        }
        return arr.toString()
    }

    fun decode(text: String): List<Clause> = try {
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val v = o.getJSONArray("v")
            Clause(o.getString("c"), o.getString("o"), (0 until v.length()).map { v.getString(it) })
        }
    } catch (e: Exception) {
        emptyList()
    }
}

data class EntryData(
    val entryId: Long,
    val patientKey: String?,
    val visitDate: Long,
    val values: Map<String, String>
)

data class ReportRow(
    val section: String,
    val label: String,
    val rule: String,
    val male: Int,
    val female: Int,
    val total: Int,
    val isBand: Boolean = false,
    val problem: String? = null
)

data class ReportResult(
    val rows: List<ReportRow>,
    val notes: List<String>,
    val visits: Int = 0,
    val patients: Int = 0
)

private data class Band(val label: String, val lo: Double, val hi: Double)

private fun norm(s: String?): String = s?.trim()?.lowercase().orEmpty()

private fun unitKey(e: EntryData): String =
    e.patientKey?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "#${e.entryId}"

object ReportEngine {

    /**
     * Turns every row of the register into column-name -> value maps.
     * First visit or revisit is worked out from the patient number across the WHOLE register,
     * unless the row was set by hand.
     */
    fun buildEntries(
        entries: List<PatientEntryEntity>,
        valuesByEntry: Map<Long, List<VariableValueEntity>>,
        variables: List<VariableDefinitionEntity>
    ): List<EntryData> {
        val nameById = variables.associate { it.id to norm(it.name) }
        val visitNames = variables.filter { it.type == VariableType.VISIT_TYPE }.map { norm(it.name) }.distinct()
        val seen = mutableSetOf<String>()
        return entries
            .sortedWith(compareBy<PatientEntryEntity>({ it.visitDate }, { it.id }))
            .map { e ->
                val key = e.patientKey?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
                val firstTime = key != null && seen.add(key)
                val effective = when {
                    e.visitTypeLocked || key == null -> e.visitType
                    firstTime -> VisitType.NEW
                    else -> VisitType.RETURN
                }
                val map = mutableMapOf<String, String>()
                valuesByEntry[e.id].orEmpty().forEach { v ->
                    nameById[v.variableDefinitionId]?.let { map[it] = v.value }
                }
                visitNames.forEach { map[it] = effective.name }
                EntryData(e.id, e.patientKey, e.visitDate, map)
            }
    }

    /** [from] and [to] are epoch millis, both inclusive. */
    fun generate(
        indicators: List<ReportIndicatorEntity>,
        settings: ReportSettingsEntity,
        variables: List<VariableDefinitionEntity>,
        codeOptions: Map<Long, List<CodeOptionEntity>>,
        entries: List<EntryData>,
        from: Long,
        to: Long
    ): ReportResult {
        val varByName = variables.associateBy { norm(it.name) }
        val inPeriod = entries.filter { it.visitDate in from..to }
        val upToEnd = entries.filter { it.visitDate <= to }
        val notes = mutableListOf<String>()
        if (settings.sexColumn.isBlank()) {
            notes += "No sex column is chosen in report setup, so only totals are shown."
        } else if (!varByName.containsKey(norm(settings.sexColumn))) {
            notes += "The sex column \"${settings.sexColumn}\" is not in this register."
        }

        val rows = mutableListOf<ReportRow>()
        indicators.forEach { ind ->
            val clauses = ClauseJson.decode(ind.conditionsJson)
            val rule = describe(ind, clauses, varByName, codeOptions)
            val missing = clauses.firstOrNull { !varByName.containsKey(norm(it.column)) }
            if (missing != null) {
                rows += ReportRow(
                    ind.section, ind.label, rule, 0, 0, 0,
                    problem = "The column \"${missing.column}\" is not in this register"
                )
                return@forEach
            }
            val pool = if (ind.scope == ReportIndicatorEntity.SCOPE_CUMULATIVE) upToEnd else inPeriod
            val matched = pool.filter { evaluate(clauses, ind.matchAny, it) }
            val (m, f, t) = tally(matched, ind.countMode, settings)
            rows += ReportRow(ind.section, ind.label, rule, m, f, t)

            val bands = parseBands(ind.ageBands)
            if (bands.isNotEmpty()) {
                if (settings.ageColumn.isBlank()) {
                    notes += "\"${ind.label}\" has age groups but no age column is chosen in report setup."
                } else {
                    bands.forEach { b ->
                        val inBand = matched.filter { e ->
                            val a = e.values[norm(settings.ageColumn)]?.trim()?.toDoubleOrNull()
                            a != null && a >= b.lo && a <= b.hi
                        }
                        val (bm, bf, bt) = tally(inBand, ind.countMode, settings)
                        rows += ReportRow(ind.section, b.label, "", bm, bf, bt, isBand = true)
                    }
                }
            }
        }
        return ReportResult(
            rows = rows,
            notes = notes.distinct(),
            visits = inPeriod.size,
            patients = inPeriod.map { unitKey(it) }.distinct().size
        )
    }

    fun describeRule(
        ind: ReportIndicatorEntity,
        variables: List<VariableDefinitionEntity>,
        codeOptions: Map<Long, List<CodeOptionEntity>>
    ): String = describe(
        ind, ClauseJson.decode(ind.conditionsJson),
        variables.associateBy { norm(it.name) }, codeOptions
    )

    private fun evaluate(clauses: List<Clause>, matchAny: Boolean, e: EntryData): Boolean {
        if (clauses.isEmpty()) return true
        val results = clauses.map { test(it, e.values[norm(it.column)]) }
        return if (matchAny) results.any { it } else results.all { it }
    }

    private fun test(c: Clause, raw: String?): Boolean {
        val v = raw?.trim().orEmpty()
        val n = v.toDoubleOrNull()
        val a = c.values.getOrNull(0)?.trim()?.toDoubleOrNull()
        val b = c.values.getOrNull(1)?.trim()?.toDoubleOrNull()
        return when (c.op) {
            OP_ONE_OF -> v.isNotEmpty() && c.values.any { it.trim().equals(v, ignoreCase = true) }
            OP_AT_LEAST -> n != null && a != null && n >= a
            OP_AT_MOST -> n != null && a != null && n <= a
            OP_BETWEEN -> n != null && a != null && b != null && n >= a && n <= b
            OP_YES -> v.equals("true", ignoreCase = true)
            OP_NO -> v.equals("false", ignoreCase = true)
            OP_FILLED -> v.isNotEmpty()
            else -> false
        }
    }

    /** Male, female, total. Patients are counted once each; visits count every row. */
    private fun tally(
        rows: List<EntryData>,
        mode: String,
        s: ReportSettingsEntity
    ): Triple<Int, Int, Int> {
        val units: List<EntryData> = if (mode == ReportIndicatorEntity.COUNT_VISITS) {
            rows
        } else {
            rows.groupBy { unitKey(it) }.values.map { it.first() }
        }
        var male = 0
        var female = 0
        val sexKey = norm(s.sexColumn)
        units.forEach { u ->
            val sx = u.values[sexKey]?.trim().orEmpty()
            if (s.sexColumn.isNotBlank() && s.maleValue.isNotBlank() && sx.equals(s.maleValue.trim(), true)) male++
            else if (s.sexColumn.isNotBlank() && s.femaleValue.isNotBlank() && sx.equals(s.femaleValue.trim(), true)) female++
        }
        return Triple(male, female, units.size)
    }

    private fun parseBands(text: String): List<Band> = text.split(",").mapNotNull { raw ->
        val t = raw.trim()
        when {
            t.isEmpty() -> null
            t.endsWith("+") ->
                t.dropLast(1).trim().toDoubleOrNull()?.let { Band("$t years", it, Double.MAX_VALUE) }
            else -> {
                val p = t.split("-")
                val lo = p.getOrNull(0)?.trim()?.toDoubleOrNull()
                val hi = p.getOrNull(1)?.trim()?.toDoubleOrNull()
                if (p.size == 2 && lo != null && hi != null) Band("$t years", lo, hi) else null
            }
        }
    }

    private fun describe(
        ind: ReportIndicatorEntity,
        clauses: List<Clause>,
        varByName: Map<String, VariableDefinitionEntity>,
        codeOptions: Map<Long, List<CodeOptionEntity>>
    ): String {
        val unit = if (ind.countMode == ReportIndicatorEntity.COUNT_VISITS) "visits" else "patients"
        val where = if (ind.scope == ReportIndicatorEntity.SCOPE_CUMULATIVE) "up to the end date" else "in the report dates"
        if (clauses.isEmpty()) return "Counts all $unit recorded $where"
        val joiner = if (ind.matchAny) " or " else " and "
        val body = clauses.joinToString(joiner) { describeClause(it, varByName[norm(it.column)], codeOptions) }
        return "Counts $unit where $body ($where)"
    }

    /** Reads the column's key so codes are shown with their meanings. */
    private fun describeClause(
        c: Clause,
        v: VariableDefinitionEntity?,
        codeOptions: Map<Long, List<CodeOptionEntity>>
    ): String {
        val name = v?.name ?: c.column
        val options = v?.let { codeOptions[it.id].orEmpty() }.orEmpty()
        fun pretty(code: String): String = when {
            v?.type == VariableType.VISIT_TYPE -> if (code.equals("NEW", true)) "First visit" else "Revisit"
            else -> options.firstOrNull { it.code.equals(code, true) }
                ?.let { "${it.code} (${it.label})" } ?: code
        }
        val a = c.values.getOrNull(0).orEmpty()
        val b = c.values.getOrNull(1).orEmpty()
        return when (c.op) {
            OP_ONE_OF -> "$name is " + c.values.joinToString(" or ") { pretty(it) }
            OP_AT_LEAST -> "$name is $a or more"
            OP_AT_MOST -> "$name is $a or less"
            OP_BETWEEN -> "$name is between $a and $b"
            OP_YES -> "$name is Yes"
            OP_NO -> "$name is No"
            OP_FILLED -> "$name is filled in"
            else -> name
        }
    }
}
