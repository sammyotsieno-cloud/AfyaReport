package ke.co.afyareport.data.backup

import ke.co.afyareport.data.local.VariableType
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.ReportRepository
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(val registerId: Long?, val error: String?)

/** A register's setup (columns, keys, report lines) as text that can be saved, shared or pasted. */
object RegisterTransfer {
    private const val FORMAT = "afyareport-register"

    suspend fun toJson(
        registerId: Long,
        registerRepository: RegisterRepository,
        reportRepo: ReportRepository
    ): String? {
        val reg = registerRepository.getRegister(registerId) ?: return null
        val vars = registerRepository.getVariablesForVersion(reg.id, reg.version)
            .filter { it.type != VariableType.VISIT_TYPE }
            .sortedBy { it.sortOrder }
        val cols = JSONArray()
        vars.forEach { v ->
            val key = JSONArray()
            if (v.type == VariableType.CODED) {
                registerRepository.getCodeOptions(v.id).forEach { o ->
                    key.put(JSONObject().put("code", o.code).put("label", o.label))
                }
            }
            cols.put(
                JSONObject()
                    .put("name", v.name)
                    .put("type", v.type.name)
                    .put("hint", v.measurementInstructions)
                    .put("required", v.isRequired)
                    .put("key", key)
            )
        }
        val s = reportRepo.getSettings(reg.id)
        val lines = JSONArray()
        reportRepo.getIndicators(reg.id).forEach { i ->
            lines.put(
                JSONObject()
                    .put("section", i.section)
                    .put("label", i.label)
                    .put("countMode", i.countMode)
                    .put("matchAny", i.matchAny)
                    .put("ageBands", i.ageBands)
                    .put("scope", i.scope)
                    .put("conditions", runCatching { JSONArray(i.conditionsJson) }.getOrDefault(JSONArray()))
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", 1)
            .put("name", reg.name)
            .put("description", reg.description)
            .put("columns", cols)
            .put("sexColumn", s.sexColumn)
            .put("maleValue", s.maleValue)
            .put("femaleValue", s.femaleValue)
            .put("ageColumn", s.ageColumn)
            .put("lines", lines)
            .toString(2)
    }

    suspend fun importJson(
        text: String,
        registerRepository: RegisterRepository,
        reportRepo: ReportRepository
    ): ImportResult {
        return try {
            val root = try {
                JSONObject(text.trim())
            } catch (e: Exception) {
                return ImportResult(null, "That is not a register setup. Check that you copied all of it.")
            }
            if (root.optString("format") != FORMAT) {
                return ImportResult(null, "That is not an AfyaReport register setup.")
            }
            val name = root.optString("name").trim()
            if (name.isEmpty()) return ImportResult(null, "The setup has no register name.")

            val prepared = mutableListOf<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>()
            prepared.add(
                VariableDefinitionEntity(
                    registerDefinitionId = 0,
                    registerVersion = 1,
                    name = "Visit Type",
                    type = VariableType.VISIT_TYPE,
                    measurementInstructions = "Select whether this is a new patient visit or a return visit",
                    isRequired = true,
                    sortOrder = 0
                ) to emptyList()
            )
            val cols = root.optJSONArray("columns") ?: JSONArray()
            for (i in 0 until cols.length()) {
                val c = cols.getJSONObject(i)
                val type = runCatching { VariableType.valueOf(c.optString("type")) }.getOrNull() ?: VariableType.TEXT
                if (type == VariableType.VISIT_TYPE) continue
                val options = mutableListOf<CodeOptionEntity>()
                if (type == VariableType.CODED) {
                    val key = c.optJSONArray("key") ?: JSONArray()
                    for (k in 0 until key.length()) {
                        val o = key.getJSONObject(k)
                        val code = o.optString("code").trim()
                        val label = o.optString("label").trim()
                        if (code.isNotEmpty() && label.isNotEmpty()) {
                            options.add(
                                CodeOptionEntity(variableDefinitionId = 0, code = code, label = label, sortOrder = options.size)
                            )
                        }
                    }
                }
                prepared.add(
                    VariableDefinitionEntity(
                        registerDefinitionId = 0,
                        registerVersion = 1,
                        name = c.optString("name").trim().ifBlank { "Untitled" },
                        type = type,
                        measurementInstructions = c.optString("hint").trim(),
                        isRequired = c.optBoolean("required", false),
                        sortOrder = prepared.size
                    ) to options
                )
            }

            val id = registerRepository.createRegister(
                name = name,
                description = root.optString("description"),
                variablesWithOptions = prepared
            )

            reportRepo.saveSettings(
                ReportSettingsEntity(
                    registerDefinitionId = id,
                    sexColumn = root.optString("sexColumn"),
                    maleValue = root.optString("maleValue"),
                    femaleValue = root.optString("femaleValue"),
                    ageColumn = root.optString("ageColumn")
                )
            )
            val lines = root.optJSONArray("lines") ?: JSONArray()
            for (i in 0 until lines.length()) {
                val l = lines.getJSONObject(i)
                val label = l.optString("label").trim()
                if (label.isEmpty()) continue
                val mode = l.optString("countMode")
                val scope = l.optString("scope")
                reportRepo.saveIndicator(
                    ReportIndicatorEntity(
                        registerDefinitionId = id,
                        section = l.optString("section").trim(),
                        label = label,
                        countMode = if (mode == ReportIndicatorEntity.COUNT_VISITS) mode else ReportIndicatorEntity.COUNT_PATIENTS,
                        matchAny = l.optBoolean("matchAny", false),
                        ageBands = l.optString("ageBands").trim(),
                        scope = if (scope == ReportIndicatorEntity.SCOPE_CUMULATIVE) scope else ReportIndicatorEntity.SCOPE_PERIOD,
                        conditionsJson = (l.optJSONArray("conditions") ?: JSONArray()).toString(),
                        sortOrder = i
                    )
                )
            }
            ImportResult(id, null)
        } catch (e: Exception) {
            ImportResult(null, "The setup could not be imported (${e.message ?: "unknown error"}).")
        }
    }
}
