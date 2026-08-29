package com.sovereign.app.updater

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class UpdateInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updateUrl = intent.getStringExtra("update_url")
        Log.d("UpdateInstall", "Update URL: $updateUrl")
        finish()
    }
}
