package com.davehq.thetopflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class TopFlowInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> launchUserAction(context, intent)
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "The Top Flow update installed successfully.")
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No installer detail."
                val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: "unknown"
                Log.w(TAG, "The Top Flow update install failed. version=$versionName status=$status message=$message")
            }
        }
    }

    private fun launchUserAction(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        if (confirmationIntent == null) {
            Log.w(TAG, "Installer requested user action but did not provide a confirmation intent.")
            return
        }
        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(confirmationIntent)
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.davehq.thetopflow.action.INSTALL_STATUS"
        const val EXTRA_VERSION_NAME = "com.davehq.thetopflow.extra.VERSION_NAME"
        private const val TAG = "TopFlowInstaller"
    }
}
