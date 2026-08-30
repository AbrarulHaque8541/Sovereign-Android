package com.sovereign.app.tools

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sovereign.app.R

class ADBControlHubScreen : Activity() {
    private val tabs = arrayOf("System", "ADB", "Fastboot", "Scripts", "Packages")
    private var selectedTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.adb_control_hub)
        setupUI()
    }

    private fun setupUI() {
        val container = findViewById<LinearLayout>(R.id.tab_container)
        container?.removeAllViews()

        for ((index, tabName) in tabs.withIndex()) {
            val button = Button(this).apply {
                text = tabName
                id = View.generateViewId()
                tag = index
                setOnClickListener { selectTab(index) }
            }
            container?.addView(button)
        }
    }

    private fun selectTab(index: Int) {
        selectedTab = index
        val message = "Selected: ${tabs[index]}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}