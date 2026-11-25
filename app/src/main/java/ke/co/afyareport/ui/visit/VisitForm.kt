package ke.co.afyareport.ui.visit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ke.co.afyareport.data.PatientEntity
import ke.co.afyareport.data.VisitEntity
import ke.co.afyareport.domain.FormField
import ke.co.afyareport.domain.bmiOf
import ke.co.afyareport.domain.csvSet
import ke.co.afyareport.domain.fmtNum
import ke.co.afyareport.domain.num
import ke.co.afyareport.domain.patientKey
import ke.co.afyareport.domain.toCsv
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Everything typed on the visit form. Every field is observable, so typing redraws at once. */
class VisitForm {
    var date by mutableStateOf(LocalDate.now())
    var patientNo by mutableStateOf("")
    var name by mutableStateOf("")
    var age by mutableStateOf("")
    var sex by mutableStateOf("")
    var visitNo by mutableStateOf("")
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var waist by mutableStateOf("")
    var sys by mutableStateOf("")
    var dia by mutableStateOf("")
    var condition by mutableStateOf("")
    var status by mutableStateOf("")
    var rbs by mutableStateOf("")
    var fbs by mutableStateOf("")
    var hba1c by mutableStateOf("")
    var diagnosis by mutableStateOf("")
    val complications = mutableStateMapOf<String, String>()
    val treatment = mutableStateListOf<String>()
    var footScreened by mutableStateOf("")
    var diabeticFoot by mutableStateOf("")
    var footRisk by mutableStateOf("")
    val footFactors = mutableStateListOf<String>()
    var footOutcome by mutableStateOf("")
    var tbScreened by mutableStateOf("")
    var tbStatus by mutableStateOf("")
    var nhif by mutableStateOf("")
    var remarks by mutableStateOf("")

    val bmi: Double? get() = bmiOf(num(weight), num(height))
    val hasDiabetes: Boolean get() = condition == "DM" || condition == "BOTH"

    fun toggleComplication(kind: String, value: String) {
        if (complications[kind] == value) complications.remove(kind) else complications[kind] = value
    }

    /** Clears everything for the next patient but keeps the date. */
    fun reset() {
        patientNo = ""; name = ""; age = ""; sex = ""; visitNo = ""
        weight = ""; height = ""; waist = ""; sys = ""; dia = ""
        condition = ""; status = ""; rbs = ""; fbs = ""; hba1c = ""; diagnosis = ""
        complications.clear(); treatment.clear()
        footScreened = ""; diabeticFoot = ""; footRisk = ""; footFactors.clear(); footOutcome = ""
        tbScreened = ""; tbStatus = ""; nhif = ""; remarks = ""
    }

    fun load(v: VisitEntity, p: PatientEntity?) {
        date = LocalDate.ofEpochDay(v.visitDate)
        patientNo = p?.patientNo ?: ""
        age = v.age?.toString() ?: ""
        sex = v.sex
        visitNo = v.visitNo?.toString() ?: ""
        weight = v.weightKg?.let { fmtNum(it) } ?: ""
        height = v.heightCm?.let { fmtNum(it) } ?: ""
        waist = v.waistCm?.let { fmtNum(it) } ?: ""
        sys = v.bpSys?.toString() ?: ""
        dia = v.bpDia?.toString() ?: ""
        condition = v.condition
        status = v.status
        rbs = v.rbs?.let { fmtNum(it) } ?: ""
        fbs = v.fbs?.let { fmtNum(it) } ?: ""
        hba1c = v.hba1c?.let { fmtNum(it) } ?: ""
        diagnosis = v.diagnosis
        complications.clear()
        csvSet(v.complications).forEach { code ->
            if (code.length >= 2) complications[code.substring(0, 1)] = code.substring(1)
        }
        treatment.clear(); treatment.addAll(csvSet(v.treatment))
        footScreened = v.footScreened
        diabeticFoot = v.diabeticFoot
        footRisk = v.footRisk
        footFactors.clear(); footFactors.addAll(csvSet(v.footRiskFactors))
        footOutcome = v.footOutcome
        tbScreened = v.tbScreened
        tbStatus = v.tbStatus
        nhif = v.nhif
        remarks = v.remarks
    }

