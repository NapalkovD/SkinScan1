package com.skinscan.app.data

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class HistoryStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val dir = File(ctx.filesDir, "thumbs").apply { mkdirs() }

    fun thumbFile(id: Long) = File(dir, "$id.jpg")

    fun load(): List<HistoryItem> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                HistoryItem(o.getLong("id"), o.optString("note"), Analysis.fromJson(o.getJSONObject("analysis")))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(items: List<HistoryItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("note", it.note).put("analysis", it.analysis.toJson()))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun add(bmp: Bitmap, note: String, analysis: Analysis): List<HistoryItem> {
        val id = System.currentTimeMillis()
        thumbFile(id).outputStream().use { ImageUtils.thumbnail(bmp).compress(Bitmap.CompressFormat.JPEG, 80, it) }
        val items = (listOf(HistoryItem(id, note, analysis)) + load()).take(50)
        persist(items)
        dir.listFiles()?.forEach { f ->
            if (items.none { thumbFile(it.id).name == f.name }) f.delete()
        }
        return items
    }

    fun remove(id: Long): List<HistoryItem> {
        thumbFile(id).delete()
        val items = load().filter { it.id != id }
        persist(items)
        return items
    }

    fun clear(): List<HistoryItem> {
        dir.listFiles()?.forEach { it.delete() }
        persist(emptyList())
        return emptyList()
    }
}
