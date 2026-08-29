package com.sovereign.app.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
 android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sovereign.app.R
import com.sovereign.app.tools.NativeSystemServiceEngine
import com.sovereign.app.tools.ScriptRunnerUtility
import com.sovereign.app.tools.FastbootProtocolBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object ADBControlHubScreen {
    private val TAG = "ADBControlHubScreen"

    val tabs = arrayOf("System", "ADB", "Fastboot", "Scripts", "Packages")

    fun showScreen(activity: AppCompatActivity) {
        activity.setContentView(R.layout.adb_control_hub)

        val tabButtons = gridLayout.findViewsWithTag("tab_button")
        selectedTab = 0
        updateTabSelection(tabButtons, selectedTab)

        gridLayout.setOnClickListener { view ->
            if (view is Button) {
                handleTabClick(view.id, activity)
            }
        }
    }

    private var selectedTab = 0

    private fun updateTabSelection(buttons: Array<Button>, selected: Int) {
        for ((index, button) in buttons.withIndex()) {
            button.text = tabs[index]
            button.setSelected(index == selected)
        }
    }

    private fun handleTabClick(tabId: Int, activity: AppCompatActivity) {
        when (tabId) {
            R.id.tab_system -> activity.startActivity(Intent(activity, SystemTools::class.java))
            R.id.tab_adb -> activity.startActivity(Intent(activity, ADBTools::class.java))
            R.id.tab_fastboot -> activity.startActivity(Intent(activity, FastbootTools::class.java))
            R.id.tab_scripts -> activity.startActivity(Intent(activity, ScriptTools::class.java))
            R.id.tab_packages -> activity.startActivity(Intent(activity, PackageTools::class.java))
        }
    }

    fun refreshUI(activity: AppCompatActivity) {
        val gridLayout = activity.findViewById<GridLayout>(R.id.adb_grid)
        showScreen(activity)
    }
}