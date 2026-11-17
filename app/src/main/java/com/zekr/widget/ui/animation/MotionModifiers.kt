package com.zekr.widget.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Modern tactile spring physics for touch interactions.
 * Compresses slightly on press and springs back naturally on release.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val animatedScale = remember { Animatable(1.0f) }
    val currentOnClick by rememberUpdatedState(onClick)

    if (!enabled) return@composed this

    this
        .graphicsLayer {
            scaleX = animatedScale.value
            scaleY = animatedScale.value
        }
        .pointerInput(enabled, scaleDown) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        animatedScale.animateTo(
                            targetValue = scaleDown,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        animatedScale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    if (released) {
                        currentOnClick()
                    }
                }
            )
        }
}

/**
 * Luminous metallic / glass light sheen that animates across the surface
 */
fun Modifier.shimmerGleam(
    enabled: Boolean = true,
    highlightColor: Color = Color.White.copy(alpha = 0.35f),
    durationMillis: Int = 2600
): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmer_gleam")
    val translateAnim by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gleam_translation"
    )

    this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height

        val xOffset = translateAnim * width
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                highlightColor.copy(alpha = 0.08f),
                highlightColor,
                highlightColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(xOffset, 0f),
            end = Offset(xOffset + width * 0.45f, height)
        )
        drawRect(brush = brush)
    }
}

