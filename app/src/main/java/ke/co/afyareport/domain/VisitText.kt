package ke.co.afyareport.domain

import ke.co.afyareport.data.VisitEntity

data class InfoSectionData(val title: String, val rows: List<Pair<String, String>>)

private fun rowsOf(vararg items: Pair<String, String?>): List<Pair<String, String>> =
    items.mapNotNull { (k, v) -> if (v.isNullOrBlank()) null else k to v }

private fun List<String>.codes(keys: List<Choice>): String =
    sorted().joinToString("\n") { "$it – ${keys.labelOf(it)}" }

/** A visit written out in plain words, grouped for the visit summary. Empty rows are left out. */
fun visitSections(v: VisitEntity): List<InfoSectionData> {
    val sections = mutableListOf<InfoSectionData>()
    fun add(title: String, rows: List<Pair<String, String>>) {
        if (rows.isNotEmpty()) sections.add(InfoSectionData(title, rows))
    }

    add(
        "Patient", rowsOf(
            "Age" to v.age?.let { "$it years" },
            "Sex" to v.sex.takeIf { it.isNotBlank() }?.let { Keys.sex.labelOf(it) },
            "Visit for the month" to v.visitNo?.toString()
        )
    )

    val bmi = bmiOf(v.weightKg, v.heightCm)
    val highBp = (v.bpSys ?: 0) >= 140 || (v.bpDia ?: 0) >= 90
    add(
        "Measurements", rowsOf(
            "Weight" to v.weightKg?.let { "${fmtNum(it)} kg" },
            "Height" to v.heightCm?.let { "${fmtNum(it)} cm" },
            "BMI" to bmi?.let { b ->
                val band = bmiBand(b) ?: ""
                "$b  (Key 1: $band, ${Keys.bmiBands.labelOf(band)})"
            },
            "Waist" to v.waistCm?.let { "${fmtNum(it)} cm" },
            "Blood pressure" to (
                if (v.bpSys != null && v.bpDia != null) {
                    "${v.bpSys}/${v.bpDia} mmHg" + (if (highBp) "  (high)" else "")
                } else null
                )
        )
    )

    add(
        "Condition", rowsOf(
            "Condition" to v.condition.takeIf { it.isNotBlank() }?.let { Keys.condition.labelOf(it) },
            "Status" to v.status.takeIf { it.isNotBlank() }?.let { "$it – ${Keys.status.labelOf(it)}" },
            "Diagnosis" to v.diagnosis.takeIf { it.isNotBlank() }?.let { "$it – ${Keys.diagnosis.labelOf(it)}" }
        )
    )

    add(
        "Tests", rowsOf(
            "RBS" to v.rbs?.let { "${fmtNum(it)} mmol/L" },
            "FBS" to v.fbs?.let { "${fmtNum(it)} mmol/L" },
            "HbA1c" to v.hba1c?.let { "${fmtNum(it)} %" + (if (it < 7.0) "  (below the 7% target)" else "") }
        )
    )

    val treatment = csvSet(v.treatment).toList()
    val complications = csvSet(v.complications).sorted().mapNotNull { code ->
        if (code.length < 2) null
        else Keys.complicationKinds.labelOf(code.substring(0, 1)) +
            (if (code.endsWith("1")) " (new)" else " (pre-existing)") + "  [$code]"
    }
    add(
        "Treatment and complications", rowsOf(
            "Treatment" to treatment.codes(Keys.treatment).takeIf { treatment.isNotEmpty() },
            "Complications" to complications.joinToString("\n").takeIf { complications.isNotEmpty() }
        )
    )

    val factors = csvSet(v.footRiskFactors).toList()
    add(
        "Diabetic foot", rowsOf(
            "Screened" to v.footScreened.takeIf { it.isNotBlank() }?.let { Keys.yesNo.labelOf(it) },
            "Diabetic foot" to v.diabeticFoot.takeIf { it.isNotBlank() }?.let { Keys.diabeticFoot.labelOf(it) },
            "Foot risk" to v.footRisk.takeIf { it.isNotBlank() }?.let { Keys.footRisk.labelOf(it) },
            "Risk factors" to factors.joinToString("\n") { Keys.footRiskFactors.labelOf(it) }.takeIf { factors.isNotEmpty() },
            "Footcare outcome" to v.footOutcome.takeIf { it.isNotBlank() }?.let { "$it – ${Keys.footOutcome.labelOf(it)}" }
        )
    )

    add(
        "TB and insurance", rowsOf(
            "Screened for TB" to v.tbScreened.takeIf { it.isNotBlank() }?.let { Keys.yesNo.labelOf(it) },
            "TB status" to v.tbStatus.takeIf { it.isNotBlank() }?.let { "$it – ${Keys.tbStatus.labelOf(it)}" },
            "Active NHIF" to v.nhif.takeIf { it.isNotBlank() }?.let { Keys.yesNo.labelOf(it) }
        )
    )

    add("Remarks", rowsOf("Remarks" to v.remarks.takeIf { it.isNotBlank() }))
    return sections
}

/** The few lines shown at the top of a patient's summary, taken from their newest records. */
fun patientBrief(visits: List<VisitEntity>): List<Pair<String, String>> {
    if (visits.isEmpty()) return emptyList()
    val newestFirst = visits.sortedWith(compareByDescending<VisitEntity> { it.visitDate }.thenByDescending { it.id })
    fun <T : Any> latest(sel: (VisitEntity) -> T?): Pair<VisitEntity, T>? {
        for (v in newestFirst) {
            val x = sel(v)
            if (x != null) return v to x
        }
        return null
    }

    val rows = mutableListOf<Pair<String, String>>()
    val age = latest { it.age }?.second
    val sex = latest { it.sex.takeIf { s -> s.isNotBlank() } }?.second
    val ageSex = listOfNotNull(age?.let { "$it years" }, sex?.let { Keys.sex.labelOf(it) }).joinToString("  •  ")
    if (ageSex.isNotEmpty()) rows.add("Age and sex" to ageSex)

    val cond = latest { it.condition.takeIf { s -> s.isNotBlank() } }?.second
    val dx = latest { it.diagnosis.takeIf { s -> s.isNotBlank() } }?.second
    val condText = listOfNotNull(
        cond?.let { Keys.condition.labelOf(it) },
        dx?.let { Keys.diagnosis.labelOf(it) }
    ).joinToString("  •  ")
    if (condText.isNotEmpty()) rows.add("Condition" to condText)

    latest { v -> if (v.bpSys != null && v.bpDia != null) "${v.bpSys}/${v.bpDia} mmHg" else null }
        ?.let { (v, t) -> rows.add("Latest blood pressure" to "$t  (${v.visitDate.asDate().format(dayFormat)})") }
    latest { v -> bmiOf(v.weightKg, v.heightCm) }
        ?.let { (v, b) -> rows.add("Latest BMI" to "$b  (${v.visitDate.asDate().format(dayFormat)})") }
    latest { v -> v.hba1c }
        ?.let { (v, h) -> rows.add("Latest HbA1c" to "${fmtNum(h)} %  (${v.visitDate.asDate().format(dayFormat)})") }

    val treatment = latest { v -> v.treatment.takeIf { it.isNotBlank() } }?.second
    if (treatment != null) {
        rows.add("Treatment" to csvSet(treatment).sorted().joinToString("\n") { "$it – ${Keys.treatment.labelOf(it)}" })
    }
    latest { v -> v.nhif.takeIf { it.isNotBlank() } }
        ?.let { (_, n) -> rows.add("Active NHIF" to Keys.yesNo.labelOf(n)) }
    return rows
}
