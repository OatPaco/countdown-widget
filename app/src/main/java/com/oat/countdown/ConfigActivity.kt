package com.oat.countdown

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val selectedIds = mutableSetOf<String>()
    private var events = listOf<CountdownEvent>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var saveBtn: Button
    private lateinit var permPrompt: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result = cancelled (user backs out)
        setResult(Activity.RESULT_CANCELED)

        // Get widget ID
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContentView(R.layout.activity_config)

        recyclerView = findViewById(R.id.event_list)
        saveBtn = findViewById(R.id.save_btn)
        permPrompt = findViewById(R.id.perm_prompt)

        recyclerView.layoutManager = LinearLayoutManager(this)

        saveBtn.setOnClickListener { save() }
        findViewById<Button>(R.id.perm_btn).setOnClickListener { requestCalendarPermission() }

        // Load existing selections if reconfiguring
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            selectedIds.addAll(PrefsHelper.getSelectedIds(this, widgetId))
        }

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            == PackageManager.PERMISSION_GRANTED
        ) {
            permPrompt.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            saveBtn.visibility = View.VISIBLE
            loadEvents()
        } else {
            permPrompt.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            saveBtn.visibility = View.GONE
            requestCalendarPermission()
        }
    }

    private fun requestCalendarPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CALENDAR),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            checkPermissionAndLoad()
        }
    }

    private fun loadEvents() {
        events = CalendarHelper.getEvents(this, 1095)
        recyclerView.adapter = EventAdapter(events, selectedIds) { updateSaveBtn() }
        updateSaveBtn()
    }

    private fun updateSaveBtn() {
        val n = selectedIds.size
        saveBtn.isEnabled = n > 0
        saveBtn.text = if (n == 0) "Select events" else "Save ($n selected)"
    }

    private fun save() {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Save selections
        PrefsHelper.setSelectedIds(this, widgetId, selectedIds)

        // Update widget
        val manager = AppWidgetManager.getInstance(this)
        CountdownWidget.updateWidget(this, manager, widgetId)

        // Return success
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
