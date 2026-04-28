package com.notp9194bot.jnotes.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notp9194bot.jnotes.util.Hashing
import kotlinx.coroutines.delay

/**
 * Modern PIN + biometric unlock screen.
 *
 * Features:
 *   - Custom 3×4 keypad — fast, no soft keyboard.
 *   - Animated PIN dots that pulse on input and shake on error.
 *   - Auto-prompts biometric (fingerprint / face / device credential).
 *   - Lockout cooldown after 5 wrong PINs (15s, doubles each cycle).
 *   - Manual fingerprint + face buttons so users can re-trigger biometric.
 */
@Composable
fun PinLockScreen(
    pinHash: String,
    pinSalt: String?,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockoutUntilMs by remember { mutableStateOf(0L) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var shake by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptics = LocalHapticFeedback.current
    val biometricAvailable = activity != null && BiometricGate.isAvailable(context)

    // Tick the clock so the cooldown countdown updates.
    LaunchedEffect(lockoutUntilMs) {
        while (lockoutUntilMs > System.currentTimeMillis()) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
        nowMs = System.currentTimeMillis()
    }

    // Auto-prompt biometric whenever the lock screen becomes resumed.
    // Using a lifecycle observer (rather than a one-shot LaunchedEffect) means
    // the prompt re-fires after the user dismisses it, comes back from
    // background, or returns to the lock screen — i.e. every real "open app"
    // moment.
    val lifecycleOwner = LocalLifecycleOwner.current
    var biometricCancelledByUser by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, biometricEnabled, biometricAvailable, activity) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                biometricEnabled &&
                biometricAvailable &&
                activity != null &&
                !biometricCancelledByUser
            ) {
                try {
                    BiometricGate.prompt(
                        activity = activity,
                        title = "Unlock jnotes",
                        subtitle = "Use fingerprint, face, or device PIN",
                        onSuccess = onUnlocked,
                        onError = { biometricCancelledByUser = true },
                        onFailed = { /* let the user try again or use PIN */ },
                    )
                } catch (_: Throwable) {
                    // Activity not in a valid state yet — user can still use PIN
                    // or tap the fingerprint key to retry.
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Also fire once on first composition with a tiny delay, in case the
    // activity finishes attaching its window after composition starts.
    LaunchedEffect(biometricEnabled, biometricAvailable, activity) {
        if (biometricEnabled && biometricAvailable && activity != null && !biometricCancelledByUser) {
            delay(180)
            try {
                BiometricGate.prompt(
                    activity = activity,
                    onSuccess = onUnlocked,
                    onError = { biometricCancelledByUser = true },
                )
            } catch (_: Throwable) { /* ignore — PIN entry remains available */ }
        }
    }

    val locked = nowMs < lockoutUntilMs
    val secondsLeft = ((lockoutUntilMs - nowMs) / 1000).coerceAtLeast(0)

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
        ),
    )

    val shakeAnim by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "shake",
        finishedListener = { shake = false },
    )
    val shakeOffsetDp = (kotlin.math.sin(shakeAnim * Math.PI * 4).toFloat() * 6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Header ─────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "jnotes is locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (locked) "Too many tries — wait $secondsLeft s"
                    else if (biometricAvailable && biometricEnabled) "Use biometric or enter your PIN"
                    else "Enter your PIN to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            // ── PIN dots ───────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .scale(if (shake) 1f + shakeAnim * 0.02f else 1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .padding(start = shakeOffsetDp.dp.coerceAtLeast(0.dp))
                        .padding(end = (-shakeOffsetDp).dp.coerceAtLeast(0.dp)),
                ) {
                    val displayLength = 6.coerceAtLeast(pin.length).coerceAtMost(8)
                    repeat(displayLength) { index ->
                        val filled = index < pin.length
                        val errorTint = error != null
                        val color = when {
                            errorTint -> MaterialTheme.colorScheme.error
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (filled) 14.dp else 12.dp)
                                .background(
                                    color = if (filled) color else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = color,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = error != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // ── Keypad ─────────────────────────────────────────────
            Keypad(
                enabled = !locked,
                showBiometric = biometricEnabled && biometricAvailable && activity != null,
                onDigit = { d ->
                    if (locked) return@Keypad
                    if (pin.length < 8) {
                        pin += d
                        error = null
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (pin.length >= 4) tryUnlock(
                            pin = pin,
                            pinHash = pinHash,
                            pinSalt = pinSalt,
                            onSuccess = onUnlocked,
                            onWrong = {
                                attempts++
                                if (attempts >= 5) {
                                    val cooldownSec = (15L shl (attempts - 5).coerceAtMost(4)).coerceAtMost(15L * 16)
                                    lockoutUntilMs = System.currentTimeMillis() + cooldownSec * 1000
                                    error = "Locked for ${cooldownSec}s"
                                } else {
                                    error = "Incorrect PIN — ${5 - attempts} tries left"
                                }
                                shake = true
                                pin = ""
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        )
                    }
                },
                onBackspace = {
                    if (locked || pin.isEmpty()) return@Keypad
                    pin = pin.dropLast(1)
                    error = null
                },
                onBiometric = {
                    if (activity != null) {
                        biometricCancelledByUser = false
                        try {
                            BiometricGate.prompt(
                                activity = activity,
                                onSuccess = onUnlocked,
                                onError = { biometricCancelledByUser = true },
                            )
                        } catch (_: Throwable) { /* PIN remains available */ }
                    }
                },
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

/** PBKDF2 / SHA-256 verification with auto-unlock on success. */
private inline fun tryUnlock(
    pin: String,
    pinHash: String,
    pinSalt: String?,
    onSuccess: () -> Unit,
    onWrong: () -> Unit,
) {
    if (Hashing.verifyPin(pin, pinHash, pinSalt)) onSuccess() else onWrong()
}

@Composable
private fun Keypad(
    enabled: Boolean,
    showBiometric: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { d -> KeypadKey(label = d, enabled = enabled) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Bottom-left slot: face / fingerprint biometric button (if available).
            if (showBiometric) {
                KeypadIconKey(
                    icon = Icons.Outlined.Fingerprint,
                    contentDescription = "Use fingerprint or face",
                    enabled = enabled,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onBiometric,
                )
            } else {
                Spacer(Modifier.size(72.dp))
            }
            KeypadKey(label = "0", enabled = enabled) { onDigit("0") }
            KeypadIconKey(
                icon = Icons.Outlined.Backspace,
                contentDescription = "Delete",
                enabled = enabled,
                onClick = onBackspace,
            )
        }
        if (showBiometric) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Face & fingerprint enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f),
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun KeypadIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f),
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
