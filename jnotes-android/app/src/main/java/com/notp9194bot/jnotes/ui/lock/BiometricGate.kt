package com.notp9194bot.jnotes.ui.lock

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wrapper around AndroidX BiometricPrompt with sensible defaults for jnotes.
 *
 * Uses [BiometricManager.Authenticators.BIOMETRIC_STRONG] when available
 * (Class 3 — fingerprint, secure face unlock), and gracefully falls back to
 * device credential (PIN/pattern/password) on devices that need it.
 */
object BiometricGate {

    /** Result of [check] — gives the caller a precise reason for unavailability. */
    enum class Availability {
        AVAILABLE,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED,
        UNKNOWN,
    }

    /** Preferred authenticators: strong biometric OR device credential. */
    private val preferredAuthenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            // On API < 30 combining STRONG + DEVICE_CREDENTIAL is unsupported, so
            // we use weak biometrics with device credential fallback handled in code.
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    fun isAvailable(context: Context): Boolean = check(context) == Availability.AVAILABLE

    fun check(context: Context): Availability {
        val bm = BiometricManager.from(context)
        return when (bm.canAuthenticate(preferredAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Availability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Availability.UNSUPPORTED
            else -> Availability.UNKNOWN
        }
    }

    fun prompt(
        activity: FragmentActivity,
        title: String = "Unlock jnotes",
        subtitle: String = "Use fingerprint, face, or device PIN",
        description: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onFailed: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Treat user-cancel as a soft event (let the PIN entry continue).
                onError(errString.toString())
            }
            override fun onAuthenticationFailed() {
                onFailed()
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(preferredAuthenticators)

        if (description != null) infoBuilder.setDescription(description)

        prompt.authenticate(infoBuilder.build())
    }
}
