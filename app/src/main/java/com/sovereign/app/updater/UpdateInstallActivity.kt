package com.sovereign.app.updater

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sovereign.app.AppScope.backgroundScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class UpdateInstallActivity : Activity() {
    companion object {
        private const val TAG = "UpdateInstallActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val action = intent?.action ?: return finish()
        
        when (action) {
            Intent.ACTION_INSTALL_PACKAGE -> {
                handleInstallPackage()
            }
            "com.sovereign.INSTALL_UPDATE" -> {
                handleDirectInstall()
            }
            else -> {
                Toast.makeText(this, "Unknown install action", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun handleInstallPackage() {
        val uri = intent?.data
        if (uri != null) {
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(installIntent)
        }
        finish()
    }
    
    private fun handleDirectInstall() {
        backgroundScope.launch(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "updates/sovereign-update.apk")
                if (!file.exists()) {
                    Log.e(TAG, "Update file not found")
                    runOnUiThread { 
                        Toast.makeText(this@UpdateInstallActivity, "Update file not found", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }
                
                val uri = FileProvider.getUriForFile(
                    this@UpdateInstallActivity,
                    "${packageName}.fileprovider",
                    file
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                runOnUiThread {
                    startActivity(installIntent)
                    finish()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Install failed", e)
                runOnUiThread {
                    Toast.makeText(this@UpdateInstallActivity, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}