    fun toEntity(base: VisitEntity?): VisitEntity {
        val diabetic = hasDiabetes
        return (base ?: VisitEntity(patientId = 0, visitDate = 0)).copy(
            visitDate = date.toEpochDay(),
            age = age.trim().toIntOrNull(),
            sex = sex,
            visitNo = visitNo.trim().toIntOrNull(),
            weightKg = num(weight),
            heightCm = num(height),
            waistCm = num(waist),
            bpSys = sys.trim().toIntOrNull(),
            bpDia = dia.trim().toIntOrNull(),
            condition = condition,
            status = status,
            rbs = num(rbs),
            fbs = num(fbs),
            hba1c = num(hba1c),
            diagnosis = diagnosis,
            complications = toCsv(complications.map { (k, v) -> k + v }),
            treatment = toCsv(treatment),
            footScreened = if (diabetic) footScreened else "",
            diabeticFoot = if (diabetic && footScreened == "Y") diabeticFoot else "",
            footRisk = if (diabetic) footRisk else "",
            footRiskFactors = if (diabetic && footRisk == "B") toCsv(footFactors) else "",
            footOutcome = if (diabetic && (diabeticFoot == "NEW" || diabeticFoot == "KNOWN")) footOutcome else "",
            tbScreened = tbScreened,
            tbStatus = if (tbScreened == "Y") tbStatus else "",
            nhif = nhif,
            remarks = remarks.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /** True when anything has been typed or ticked (the date alone does not count). */
    fun hasContent(): Boolean =
        listOf(
            patientNo, name, age, sex, visitNo, weight, height, waist, sys, dia, condition, status,
            rbs, fbs, hba1c, diagnosis, footScreened, diabeticFoot, footRisk, footOutcome,
            tbScreened, tbStatus, nhif, remarks
        ).any { it.isNotBlank() } ||
            complications.isNotEmpty() || treatment.isNotEmpty() || footFactors.isNotEmpty()

    fun toJson(): String = JSONObject().apply {
        put("date", date.toEpochDay())
        put("patientNo", patientNo)
        put("name", name)
        put("age", age)
        put("sex", sex)
        put("visitNo", visitNo)
        put("weight", weight)
        put("height", height)
        put("waist", waist)
        put("sys", sys)
        put("dia", dia)
        put("condition", condition)
        put("status", status)
        put("rbs", rbs)
        put("fbs", fbs)
        put("hba1c", hba1c)
        put("diagnosis", diagnosis)
        put("complications", JSONObject(complications.toMap()))
        put("treatment", JSONArray(treatment.toList()))
        put("footScreened", footScreened)
        put("diabeticFoot", diabeticFoot)
        put("footRisk", footRisk)
        put("footFactors", JSONArray(footFactors.toList()))
        put("footOutcome", footOutcome)
        put("tbScreened", tbScreened)
        put("tbStatus", tbStatus)
        put("nhif", nhif)
        put("remarks", remarks)
    }.toString()

    fun fromJson(s: String) {
        val o = JSONObject(s)
        val d = LocalDate.ofEpochDay(o.optLong("date", LocalDate.now().toEpochDay()))
        date = if (d.isAfter(LocalDate.now())) LocalDate.now() else d
        patientNo = o.optString("patientNo")
        name = o.optString("name")
        age = o.optString("age")
        sex = o.optString("sex")
        visitNo = o.optString("visitNo")
        weight = o.optString("weight")
        height = o.optString("height")
        waist = o.optString("waist")
        sys = o.optString("sys")
        dia = o.optString("dia")
        condition = o.optString("condition")
        status = o.optString("status")
        rbs = o.optString("rbs")
        fbs = o.optString("fbs")
        hba1c = o.optString("hba1c")
        diagnosis = o.optString("diagnosis")
        footScreened = o.optString("footScreened")
        diabeticFoot = o.optString("diabeticFoot")
        footRisk = o.optString("footRisk")
        footOutcome = o.optString("footOutcome")
        tbScreened = o.optString("tbScreened")
        tbStatus = o.optString("tbStatus")
        nhif = o.optString("nhif")
        remarks = o.optString("remarks")
        complications.clear()
        o.optJSONObject("complications")?.let { c -> c.keys().forEach { k -> complications[k] = c.optString(k) } }
        treatment.clear()
        o.optJSONArray("treatment")?.let { a -> for (i in 0 until a.length()) treatment.add(a.getString(i)) }
        footFactors.clear()
        o.optJSONArray("footFactors")?.let { a -> for (i in 0 until a.length()) footFactors.add(a.getString(i)) }
    }

    fun validate(required: Set<FormField>): Map<FormField, String> {
        val e = mutableMapOf<FormField, String>()
        fun need(f: FormField, missing: Boolean) {
            if (f in required && missing) e[f] = "Required"
        }
        fun range(f: FormField, s: String, lo: Double, hi: Double, msg: String) {
            need(f, s.isBlank())
            if (s.isNotBlank()) {
                val v = num(s)
                if (v == null || v < lo || v > hi) e[f] = msg
            }
        }

        if (date.isAfter(LocalDate.now())) e[FormField.DATE] = "This date is in the future"
        need(FormField.PATIENT_NO, patientKey(patientNo).isEmpty())
        if (patientKey(patientNo).length > 30) e[FormField.PATIENT_NO] = "Too long"

        need(FormField.AGE, age.isBlank())
        if (age.isNotBlank() && age.trim().toIntOrNull()?.let { it in 0..120 } != true) {
            e[FormField.AGE] = "Enter 0 to 120"
        }
        need(FormField.SEX, sex.isBlank())
        need(FormField.VISIT_NO, visitNo.isBlank())
        if (visitNo.isNotBlank() && visitNo.trim().toIntOrNull()?.let { it in 1..99 } != true) {
            e[FormField.VISIT_NO] = "Enter 1 to 99"
        }

        range(FormField.WEIGHT, weight, 1.0, 300.0, "Enter 1 to 300 kg")
        range(FormField.HEIGHT, height, 30.0, 250.0, "Enter 30 to 250 cm")
        range(FormField.WAIST, waist, 20.0, 250.0, "Enter 20 to 250 cm")

        need(FormField.BP, sys.isBlank() && dia.isBlank())
        if (sys.isNotBlank() || dia.isNotBlank()) {
            val s = sys.trim().toIntOrNull()
            val d = dia.trim().toIntOrNull()
            val msg = when {
                s == null || d == null -> "Fill in both numbers"
                s !in 50..300 || d !in 30..200 -> "Check the numbers (top 50 to 300, bottom 30 to 200)"
                s <= d -> "The top number must be higher than the bottom"
                else -> null
            }
            if (msg != null) e[FormField.BP] = msg
        }

        need(FormField.CONDITION, condition.isBlank())
        need(FormField.STATUS, status.isBlank())

        need(FormField.BLOOD_SUGAR, rbs.isBlank() && fbs.isBlank())
        listOf(rbs, fbs).forEach { s ->
            if (s.isNotBlank()) {
                val v = num(s)
                if (v == null || v < 0.5 || v > 60.0) e[FormField.BLOOD_SUGAR] = "Enter 0.5 to 60 mmol/L"
            }
        }
        range(FormField.HBA1C, hba1c, 3.0, 20.0, "Enter 3 to 20 %")

        need(FormField.DIAGNOSIS, diagnosis.isBlank())
        need(FormField.TREATMENT, treatment.isEmpty())

        if (hasDiabetes) {
            need(FormField.FOOT_SCREENING, footScreened.isBlank())
            need(FormField.FOOT_RISK, footRisk.isBlank())
        }
        need(FormField.TB_SCREENING, tbScreened.isBlank())
        if (tbScreened == "Y" && tbStatus.isBlank()) e[FormField.TB_SCREENING] = "Choose the TB status"
        need(FormField.NHIF, nhif.isBlank())
        return e
    }
}
