package com.notp9194bot.jnotes.ui.common

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Renders a list of media attachments (image / video / audio / file) for a
 * note. Images are shown inline (tap to zoom). Audio plays in-line with a
 * play / pause button. Video shows a thumbnail + play overlay (tap to open
 * in the system video player). Files show a row with name + open action.
 */
@Composable
fun NoteAttachmentsView(
    uris: List<String>,
    kinds: List<String>,
    onDelete: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uris.isEmpty()) return
    val ctx = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Media (${uris.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        uris.forEachIndexed { index, uriStr ->
            val kind = kinds.getOrNull(index) ?: guessKind(uriStr)
            val uri = remember(uriStr) { Uri.parse(uriStr) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (kind) {
                        "image" -> ImageAttachmentRow(uri = uri, onClick = { onImageClick(index) })
                        "video" -> VideoAttachmentRow(
                            uri = uri,
                            onOpen = { openExternally(ctx, uri, "video/*") },
                        )
                        "audio" -> AudioAttachmentRow(uri = uri)
                        else -> FileAttachmentRow(
                            uri = uri,
                            onOpen = { openExternally(ctx, uri, ctx.contentResolver.getType(uri)) },
                        )
                    }
                    IconButton(
                        onClick = { onDelete(uriStr) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove attachment",
                            tint = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun guessKind(uri: String): String {
    val lower = uri.lowercase()
    return when {
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp") -> "image"
        lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") ||
            lower.endsWith(".mkv") || lower.endsWith(".3gp") -> "video"
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") ||
            lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flac") -> "audio"
        else -> "file"
    }
}

@Composable
private fun ImageAttachmentRow(uri: Uri, onClick: () -> Unit) {
    val ctx = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 28) {
                    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.setTargetSampleSize(2)
                    }.asImageBitmap()
                } else {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Loading image…", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Image(
            bitmap = bmp,
            contentDescription = "Photo attachment",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() },
        )
    }
}

@Composable
private fun VideoAttachmentRow(uri: Uri, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    var thumb by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        thumb = withContext(Dispatchers.IO) {
            runCatching {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(ctx, uri)
                val frame = mmr.getFrameAtTime(0L) ?: mmr.frameAtTime
                mmr.release()
                frame?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .clickable { onOpen() },
        contentAlignment = Alignment.Center,
    ) {
        thumb?.let {
            Image(
                bitmap = it,
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun AudioAttachmentRow(uri: Uri) {
    val ctx = LocalContext.current
    val player = remember(uri) { MediaPlayer() }
    var prepared by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var error by remember(uri) { mutableStateOf<String?>(null) }

    DisposableEffect(uri) {
        runCatching {
            player.setDataSource(ctx, uri)
            player.setOnPreparedListener {
                prepared = true
                durationMs = it.duration.toLong().coerceAtLeast(0L)
            }
            player.setOnCompletionListener {
                playing = false
                positionMs = 0L
                runCatching { player.seekTo(0) }
            }
            player.setOnErrorListener { _, _, _ ->
                error = "Cannot play audio"
                true
            }
            player.prepareAsync()
        }.onFailure { error = "Cannot open audio" }
        onDispose {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            positionMs = runCatching { player.currentPosition.toLong() }.getOrDefault(positionMs)
            delay(250)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                .clickable(enabled = prepared && error == null) {
                    if (player.isPlaying) {
                        player.pause(); playing = false
                    } else {
                        runCatching { player.start() }; playing = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = uri.lastPathSegment?.substringAfterLast('/')?.take(40) ?: "Audio",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            // Simple progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        RoundedCornerShape(2.dp),
                    ),
            ) {
                val frac =
                    if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                error ?: "${formatTime(positionMs)}  /  ${formatTime(durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun FileAttachmentRow(uri: Uri, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                uri.lastPathSegment?.substringAfterLast('/')?.take(50) ?: uri.toString().take(50),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Tap to open",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
    }
}

private fun openExternally(ctx: android.content.Context, uri: Uri, mime: String?) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(Intent.createChooser(intent, "Open with")) }
}

@Suppress("MagicNumber")
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
