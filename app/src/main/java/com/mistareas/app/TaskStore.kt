package com.mistareas.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Guarda todo en el telefono (SharedPreferences). No usa internet. */
object TaskStore {
    private const val PREFS = "mistareas"
    private const val KEY_TASKS = "tasks"
    private const val KEY_PINNED = "pinned"
    private const val KEY_W_COLOR = "w_color"
    private const val KEY_W_SIZE = "w_size"
    private const val KEY_W_FILTER = "w_filter"
    private const val KEY_W_ALPHA = "w_alpha"

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Tareas ----------

    @Synchronized
    fun all(c: Context): List<Task> {
        val raw = prefs(c).getString(KEY_TASKS, "[]") ?: "[]"
        val arr = try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
        val list = ArrayList<Task>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val d = o.optString("date", "")
            val date = if (d.isEmpty()) null else try {
                LocalDate.parse(d)
            } catch (e: Exception) {
                null
            }
            list.add(Task(o.optLong("id"), o.optString("title"), date, o.optBoolean("done")))
        }
        return list
    }

    @Synchronized
    private fun save(c: Context, list: List<Task>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("date", t.date?.toString() ?: "")
            o.put("done", t.done)
            arr.put(o)
        }
        prefs(c).edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    /** Pendientes primero, ordenadas por fecha (sin fecha al final); hechas al fondo. */
    fun sorted(c: Context): List<Task> =
        all(c).sortedWith(
            compareBy<Task>({ it.done }, { it.date?.toEpochDay() ?: Long.MAX_VALUE }, { it.id })
        )

    /** Pendientes de hoy y atrasadas. */
    fun dueToday(c: Context): List<Task> {
        val today = LocalDate.now()
        return sorted(c).filter { !it.done && it.date != null && !it.date.isAfter(today) }
    }

    /** Pendientes futuras o sin fecha. */
    fun upcoming(c: Context): List<Task> {
        val today = LocalDate.now()
        return sorted(c).filter { !it.done && (it.date == null || it.date.isAfter(today)) }
    }

    @Synchronized
    fun add(c: Context, title: String, date: LocalDate?) {
        val list = all(c).toMutableList()
        list.add(Task(System.currentTimeMillis(), title, date, false))
        save(c, list)
    }

    @Synchronized
    fun toggle(c: Context, id: Long) {
        save(c, all(c).map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    @Synchronized
    fun delete(c: Context, id: Long) {
        save(c, all(c).filter { it.id != id })
    }

    @Synchronized
    fun clearDone(c: Context) {
        save(c, all(c).filter { !it.done })
    }

    // ---------- Ajustes ----------

    fun isPinned(c: Context): Boolean = prefs(c).getBoolean(KEY_PINNED, true)

    fun setPinned(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean(KEY_PINNED, value).apply()
    }

    fun widgetColor(c: Context): Int = prefs(c).getInt(KEY_W_COLOR, 0)
    fun widgetSize(c: Context): Int = prefs(c).getInt(KEY_W_SIZE, 1)
    fun widgetFilter(c: Context): Int = prefs(c).getInt(KEY_W_FILTER, 0)
    fun widgetAlpha(c: Context): Int = prefs(c).getInt(KEY_W_ALPHA, 90).coerceIn(20, 100)

    fun saveWidgetSettings(c: Context, color: Int, size: Int, filter: Int, alpha: Int) {
        prefs(c).edit()
            .putInt(KEY_W_COLOR, color)
            .putInt(KEY_W_SIZE, size)
            .putInt(KEY_W_FILTER, filter)
            .putInt(KEY_W_ALPHA, alpha.coerceIn(20, 100))
            .apply()
    }

    // ---------- Refrescar widget y notificacion ----------

    fun notifyChanged(c: Context) {
        TaskWidget.refreshAll(c)
        PinnedNotifier.refresh(c)
    }
}
