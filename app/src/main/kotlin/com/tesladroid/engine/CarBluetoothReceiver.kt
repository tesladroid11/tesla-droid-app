package com.tesladroid.engine

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.tesladroid.core.engine.CoreEngineBridge
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.core.util.AppPreferences

/**
 * Handles vehicle Bluetooth connection and disconnection events to automatically
 * start streaming on entry and gracefully stop streaming on vehicle exit.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CarBluetoothReceiver"
        private const val DISCONNECT_GRACE_PERIOD_MS = 30_000L
        private var disconnectRunnable: Runnable? = null
        private val handler = Handler(Looper.getMainLooper())
        private var wakeLock: PowerManager.WakeLock? = null

        private fun acquireWakeLock(context: Context) {
            try {
                if (wakeLock == null) {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TeslaDroid:BTGrace")
                    wakeLock?.setReferenceCounted(false)
                }
                wakeLock?.acquire(DISCONNECT_GRACE_PERIOD_MS + 5000L)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to acquire wake lock: ${t.message}")
            }
        }

        private fun releaseWakeLock() {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return
        val prefs = AppPreferences(context)
        val configuredCarAddr = prefs.carBtAddr.trim()

        if (configuredCarAddr.isBlank()) {
            Log.d(TAG, "No car Bluetooth address configured; ignoring BT event: $action")
            return
        }

        val device = @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val deviceAddr = try { device?.address ?: "" } catch (_: SecurityException) { "" }
        val deviceName = try { device?.name ?: "" } catch (_: SecurityException) { "" }

        val isTargetCar = deviceAddr.equals(configuredCarAddr, ignoreCase = true)

        Log.i(TAG, "BT Event: action=$action, device=$deviceName ($deviceAddr), matchesCar=$isTargetCar")

        val bridge = CoreEngineBridge.instance

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (isTargetCar) {
                    // Cancel any pending disconnect auto-stop
                    disconnectRunnable?.let {
                        handler.removeCallbacks(it)
                        disconnectRunnable = null
                        releaseWakeLock()
                        Log.i(TAG, "Car BT reconnected within grace period. Disconnect auto-stop cancelled.")
                    }

                    val isStreaming = bridge.connectionState.value == ConnectionState.STREAMING || bridge.connectionState.value == ConnectionState.CAR_CONNECTED
                    if (prefs.autoStart && !isStreaming) {
                        Log.i(TAG, "Car Bluetooth connected ($deviceAddr) & auto-start is enabled! Launching streaming session...")
                        bridge.startShizukuStreaming(context.applicationContext) { success, error ->
                            Log.i(TAG, "Auto-start streaming result: success=$success, error=$error")
                        }
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val isBtOff = action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF

                if (isTargetCar || isBtOff) {
                    if (bridge.connectionState.value != ConnectionState.IDLE) {
                        Log.i(TAG, "Car Bluetooth disconnected! Starting 30-second disconnect grace timer before stopping stream...")
                        acquireWakeLock(context)
                        disconnectRunnable?.let { handler.removeCallbacks(it) }

                        val r = Runnable {
                            Log.i(TAG, "30-second grace timer elapsed without car BT reconnect. Stopping streaming session!")
                            bridge.stopStreaming(context.applicationContext)
                            bridge.dismissSystemDim()
                            releaseWakeLock()
                            disconnectRunnable = null
                        }
                        disconnectRunnable = r
                        handler.postDelayed(r, DISCONNECT_GRACE_PERIOD_MS)
                    }
                }
            }
        }
    }
}
