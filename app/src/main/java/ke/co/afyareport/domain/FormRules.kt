package ke.co.afyareport.domain

import android.content.Context

/** Every column that can be switched between compulsory and optional. */
enum class FormField(
    val label: String,
    val locked: Boolean = false,
    val defaultRequired: Boolean = false
) {
    DATE("Visit date", true, true),
    PATIENT_NO("Patient number", true, true),
    AGE("Age", false, true),
    SEX("Sex", false, true),
    VISIT_NO("Visit for the month"),
    WEIGHT("Weight"),
    HEIGHT("Height"),
    WAIST("Waist circumference"),
    BP("Blood pressure"),
    CONDITION("Hypertension, diabetes or both", false, true),
    STATUS("Newly diagnosed or previously known", false, true),
    BLOOD_SUGAR("Blood sugar"),
    HBA1C("HbA1c"),
    DIAGNOSIS("Diagnosis"),
    TREATMENT("Treatment"),
    FOOT_SCREENING("Diabetic foot screening"),
    FOOT_RISK("Foot risk assessment"),
    TB_SCREENING("Screened for TB"),
    NHIF("NHIF")
}

class RegisterPrefs(context: Context) {
    private val sp = context.getSharedPreferences("afyareport_dm", Context.MODE_PRIVATE)

    fun required(): Set<FormField> {
        val saved = sp.getStringSet("required", null)
        return if (saved == null) {
            FormField.entries.filter { it.defaultRequired }.toSet()
        } else {
            FormField.entries.filter { it.locked || it.name in saved }.toSet()
        }
    }

    fun setRequired(field: FormField, on: Boolean) {
        val cur = required().map { it.name }.toMutableSet()
        if (on) cur.add(field.name) else cur.remove(field.name)
        sp.edit().putStringSet("required", cur).apply()
    }

    /** The last visit date used, as an epoch day, so a month of old entries needs one date change. */
    fun lastDate(): Long? = if (sp.contains("last_date")) sp.getLong("last_date", 0L) else null

    fun setLastDate(epochDay: Long) {
        sp.edit().putLong("last_date", epochDay).apply()
    }
}
