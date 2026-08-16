package com.stride.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.stride.core.common.WeatherCondition
import kotlin.math.sin
import kotlin.random.Random

/**
 * A live, condition-driven background — not a static icon. Respects [LocalReducedMotion]: falls
 * back to a still frame (particles/rays drawn at their t=0 position) rather than looping motion.
 * Deliberately simple shapes (Canvas primitives), not illustration assets — see docs/foundation.md
 * "Weather" for why a runner should get real information here, not just decoration.
 */
@Composable
fun WeatherAnimation(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "weather")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (reducedMotion) 1 else 4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "weatherT",
    )
    val progress = if (reducedMotion) 0f else t

    Canvas(modifier = modifier.fillMaxSize()) {
        when (condition) {
            WeatherCondition.CLEAR -> drawClear(progress, isDay, size.minDimension)
            WeatherCondition.PARTLY_CLOUDY -> {
                drawClear(progress, isDay, size.minDimension * 0.7f)
                drawClouds(progress, count = 2)
            }
            WeatherCondition.CLOUDY -> drawClouds(progress, count = 4)
            WeatherCondition.FOG -> drawFog(progress)
            WeatherCondition.RAIN -> {
                drawClouds(progress, count = 2)
                drawRain(progress, dropCount = 40)
            }
            WeatherCondition.SNOW -> {
                drawClouds(progress, count = 2)
                drawSnow(progress, flakeCount = 30)
            }
            WeatherCondition.THUNDERSTORM -> {
                drawClouds(progress, count = 3)
                drawRain(progress, dropCount = 55)
                drawLightningFlash(progress)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClear(t: Float, isDay: Boolean, diameter: Float) {
    val center = Offset(size.width / 2, size.height * 0.35f)
    val radius = diameter * 0.16f
    val color = if (isDay) Color(0xFFFFC24D) else Color(0xFFE7ECFA)
    if (isDay) {
        rotate(degrees = t * 360f, pivot = center) {
            repeat(8) { i ->
                val angle = (i / 8f) * 2 * Math.PI
                val start = Offset(
                    center.x + (radius * 1.3f * kotlin.math.cos(angle)).toFloat(),
                    center.y + (radius * 1.3f * kotlin.math.sin(angle)).toFloat(),
                )
                val end = Offset(
                    center.x + (radius * 1.8f * kotlin.math.cos(angle)).toFloat(),
                    center.y + (radius * 1.8f * kotlin.math.sin(angle)).toFloat(),
                )
                drawLine(color.copy(alpha = .8f), start, end, strokeWidth = 4f)
            }
        }
    }
    drawCircle(color, radius = radius, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClouds(t: Float, count: Int) {
    repeat(count) { i ->
        val baseY = size.height * (0.2f + 0.12f * i)
        val speed = 0.5f + i * 0.15f
        val x = ((t * speed + i * 0.33f) % 1.2f - 0.1f) * size.width
        val cloudWidth = size.width * 0.32f
        drawRoundRect(
            color = Color.White.copy(alpha = .55f),
            topLeft = Offset(x, baseY),
            size = androidx.compose.ui.geometry.Size(cloudWidth, cloudWidth * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cloudWidth * 0.2f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFog(t: Float) {
    repeat(4) { i ->
        val y = size.height * (0.25f + i * 0.18f)
        val alpha = 0.25f + 0.1f * sin((t * 2 * Math.PI + i).toFloat())
        drawRect(Color.White.copy(alpha = alpha.coerceIn(0.1f, 0.4f)), topLeft = Offset(0f, y), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.08f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRain(t: Float, dropCount: Int) {
    val rnd = Random(dropCount)
    repeat(dropCount) {
        val xFrac = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val yFrac = (t + phase) % 1f
        val x = xFrac * size.width
        val y = yFrac * size.height
        drawLine(
            color = Color(0xFF9FCBFF).copy(alpha = .8f),
            start = Offset(x, y),
            end = Offset(x - 3f, y + 14f),
            strokeWidth = 2.5f,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnow(t: Float, flakeCount: Int) {
    val rnd = Random(flakeCount)
    repeat(flakeCount) {
        val xFrac = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val yFrac = (t + phase) % 1f
        val sway = (sin((t * 2 * Math.PI * 2 + phase * 10).toFloat()) * 10f)
        val x = xFrac * size.width + sway
        val y = yFrac * size.height
        drawCircle(Color.White.copy(alpha = .9f), radius = 3f, center = Offset(x, y))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightningFlash(t: Float) {
    val flashWindow = (t % 0.5f)
    if (flashWindow < 0.04f) {
        drawRect(Color.White.copy(alpha = .25f))
    }
}
