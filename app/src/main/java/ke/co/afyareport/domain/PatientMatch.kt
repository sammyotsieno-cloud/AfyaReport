package ke.co.afyareport.domain

import ke.co.afyareport.data.PatientEntity

/** Patient numbers match ignoring case and spaces. */
fun patientKey(no: String): String = no.trim().lowercase().filter { !it.isWhitespace() }

private val chunks = Regex("\\d+|\\D+")

/** Natural order: 2 comes before 10, and 045/26 sorts by its number parts. */
fun naturalCompare(a: String, b: String): Int {
    val x = chunks.findAll(a.lowercase()).map { it.value }.toList()
    val y = chunks.findAll(b.lowercase()).map { it.value }.toList()
    for (i in 0 until minOf(x.size, y.size)) {
        val p = x[i]
        val q = y[i]
        val c = if (p[0].isDigit() && q[0].isDigit()) {
            val pn = p.trimStart('0')
            val qn = q.trimStart('0')
            if (pn.length != qn.length) pn.length - qn.length else pn.compareTo(qn)
        } else {
            p.compareTo(q)
        }
        if (c != 0) return c
    }
    return x.size - y.size
}

/** True when the two keys differ by exactly one typo: one changed, added or dropped character, or two swapped. */
fun withinOneEdit(a: String, b: String): Boolean {
    if (a == b) return false
    if (kotlin.math.abs(a.length - b.length) > 1) return false
    if (a.length == b.length) {
        val diff = a.indices.filter { a[it] != b[it] }
        return diff.size == 1 ||
            (diff.size == 2 && diff[1] == diff[0] + 1 && a[diff[0]] == b[diff[1]] && a[diff[1]] == b[diff[0]])
    }
    val s = if (a.length < b.length) a else b
    val l = if (a.length < b.length) b else a
    var i = 0
    while (i < s.length && s[i] == l[i]) i++
    return s.substring(i) == l.substring(i + 1)
}

fun similarPatients(key: String, patients: List<PatientEntity>, max: Int = 3): List<PatientEntity> =
    if (key.length < 3) emptyList()
    else patients.filter { it.patientKey != key && withinOneEdit(key, it.patientKey) }.take(max)
