package com.skinscan.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Condition(
    val name: String,
    val likelihood: String,
    val description: String
)

data class Analysis(
    val summary: String,
    val severity: String,            // low | medium | high
    val conditions: List<Condition>,
    val recommendations: List<String>,
    val seeDoctor: Boolean,
    val urgentSigns: List<String>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("summary", summary)
        put("severity", severity)
        put("see_doctor", seeDoctor)
        put("conditions", JSONArray().also { arr ->
            conditions.forEach {
                arr.put(JSONObject().put("name", it.name).put("likelihood", it.likelihood).put("description", it.description))
            }
        })
        put("recommendations", JSONArray(recommendations))
        put("urgent_signs", JSONArray(urgentSigns))
    }

    companion object {
        private fun JSONArray?.strings(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
        }

        fun fromJson(o: JSONObject): Analysis {
            val conds = o.optJSONArray("conditions")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val c = arr.optJSONObject(i) ?: return@mapNotNull null
                    Condition(
                        c.optString("name"),
                        c.optString("likelihood"),
                        c.optString("description")
                    )
                }
            } ?: emptyList()
            return Analysis(
                summary = o.optString("summary"),
                severity = o.optString("severity", "low").lowercase(),
                conditions = conds,
                recommendations = o.optJSONArray("recommendations").strings(),
                seeDoctor = o.optBoolean("see_doctor", false),
                urgentSigns = o.optJSONArray("urgent_signs").strings()
            )
        }

        /** Достаёт JSON из ответа модели (даже если он обёрнут в ```json ... ```). */
        fun parse(text: String): Analysis {
            val s = text.indexOf('{')
            val e = text.lastIndexOf('}')
            if (s >= 0 && e > s) {
                try {
                    return fromJson(JSONObject(text.substring(s, e + 1)))
                } catch (_: Exception) { }
            }
            return Analysis(text.trim(), "low", emptyList(), emptyList(), true, emptyList())
        }
    }
}

data class HistoryItem(
    val id: Long,
    val note: String,
    val analysis: Analysis
)
