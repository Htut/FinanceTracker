package com.financetracker.evolva.data.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent

/**
 * Builds an intent that asks for the device PIN / pattern / password.
 * Launch with [androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult].
 */
object DeviceCredentialAuth {

    fun createConfirmIntent(
        activity: Activity,
        title: String,
        description: String
    ): Intent? {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isDeviceSecure) {
            return null
        }
        @Suppress("DEPRECATION")
        return keyguard.createConfirmDeviceCredentialIntent(title, description)
    }
}
