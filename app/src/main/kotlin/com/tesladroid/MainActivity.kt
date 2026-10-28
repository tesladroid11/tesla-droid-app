package com.tesladroid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tesladroid.core.engine.CoreEngineBridge
import com.tesladroid.engine.ShizukuBridge
import com.tesladroid.theme.TeslaDroidTheme
import com.tesladroid.ui.dashboard.DashboardScreen
import com.tesladroid.ui.dashboard.DashboardViewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.i("MainActivity", "RECORD_AUDIO permission granted")
        }
    }

    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            CoreEngineBridge.instance.startShizukuStreaming(this)
            Toast.makeText(this, "Streaming started!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "VPN permission required for local car connection (100.99.9.9)", Toast.LENGTH_SHORT).show()
        }
    }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
        if (requestCode == 4242) {
            viewModel.refreshPermissions(this)
        }
    }

    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            viewModel.refreshPermissions(this)
            viewModel.checkAndTriggerAutoStart(this) { vpnIntent ->
                vpnConsentLauncher.launch(vpnIntent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.addBinderReceivedListener(shizukuBinderListener)
        } catch (e: Throwable) {}

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        Thread {
            try {
                if (Shizuku.pingBinder()) {
                    ShizukuBridge.execShell("pm grant com.tesladroid android.permission.RECORD_AUDIO; pm grant com.tesladroid android.permission.WRITE_SECURE_SETTINGS; setprop service.adb.tcp.port 5555")
                }
            } catch (_: Throwable) {}
        }.start()

        // Keep screen awake while app is running to prevent MIUI/system sleep & killing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Pre-warm / enroll Let's Encrypt TLS certificate in background so it's ready before streaming starts
        com.tesladroid.core.crypto.CertificateManager.ensureCertificateAsync(this)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Dynamic screen brightness dimming
            LaunchedEffect(uiState.isDimmed) {
                val lp = window.attributes
                lp.screenBrightness = if (uiState.isDimmed) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }

            TeslaDroidTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onRequireVpnConsent = { vpnIntent ->
                        vpnConsentLauncher.launch(vpnIntent)
                    }
                )
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            viewModel.onUserInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        try {
            val carBtClass = Class.forName("com.tesladroid.engine.CarBluetooth")
            val field = carBtClass.getField("b")
            field.setBoolean(null, true)
        } catch (_: Throwable) {}
        viewModel.onActivityResumed()
        viewModel.refreshPermissions(this)
        CoreEngineBridge.instance.refreshShortCode(this)
        viewModel.checkAndTriggerAutoStart(this) { vpnIntent ->
            vpnConsentLauncher.launch(vpnIntent)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            val carBtClass = Class.forName("com.tesladroid.engine.CarBluetooth")
            val field = carBtClass.getField("b")
            field.setBoolean(null, false)
        } catch (_: Throwable) {}
        viewModel.onActivityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        } catch (e: Throwable) {}
    }
}
