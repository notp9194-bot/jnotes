package com.notp9194bot.jnotes.ui.common

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File

/**
 * Records an audio clip to the app's private files directory and returns
 * the resulting content:// URI via [onSaved] when the user taps Save.
 *
 * If the RECORD_AUDIO permission has not yet been granted, asks for it
 * the first time the dialog opens.
 */
@Composable
fun AudioRecorderDialog(
    onSaved: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val askPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPerm = granted }

    LaunchedEffect(Unit) {
        if (!hasPerm) askPerm.launch(Manifest.permission.RECORD_AUDIO)
    }

    var recording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val outFile = remember {
        val dir = File(ctx.filesDir, "attachments").apply { mkdirs() }
        File(dir, "rec_${System.currentTimeMillis()}.m4a")
    }
    val recorder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
    }

    fun startRecording() {
        runCatching {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recording = true
            elapsedMs = 0L
        }.onFailure {
            error = "Could not start recording: ${it.message}"
            recording = false
        }
    }

    fun stopRecording() {
        if (!recording) return
        runCatching {
            recorder.stop()
        }.onFailure { /* ignore — short clip */ }
        recording = false
        savedUri = runCatching {
            FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                outFile,
            )
        }.getOrNull()
    }

    LaunchedEffect(recording) {
        while (recording) {
            delay(200)
            elapsedMs += 200
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { if (recording) recorder.stop() }
            runCatching { recorder.release() }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (recording) runCatching { recorder.stop() }
            onDismiss()
        },
        title = { Text("Record audio") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!hasPerm) {
                    Text(
                        "Microphone permission is required to record audio.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { askPerm.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Grant microphone")
                    }
                } else {
                    Text(
                        formatDuration(elapsedMs),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                if (recording) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            onClick = {
                                if (recording) stopRecording() else startRecording()
                            },
                            modifier = Modifier.size(96.dp),
                        ) {
                            Icon(
                                if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                                contentDescription = if (recording) "Stop" else "Record",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    Text(
                        when {
                            recording -> "Recording… tap to stop"
                            savedUri != null -> "Tap Save to attach this clip"
                            else -> "Tap the mic to start"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (savedUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(120.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Clip ready (${formatDuration(elapsedMs)})",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = savedUri != null && !recording,
                onClick = {
                    savedUri?.let { onSaved(it) }
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (recording) runCatching { recorder.stop() }
                onDismiss()
            }) { Text("Cancel") }
        },
    )
}

@Suppress("MagicNumber")
private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
