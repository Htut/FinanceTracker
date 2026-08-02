package com.financetracker.evolva.data.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Prompts for the device PIN / pattern / password (or biometrics when available).
 */
object DeviceCredentialAuth {

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
        ) {
            onError("Set a screen lock (PIN, pattern, or password) on this device first.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError(errString.toString())
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
