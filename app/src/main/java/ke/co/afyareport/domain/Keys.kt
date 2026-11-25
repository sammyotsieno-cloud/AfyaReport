package ke.co.afyareport.domain

data class Choice(val code: String, val label: String)

fun List<Choice>.labelOf(code: String): String = firstOrNull { it.code == code }?.label ?: code

/** Every key printed on the MOH 222, as fixed choices. */
object Keys {
    val sex = listOf(Choice("M", "Male"), Choice("F", "Female"), Choice("O", "Others"))

    val condition = listOf(
        Choice("HTN", "Hypertension only"),
        Choice("DM", "Diabetes only"),
        Choice("BOTH", "Both")
    )

    /** Key 2 */
    val status = listOf(Choice("a", "Newly diagnosed"), Choice("b", "Previously known"))

    /** Key 3 */
    val diagnosis = listOf(
        Choice("a", "Type 1 diabetes"),
        Choice("b", "Type 2 diabetes"),
        Choice("c", "Gestational diabetes"),
        Choice("d", "Diabetes from other causes"),
        Choice("e", "Hypertension"),
        Choice("f", "Pre-eclampsia")
    )

    fun diagnosisFor(condition: String): List<Choice> = when (condition) {
        "HTN" -> diagnosis.filter { it.code == "e" || it.code == "f" }
        "DM", "BOTH" -> diagnosis.filter { it.code in listOf("a", "b", "c", "d") }
        else -> diagnosis
    }

    /** Key 1 */
    val bmiBands = listOf(
        Choice("a", "18.4 or less"),
        Choice("b", "18.5 to 24.9"),
        Choice("c", "25 to 29.9"),
        Choice("d", "30 or more")
    )

    /** Key 4: letter + 1 (new) or 2 (pre-existing). d (stroke) and f (heart failure) are added by the app. */
    val complicationKinds = listOf(
        Choice("a", "Retinopathy"),
        Choice("b", "Peripheral neuropathy"),
        Choice("c", "Nephropathy / kidney disease"),
        Choice("d", "Stroke"),
        Choice("e", "Ischaemic heart disease"),
        Choice("f", "Heart failure"),
        Choice("g", "Peripheral vascular disease"),
        Choice("h", "Erectile dysfunction"),
        Choice("j", "Cataracts"),
        Choice("k", "Dental complication"),
        Choice("l", "Dyslipidemia"),
        Choice("m", "Obesity"),
        Choice("n", "HIV"),
        Choice("o", "TB")
    )
    val addedByApp = setOf("d", "f")

    /** Key 5 */
    val treatment = listOf(
        Choice("a", "Diet and physical activity"),
        Choice("b", "Oral glucose-lowering agents (OGLAs)"),
        Choice("c", "Insulin and OGLAs"),
        Choice("d", "Insulin"),
        Choice("e", "Antihypertensive"),
        Choice("f", "Herbal"),
        Choice("g", "Other")
    )

    val yesNo = listOf(Choice("Y", "Yes"), Choice("N", "No"))
    val diabeticFoot = listOf(Choice("NEW", "New"), Choice("KNOWN", "Known"), Choice("NIL", "Nil"))

    /** Key 6-A / 6-B */
    val footRisk = listOf(Choice("A", "Low risk (6-A)"), Choice("B", "High risk (6-B)"))
    val footRiskFactors = listOf(
        Choice("i", "Loss of protective sensation"),
        Choice("ii", "Absent pedal pulses"),
        Choice("iii", "Foot deformity"),
        Choice("iv", "History of foot ulcer"),
        Choice("v", "Prior amputation")
    )

    /** Key 7 */
    val footOutcome = listOf(
        Choice("a", "Ulcer healed"),
        Choice("b", "Surgical debridement"),
        Choice("c", "Amputation"),
        Choice("d", "Loss to follow-up"),
        Choice("e", "Admitted"),
        Choice("f", "Referred")
    )

    /** Key 8 */
    val tbStatus = listOf(
        Choice("1", "Presumed TB"),
        Choice("2", "No signs"),
        Choice("3", "On TB treatment")
    )
}
