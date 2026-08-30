package com.sovereign.app.vault

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.sovereign.app.R

class VaultLockActivity : AppCompatActivity() {

    private lateinit var vaultManager: VaultManager
    private var isSetupMode = false
    private var enteredPin = StringBuilder()
    private var confirmPin: String? = null
    private var state: LockState = LockState.ENTER_PIN

    private enum class LockState {
        ENTER_PIN,
        SET_NEW_PIN,
        CONFIRM_PIN,
        SETUP_COMPLETE
    }

    private lateinit var rootView: LinearLayout
    private lateinit var pinDotsContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var keypadContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultManager = VaultManager.getInstance(this)
        isSetupMode = !vaultManager.isVaultInitialized()

        setupUI()
    }

    private fun setupUI() {
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0a0e27.toInt())
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        // Lock icon
        rootView.addView(ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(0xFFef4444.toInt())
            layoutParams = LinearLayout.LayoutParams(100, 100).apply { setMargins(0, 80, 0, 24) }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        })

        // Title
        rootView.addView(TextView(this).apply {
            text = if (isSetupMode) "🔒 Set Up Your Vault" else "🔐 Enter PIN"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 24f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setPadding(24, 16, 24, 8)
        })

        // Status
        statusText = TextView(this).apply {
            text = if (isSetupMode) "Create a 4-digit PIN" else "Enter your PIN to unlock"
            setTextColor(0xFF9ca3af.toInt())
            textSize = 14f
            setPadding(24, 0, 24, 32)
        }
        rootView.addView(statusText)

        // PIN dots
        val act = this
        pinDotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        repeat(4) { i ->
            pinDotsContainer.addView(ImageView(act).apply {
                setImageResource(android.R.drawable.presence_invisible)
                setColorFilter(0xFFef4444.toInt())
                layoutParams = LinearLayout.LayoutParams(32, 32).apply { setMargins(8, 0, 8, 24) }
                tag = "dot_$i"
            })
        }
        rootView.addView(pinDotsContainer)

        // Keypad
        keypadContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
            }
            row.forEach { key ->
                if (key.isNotEmpty()) {
                    rowLayout.addView(createKeyButton(key))
                } else {
                    rowLayout.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(80, 80)
                    })
                }
            }
            keypadContainer.addView(rowLayout)
        }
        rootView.addView(keypadContainer)

        // Biometric button (if available)
        if (!isSetupMode && vaultManager.isFingerprintEnabled() && isBiometricAvailable()) {
            rootView.addView(Button(this).apply {
                text = "🔐 Use Fingerprint"
                setBackgroundColor(0xFF1e293b.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(32, 16, 32, 16)
                setOnClickListener { showBiometricPrompt() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 24, 0, 0) }
            })
        }

        setContentView(rootView)
    }

    private fun createKeyButton(key: String): Button {
        return Button(this).apply {
            text = key
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1e293b.toInt())
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { setMargins(8, 8, 8, 8) }
            setOnClickListener {
                if (key == "⌫") {
                    onBackspace()
                } else {
                    onKeyPressed(key)
                }
            }
        }
    }

    private fun onKeyPressed(key: String) {
        if (enteredPin.length < 4) {
            enteredPin.append(key)
            updateDots()
            playTickSound()

            if (enteredPin.length == 4) {
                android.os.Handler(mainLooper).postDelayed({
                    processPin()
                }, 200)
            }
        }
    }

    private fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updateDots()
        }
    }

    private fun updateDots() {
        for (i in 0..3) {
            val dot = pinDotsContainer.findViewWithTag<ImageView>("dot_$i")
            if (i < enteredPin.length) {
                dot.setImageResource(android.R.drawable.presence_online)
                dot.setColorFilter(0xFFef4444.toInt())
            } else {
                dot.setImageResource(android.R.drawable.presence_invisible)
                dot.setColorFilter(0xFFef4444.toInt())
            }
        }
    }

    private fun processPin() {
        val pin = enteredPin.toString()
        enteredPin.clear()

        when (state) {
            LockState.ENTER_PIN -> {
                if (vaultManager.verifyPin(pin)) {
                    vaultManager.recordUnlock()
                    onUnlockSuccess()
                } else {
                    showError("Wrong PIN. Try again.")
                    shakeDots()
                }
            }
            LockState.SET_NEW_PIN -> {
                if (pin.length == 4) {
                    confirmPin = pin
                    state = LockState.CONFIRM_PIN
                    statusText.text = "Confirm your PIN"
                    updateDots()
                }
            }
            LockState.CONFIRM_PIN -> {
                if (pin == confirmPin) {
                    vaultManager.setupPin(pin)
                    state = LockState.SETUP_COMPLETE
                    onSetupComplete()
                } else {
                    showError("PINs don't match. Try again.")
                    confirmPin = null
                    state = LockState.SET_NEW_PIN
                    statusText.text = "Create a 4-digit PIN"
                    updateDots()
                }
            }
            LockState.SETUP_COMPLETE -> {}
        }
    }

    private fun showError(msg: String) {
        statusText.text = msg
        statusText.setTextColor(0xFFef4444.toInt())
        android.os.Handler(mainLooper).postDelayed({
            statusText.setTextColor(0xFF9ca3af.toInt())
            if (state == LockState.ENTER_PIN) {
                statusText.text = "Enter your PIN to unlock"
            }
        }, 2000)
    }

    private fun shakeDots() {
        val shake = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        pinDotsContainer.startAnimation(shake)
    }

    private fun playTickSound() {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.apply {
                setVolume(0.1f, 0.1f)
                start()
                setOnCompletionListener { release() }
            }
        } catch (e: Exception) { }
    }

    private fun onUnlockSuccess() {
        Toast.makeText(this, "🔓 Vault Unlocked!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, VaultActivity::class.java))
        finish()
    }

    private fun onSetupComplete() {
        Toast.makeText(this, "✅ Vault Created!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, VaultActivity::class.java))
        finish()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    vaultManager.recordUnlock()
                    onUnlockSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showError(errString.toString())
                    }
                }
                override fun onAuthenticationFailed() {
                    showError("Fingerprint not recognized")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Use your fingerprint")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onBackPressed() {
        if (isSetupMode) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
