package ke.co.afyareport.domain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

data class DraftInfo(val patientNo: String, val savedAt: Long)

/** Keeps one unfinished visit entry on the phone until it is saved or discarded. */
class DraftStore(context: Context) {
    private val sp = context.getSharedPreferences("afyareport_draft", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val info: StateFlow<DraftInfo?> = state

    private fun read(): DraftInfo? {
        val json = sp.getString("json", null) ?: return null
        val no = runCatching { JSONObject(json).optString("patientNo") }.getOrDefault("")
        return DraftInfo(no, sp.getLong("savedAt", 0L))
    }

    fun loadJson(): String? = sp.getString("json", null)

    fun save(json: String) {
        sp.edit().putString("json", json).putLong("savedAt", System.currentTimeMillis()).apply()
        state.value = read()
    }

    fun clear() {
        if (sp.contains("json")) {
            sp.edit().remove("json").remove("savedAt").apply()
            state.value = null
        }
    }
}
