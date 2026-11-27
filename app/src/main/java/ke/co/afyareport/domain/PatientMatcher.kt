package ke.co.afyareport.domain

import ke.co.afyareport.data.PatientEntity
import ke.co.afyareport.data.VisitEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** What the app knows about one patient, gathered from their record and visits. */
data class PatientFacts(
    val patient: PatientEntity,
    val sex: String,
    val ageBase: Int?,
    val ageBaseDay: Long,
    val visitCount: Int,
    val lastVisitDay: Long?
) {
    /** The patient's age on a given date, worked out from the most recent age recorded for them. */
    fun ageOn(date: LocalDate): Int? = ageBase?.let {
        it + ChronoUnit.YEARS.between(LocalDate.ofEpochDay(ageBaseDay), date).toInt()
    }
}

enum class MatchLevel { LIKELY, POSSIBLE }

data class MatchEval(
    val score: Int,
    val reasons: List<String>,
    val conflicts: List<String>,
    val numberEvidence: Boolean,
    val nameEvidence: Boolean
)

data class Suggestion(
    val facts: PatientFacts,
    val score: Int,
    val level: MatchLevel,
    val reasons: List<String>,
    val conflicts: List<String>
)

/** The details a patient usually has, used to fill a new visit. */
data class Recurring(
    val sex: String,
    val age: Int?,
    val condition: String,
    val diagnosis: String,
    val treatment: Set<String>,
    val nhif: String,
    val heightCm: Double?
)

fun buildFacts(patients: List<PatientEntity>, visits: List<VisitEntity>): List<PatientFacts> {
    val byPatient = visits.groupBy { it.patientId }
    return patients.map { p ->
        val newest = byPatient[p.id].orEmpty()
            .sortedWith(compareByDescending<VisitEntity> { it.visitDate }.thenByDescending { it.id })
        val ageVisit = newest.firstOrNull { it.age != null }
        PatientFacts(
            patient = p,
            sex = newest.firstOrNull { it.sex.isNotBlank() }?.sex ?: p.sex,
            ageBase = ageVisit?.age ?: p.age,
            ageBaseDay = ageVisit?.visitDate ?: p.ageAsOf,
            visitCount = newest.size,
            lastVisitDay = newest.firstOrNull()?.visitDate
        )
    }
}

private fun tokens(s: String): List<String> =
    s.lowercase().split(Regex("[^\\p{L}]+")).filter { it.length >= 2 }

private fun tokenScore(a: String, b: String): Double = when {
    a == b -> 1.0
    a.length >= 4 && b.length >= 4 && withinOneEdit(a, b) -> 0.8
    a.length >= 3 && b.length >= 3 && (a.startsWith(b) || b.startsWith(a)) -> 0.6
    else -> 0.0
}

/** 1.0 for the same name, down to 0.0 for unrelated names, and -1.0 when either name is missing. */
fun nameSimilarity(typed: String, known: String): Double {
    val t = tokens(typed)
    val k = tokens(known)
    if (t.isEmpty() || k.isEmpty()) return -1.0
    val best = t.map { a -> k.maxOf { b -> tokenScore(a, b) } }
    return best.sum() / maxOf(t.size, k.size)
}

/**
 * Compares what is being typed with one existing patient. Matching evidence adds points, and
 * contradictions (another sex, a very different age, an unrelated name) take them away.
 */
fun evaluateMatch(
    c: PatientFacts,
    typedNo: String,
    typedName: String,
    typedAge: Int?,
    typedSex: String,
    on: LocalDate
): MatchEval {
    var score = 0
    val reasons = mutableListOf<String>()
    val conflicts = mutableListOf<String>()
    var numberEv = false
    var nameEv = false

    val key = patientKey(typedNo)
    val ck = c.patient.patientKey
    if (key.isNotEmpty()) {
        when {
            key == ck -> { score += 50; reasons += "same number"; numberEv = true }
            key.length >= 2 && withinOneEdit(key, ck) -> {
                score += 30; reasons += "number differs by one character"; numberEv = true
            }
            key.length >= 3 && key.toList().sorted() == ck.toList().sorted() -> {
                score += 20; reasons += "same digits in a different order"; numberEv = true
            }
        }
    }

    val ns = nameSimilarity(typedName, c.patient.name)
    when {
        ns >= 0.99 -> { score += 40; reasons += "same name"; nameEv = true }
        ns >= 0.5 -> { score += 25; reasons += "similar name"; nameEv = true }
        ns == 0.0 -> { score -= 30; conflicts += "a different name" }
    }

    if ((typedSex == "M" || typedSex == "F") && (c.sex == "M" || c.sex == "F")) {
        if (typedSex == c.sex) {
            score += 10; reasons += "same sex"
        } else {
            score -= 40; conflicts += "a different sex"
        }
    }

    val known = c.ageOn(on)
    if (typedAge != null && known != null) {
        val d = abs(known - typedAge)
        when {
            d <= 1 -> { score += 15; reasons += "same age" }
            d <= 3 -> { score += 5; reasons += "similar age" }
            d <= 6 -> { score -= 10; conflicts += "an age $d years apart" }
            else -> { score -= 35; conflicts += "an age $d years apart" }
        }
    }
    return MatchEval(score, reasons, conflicts, numberEv, nameEv)
}

/** Existing patients (other than an exact number match) that this entry may really belong to. */
fun suggestPatients(
    candidates: List<PatientFacts>,
    typedNo: String,
    typedName: String,
    typedAge: Int?,
    typedSex: String,
    on: LocalDate,
    max: Int = 3
): List<Suggestion> {
    val key = patientKey(typedNo)
    return candidates
        .filter { it.patient.patientKey != key }
        .mapNotNull { c ->
            val e = evaluateMatch(c, typedNo, typedName, typedAge, typedSex, on)
            if (!(e.numberEvidence || e.nameEvidence)) return@mapNotNull null
            val level = when {
                e.score >= 60 -> MatchLevel.LIKELY
                e.score >= 35 -> MatchLevel.POSSIBLE
                else -> return@mapNotNull null
            }
            Suggestion(c, e.score, level, e.reasons, e.conflicts)
        }
        .sortedByDescending { it.score }
        .take(max)
}

/** The most recurring value of each stable detail, so a returning patient can be filled in one tap. */
fun recurringProfile(patient: PatientEntity, visits: List<VisitEntity>, on: LocalDate): Recurring {
    val newest = visits.sortedWith(compareByDescending<VisitEntity> { it.visitDate }.thenByDescending { it.id })
    fun <T : Any> mode(sel: (VisitEntity) -> T?): T? =
        newest.mapNotNull(sel).groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val facts = buildFacts(listOf(patient), visits).first()
    return Recurring(
        sex = facts.sex,
        age = facts.ageOn(on),
        condition = mode { it.condition.takeIf { s -> s.isNotBlank() } } ?: "",
        diagnosis = mode { it.diagnosis.takeIf { s -> s.isNotBlank() } } ?: "",
        treatment = csvSet(mode { it.treatment.takeIf { s -> s.isNotBlank() } } ?: ""),
        nhif = mode { it.nhif.takeIf { s -> s.isNotBlank() } } ?: "",
        heightCm = newest.firstNotNullOfOrNull { it.heightCm }
    )
}
