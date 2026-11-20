package com.zekr.widget.routine.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.zekr.widget.routine.engine.RoutineEngine
import com.zekr.widget.routine.model.BluetoothEventType
import com.zekr.widget.routine.model.ChargingConditionType

class RoutineEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        RoutineEngine.init(context)

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val name = getDeviceNameSafe(context, device)
                val mac = device?.address
                RoutineEngine.onBluetoothEvent(BluetoothEventType.CONNECTED, name, mac)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val name = getDeviceNameSafe(context, device)
                val mac = device?.address
                RoutineEngine.onBluetoothEvent(BluetoothEventType.DISCONNECTED, name, mac)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                RoutineEngine.onPowerEvent(ChargingConditionType.CONNECTED)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                RoutineEngine.onPowerEvent(ChargingConditionType.DISCONNECTED)
            }
            Intent.ACTION_TIME_TICK -> {
                RoutineEngine.onTimeTick()
            }
        }
    }

    private fun getDeviceNameSafe(context: Context, device: BluetoothDevice?): String? {
        if (device == null) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name ?: device.address
                } else {
                    device.address
                }
            } else {
                @Suppress("DEPRECATION")
                device.name ?: device.address
            }
        } catch (e: SecurityException) {
            device.address
        }
    }
}
