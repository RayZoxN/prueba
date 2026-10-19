package com.mistareas.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate

/** Colores y tamaños disponibles para personalizar el widget. */
data class WidgetTheme(val bg: Int, val text: Int, val sub: Int) {
    companion object {
        fun get(index: Int): WidgetTheme = when (index) {
            1 -> WidgetTheme(c("#F5F5F5"), c("#212121"), c("#666666")) // Claro
            2 -> WidgetTheme(c("#1E3A5F"), c("#FFFFFF"), c("#B8C8E0")) // Azul
            3 -> WidgetTheme(c("#1F4D3A"), c("#FFFFFF"), c("#B5D6C5")) // Verde
            4 -> WidgetTheme(c("#3B2A5C"), c("#FFFFFF"), c("#CFC3E6")) // Morado
            else -> WidgetTheme(c("#1E1E2E"), c("#FFFFFF"), c("#B0B0C0")) // Oscuro
        }

        fun textSp(index: Int): Float = when (index) {
            0 -> 12f
            2 -> 18f
            else -> 14f
        }

        private fun c(hex: String): Int = Color.parseColor(hex)
    }
}

class TaskWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.mistareas.app.TOGGLE"
        const val EXTRA_ID = "task_id"

        fun refreshAll(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, TaskWidget::class.java))
            if (ids.isEmpty()) return
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            for (id in ids) {
                mgr.updateAppWidget(id, build(c, id))
            }
        }

        fun build(c: Context, widgetId: Int): RemoteViews {
            val rv = RemoteViews(c.packageName, R.layout.widget_layout)
            val theme = WidgetTheme.get(TaskStore.widgetColor(c))
            val alpha = TaskStore.widgetAlpha(c) * 255 / 100

            // Fondo redondeado con color y transparencia elegidos
            rv.setInt(R.id.widget_bg, "setColorFilter", theme.bg)
            rv.setInt(R.id.widget_bg, "setImageAlpha", alpha)
            rv.setTextColor(R.id.widget_title, theme.text)
            rv.setTextColor(R.id.widget_add, theme.text)
            rv.setTextColor(R.id.widget_empty, theme.sub)
            rv.setTextViewText(R.id.widget_title, "Mis tareas")

            // Lista
            val svc = Intent(c, TaskWidgetService::class.java)
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.widget_list, svc)
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Tocar una tarea la marca como hecha
            val toggle = Intent(c, TaskWidget::class.java).setAction(ACTION_TOGGLE)
            val togglePi = PendingIntent.getBroadcast(
                c, 0, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            rv.setPendingIntentTemplate(R.id.widget_list, togglePi)

            // Boton "+" abre la app lista para agregar
            val add = Intent(c, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_ADD, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            rv.setOnClickPendingIntent(
                R.id.widget_add,
                PendingIntent.getActivity(
                    c, 1, add,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Tocar el titulo abre la app
            val open = Intent(c, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            rv.setOnClickPendingIntent(
                R.id.widget_title,
                PendingIntent.getActivity(
                    c, 2, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return rv
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, build(context, id))
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            if (id != -1L) {
                TaskStore.toggle(context, id)
                TaskStore.notifyChanged(context)
            }
        }
    }
}

/** Entrega al widget la lista de tareas. */
class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskWidgetFactory(applicationContext)
}

class TaskWidgetFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val today = LocalDate.now()
        val onlyToday = TaskStore.widgetFilter(ctx) == 1
        items = TaskStore.sorted(ctx).filter {
            !it.done && (!onlyToday || (it.date != null && !it.date.isAfter(today)))
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_item)
        if (position >= items.size) return rv
        val t = items[position]
        val theme = WidgetTheme.get(TaskStore.widgetColor(ctx))
        val sp = WidgetTheme.textSp(TaskStore.widgetSize(ctx))

        rv.setTextViewText(R.id.item_title, "○  " + t.title)
        rv.setTextColor(R.id.item_title, theme.text)
        rv.setTextViewTextSize(R.id.item_title, TypedValue.COMPLEX_UNIT_SP, sp)

        rv.setTextViewText(R.id.item_date, t.dateLabel())
        rv.setTextColor(
            R.id.item_date,
            if (t.isOverdue()) Color.parseColor("#FF6B6B") else theme.sub
        )
        rv.setTextViewTextSize(R.id.item_date, TypedValue.COMPLEX_UNIT_SP, sp - 2f)

        val fill = Intent().putExtra(TaskWidget.EXTRA_ID, t.id)
        rv.setOnClickFillInIntent(R.id.item_root, fill)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
