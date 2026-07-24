package com.davehq.thetopflow.ui

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo as DeviceInfo
import android.os.Handler
import android.os.Looper

/**
 * Rap Ready one-knob control, gated on headphones.
 * Inspired by RapReady One (HPF → expander → EQ → compression → de-ess → saturation → limiter).
 * Full real-time DSP port is a later milestone; this exposes the control safely and only
 * when the user can hear the result on headphones.
 */
fun hasHeadphonesConnected(context: Context): Boolean {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
                else -> false
            }
        }
    } else {
        @Suppress("DEPRECATION")
        am.isWiredHeadsetOn || am.isBluetoothA2dpOn
    }
}

@Composable
fun rememberHeadphonesConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(hasHeadphonesConnected(context)) }
    DisposableEffect(context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onDispose { }
        } else {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out DeviceInfo>?) {
                    connected = hasHeadphonesConnected(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out DeviceInfo>?) {
                    connected = hasHeadphonesConnected(context)
                }
            }
            am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            onDispose { am.unregisterAudioDeviceCallback(callback) }
        }
    }
    return connected
}

@Composable
fun RapReadyKnob(
    accent: Color,
    amount: Float,
    onAmountChange: (Float) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headphones = rememberHeadphonesConnected()
    if (!headphones) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "Rap Ready knob appears when headphones are connected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Rap Ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
                Text(
                    "${amount.roundToInt()} · mix-ready vocal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "0 = dry · 62 = default · 100 = full chain",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = amount,
                onValueChange = onAmountChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.8f)
                )
            )
            OutlinedButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply to selected recording")
            }
        }
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
