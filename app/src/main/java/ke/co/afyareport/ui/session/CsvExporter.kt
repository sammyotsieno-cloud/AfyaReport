package ke.co.afyareport.ui.session

import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity

fun buildSessionCsv(
    entries: List<PatientEntryEntity>,
    variables: List<VariableDefinitionEntity>,
    codeOptions: Map<Long, List<CodeOptionEntity>>,
    valuesByEntry: Map<Long, List<VariableValueEntity>>
): String {
    val exportVariables = variables.filter { it.type != VariableType.VISIT_TYPE }
    val headers = listOf("Entry#", "VisitType", "PatientKey") + exportVariables.map { it.name }
    return buildString {
        appendLine(headers.joinToString(",") { escapeCsv(it) })
        entries.forEach { entry ->
            val valueMap = valuesByEntry[entry.id].orEmpty().associateBy { it.variableDefinitionId }
            val row = mutableListOf(
                (entry.sortOrder + 1).toString(),
                entry.visitType.name,
                entry.patientKey.orEmpty()
            )
            exportVariables.forEach { variable ->
                val raw = valueMap[variable.id]?.value.orEmpty()
                val value = when (variable.type) {
                    VariableType.CODED -> {
                        val option = codeOptions[variable.id].orEmpty().firstOrNull { it.code == raw }
                        if (option == null) raw else "${option.code} – ${option.label}"
                    }
                    VariableType.BOOLEAN -> when (raw) {
                        "true" -> "Yes"
                        "false" -> "No"
                        else -> raw
                    }
                    else -> raw
                }
                row.add(value)
            }
            appendLine(row.joinToString(",") { escapeCsv(it) })
        }
    }
}

private fun escapeCsv(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
