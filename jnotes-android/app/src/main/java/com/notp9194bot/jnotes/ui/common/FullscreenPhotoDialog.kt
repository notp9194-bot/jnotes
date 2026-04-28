package com.notp9194bot.jnotes.ui.common

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Full-screen, immersive photo viewer.
 *
 *  •  Pinch to zoom (1x – 5x), double-tap to toggle 1x / 2.5x.
 *  •  Pan when zoomed.
 *  •  Swipe DOWN (or up) to dismiss when at base scale — the image fades
 *     and translates with the gesture, then the dialog closes.
 *  •  Tap × in the corner to close.
 */
@Composable
fun FullscreenPhotoDialog(
    onDismiss: () -> Unit,
    drawableRes: Int? = null,
    uri: Uri? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        FullscreenPhotoContent(
            onDismiss = onDismiss,
            drawableRes = drawableRes,
            uri = uri,
        )
    }
}

@Composable
private fun FullscreenPhotoContent(
    onDismiss: () -> Unit,
    @DrawableRes drawableRes: Int?,
    uri: Uri?,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Transform state ────────────────────────────────────────────────
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Dismiss-drag tracking
    var dragY by remember { mutableFloatStateOf(0f) }
    val backdropAlpha = remember { Animatable(1f) }

    // For URI-based images — load bitmap async
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        if (uri != null) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                        ImageDecoder.decodeBitmap(src).asImageBitmap()
                    } else {
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input)?.asImageBitmap()
                        }
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha.value)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Pinch / zoom / pan
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale.value * zoom).coerceIn(1f, 5f)
                        scope.launch { scale.snapTo(newScale) }
                        if (newScale > 1f) {
                            scope.launch { offsetX.snapTo(offsetX.value + pan.x) }
                            scope.launch { offsetY.snapTo(offsetY.value + pan.y) }
                        } else {
                            scope.launch { offsetX.snapTo(0f) }
                            scope.launch { offsetY.snapTo(0f) }
                        }
                    }
                }
                // Double-tap to toggle zoom
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scope.launch {
                                if (scale.value > 1f) {
                                    scale.animateTo(1f, tween(220))
                                    offsetX.animateTo(0f, tween(220))
                                    offsetY.animateTo(0f, tween(220))
                                } else {
                                    scale.animateTo(2.5f, tween(220))
                                }
                            }
                        },
                    )
                }
                // Vertical-drag to dismiss (only at base scale)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (abs(dragY) > 220f) {
                                    backdropAlpha.animateTo(0f, tween(160))
                                    onDismiss()
                                } else {
                                    // Snap back
                                    val anim = Animatable(dragY)
                                    dragY = 0f
                                    backdropAlpha.animateTo(1f, tween(180))
                                    anim.animateTo(0f, tween(180))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                dragY = 0f
                                backdropAlpha.animateTo(1f, tween(180))
                            }
                        },
                    ) { _, dragAmount ->
                        if (scale.value <= 1.01f) {
                            dragY += dragAmount
                            // Fade backdrop with drag distance
                            val faded = (1f - (abs(dragY) / 1200f)).coerceIn(0.2f, 1f)
                            scope.launch { backdropAlpha.snapTo(faded) }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val baseModifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value + dragY,
                )

            when {
                drawableRes != null -> Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = baseModifier,
                )
                bitmap != null -> Image(
                    bitmap = bitmap!!,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = baseModifier,
                )
            }
        }

        // Close button (top-right)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
