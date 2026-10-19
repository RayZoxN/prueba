package com.mistareas.app

import android.Manifest
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ADD = "open_add"
    }

    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private val adapter = TaskAdapter()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            PinnedNotifier.refresh(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listTasks)
        emptyView = findViewById(R.id.tvEmpty)
        listView.adapter = adapter

        // Tocar una tarea: marcar / desmarcar como hecha
        listView.setOnItemClickListener { _, _, position, _ ->
            val t = adapter.getItem(position)
            TaskStore.toggle(this, t.id)
            TaskStore.notifyChanged(this)
            reload()
        }
        // Mantener presionada: borrar
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val t = adapter.getItem(position)
            AlertDialog.Builder(this)
                .setTitle("¿Borrar tarea?")
                .setMessage(t.title)
                .setPositiveButton("Borrar") { _, _ ->
                    TaskStore.delete(this, t.id)
                    TaskStore.notifyChanged(this)
                    reload()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            true
        }

        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }
        findViewById<View>(R.id.btnMenu).setOnClickListener { showMenu(it) }

        val sw = findViewById<CompoundButton>(R.id.swPinned)
        sw.isChecked = TaskStore.isPinned(this)
        sw.setOnCheckedChangeListener { _, checked ->
            TaskStore.setPinned(this, checked)
            if (checked) {
                askNotificationPermission()
                PinnedNotifier.start(this)
            } else {
                PinnedNotifier.stop(this)
            }
        }

        askNotificationPermission()
        handleIntent()
    }

    override fun onResume() {
        super.onResume()
        reload()
        if (TaskStore.isPinned(this) && !PinnedService.running) {
            PinnedNotifier.start(this)
        }
    }

    private fun handleIntent() {
        if (intent?.getBooleanExtra(EXTRA_ADD, false) == true) {
            intent.removeExtra(EXTRA_ADD)
            showAddDialog()
        }
    }

    private fun reload() {
        adapter.setItems(TaskStore.sorted(this))
        emptyView.visibility = if (adapter.count == 0) View.VISIBLE else View.GONE
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------- Menu ----------

    private fun showMenu(anchor: View) {
        val pm = PopupMenu(this, anchor)
        pm.menu.add(0, 1, 0, "Añadir widget a la pantalla")
        pm.menu.add(0, 2, 1, "Personalizar widget")
        pm.menu.add(0, 3, 2, "Borrar tareas hechas")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> pinWidget()
                2 -> showWidgetSettings()
                3 -> {
                    TaskStore.clearDone(this)
                    TaskStore.notifyChanged(this)
                    reload()
                }
            }
            true
        }
        pm.show()
    }

    private fun pinWidget() {
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, TaskWidget::class.java), null, null)
        } else {
            Toast.makeText(
                this,
                "Mantén presionado un espacio vacío de la pantalla de inicio → Widgets → Mis tareas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- Dialogo: nueva tarea ----------

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add, null)
        val et = view.findViewById<EditText>(R.id.etTitle)
        val btnDate = view.findViewById<Button>(R.id.btnDate)
        val chk = view.findViewById<CheckBox>(R.id.chkNoDate)
        var chosen: LocalDate? = LocalDate.now()

        fun refreshButton() {
            btnDate.text = "Fecha: " + dateLabelFor(chosen)
        }
        refreshButton()

        btnDate.setOnClickListener {
            val base = chosen ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    chosen = LocalDate.of(y, m + 1, d)
                    chk.isChecked = false
                    refreshButton()
                },
                base.year, base.monthValue - 1, base.dayOfMonth
            ).show()
        }
        chk.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chosen = null
            } else if (chosen == null) {
                chosen = LocalDate.now()
            }
            refreshButton()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Nueva tarea")
            .setView(view)
            .setPositiveButton("Agregar") { _, _ ->
                val title = et.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(this, "Escribe la tarea", Toast.LENGTH_SHORT).show()
                } else {
                    TaskStore.add(this, title, chosen)
                    TaskStore.notifyChanged(this)
                    reload()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        et.requestFocus()
    }

    // ---------- Dialogo: personalizar widget ----------

    private fun showWidgetSettings() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_widget_settings, null)
        val spColor = view.findViewById<Spinner>(R.id.spColor)
        val spSize = view.findViewById<Spinner>(R.id.spSize)
        val spFilter = view.findViewById<Spinner>(R.id.spFilter)
        val sbAlpha = view.findViewById<SeekBar>(R.id.sbAlpha)

        fun setup(sp: Spinner, arrayRes: Int, selected: Int) {
            val a = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item
            )
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sp.adapter = a
            sp.setSelection(selected)
        }
        setup(spColor, R.array.widget_colors, TaskStore.widgetColor(this))
        setup(spSize, R.array.widget_sizes, TaskStore.widgetSize(this))
        setup(spFilter, R.array.widget_filters, TaskStore.widgetFilter(this))
        sbAlpha.max = 80
        sbAlpha.progress = TaskStore.widgetAlpha(this) - 20

        AlertDialog.Builder(this)
            .setTitle("Personalizar widget")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                TaskStore.saveWidgetSettings(
                    this,
                    spColor.selectedItemPosition,
                    spSize.selectedItemPosition,
                    spFilter.selectedItemPosition,
                    sbAlpha.progress + 20
                )
                TaskWidget.refreshAll(this)
                Toast.makeText(this, "Widget actualizado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Lista ----------

    private inner class TaskAdapter : BaseAdapter() {
        private var items: List<Task> = emptyList()

        fun setItems(list: List<Task>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Task = items[position]
        override fun getItemId(position: Int): Long = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            val t = items[position]
            val check = v.findViewById<TextView>(R.id.tvCheck)
            val title = v.findViewById<TextView>(R.id.tvTitle)
            val date = v.findViewById<TextView>(R.id.tvDate)

            check.text = if (t.done) "☑" else "☐"
            title.text = t.title
            title.paintFlags = if (t.done) {
                title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            date.text = t.dateLabel()
            date.setTextColor(if (t.isOverdue()) Color.parseColor("#E5484D") else Color.GRAY)
            v.alpha = if (t.done) 0.5f else 1f
            return v
        }
    }
}
