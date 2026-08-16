package com.stride.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

enum class RunnerMode { WALK, RUN, STRETCH }

/**
 * A procedural stick-figure — plain Canvas draw calls, not an illustration asset — animated to
 * read clearly as walking, running, or stretching at a glance on the active-run screen. Respects
 * [LocalReducedMotion]: holds a single mid-motion frame instead of looping.
 */
@Composable
fun RunnerFigure(mode: RunnerMode, modifier: Modifier = Modifier, color: Color = Color.White) {
    val reducedMotion = LocalReducedMotion.current
    val periodMs = when (mode) {
        RunnerMode.RUN -> 700
        RunnerMode.WALK -> 1100
        RunnerMode.STRETCH -> 2200
    }
    val transition = rememberInfiniteTransition(label = "runner")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (reducedMotion) 1 else periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "runnerT",
    )
    val phase = if (reducedMotion) 0.25f else t

    Canvas(modifier = modifier) {
        when (mode) {
            RunnerMode.WALK -> drawGaitFigure(phase, color, strideAmplitudeDeg = 28f, bob = size.height * 0.02f, lean = 2f)
            RunnerMode.RUN -> drawGaitFigure(phase, color, strideAmplitudeDeg = 50f, bob = size.height * 0.05f, lean = 10f)
            RunnerMode.STRETCH -> drawStretchFigure(phase, color)
        }
    }
}

private fun DrawScope.drawGaitFigure(phase: Float, color: Color, strideAmplitudeDeg: Float, bob: Float, lean: Float) {
    val cx = size.width / 2
    val groundY = size.height * 0.92f
    val hipY = groundY - size.height * 0.42f - bob * sin(phase * 2 * Math.PI).toFloat().let { if (it > 0) it else 0f }
    val hip = Offset(cx, hipY)
    val legLength = size.height * 0.38f
    val armLength = size.height * 0.26f
    val torsoLength = size.height * 0.28f
    val headRadius = size.height * 0.07f

    val swing = sin(phase * 2 * Math.PI).toFloat() // -1..1
    val legAngleL = Math.toRadians((strideAmplitudeDeg * swing).toDouble()).toFloat()
    val legAngleR = Math.toRadians((strideAmplitudeDeg * -swing).toDouble()).toFloat()
    val armAngleL = -legAngleL * 0.7f
    val armAngleR = -legAngleR * 0.7f

    val leanRad = Math.toRadians(lean.toDouble()).toFloat()
    val shoulder = Offset(hip.x + torsoLength * sin(leanRad), hip.y - torsoLength * cos(leanRad))
    val head = Offset(shoulder.x + headRadius * sin(leanRad) * 2, shoulder.y - headRadius * 2.2f)

    val stroke = size.minDimension * 0.035f

    fun limbEnd(origin: Offset, angle: Float, length: Float, baseTilt: Float = 0f): Offset {
        val a = angle + baseTilt
        return Offset(origin.x + length * sin(a), origin.y + length * cos(a))
    }

    val footL = limbEnd(hip, legAngleL, legLength)
    val footR = limbEnd(hip, legAngleR, legLength)
    val handL = limbEnd(shoulder, armAngleL, armLength, baseTilt = Math.PI.toFloat())
    val handR = limbEnd(shoulder, armAngleR, armLength, baseTilt = Math.PI.toFloat())

    drawLine(color, hip, footL, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, hip, footR, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, shoulder, handL, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
    drawLine(color, shoulder, handR, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
    drawLine(color, hip, shoulder, strokeWidth = stroke, cap = StrokeCap.Round)
    drawCircle(color, radius = headRadius, center = head)
}

/** A gentle reach-and-hold pose (front leg forward, arms overhead), swaying slowly — reads as
 * "stretching" without needing to depict a specific exercise. */
private fun DrawScope.drawStretchFigure(phase: Float, color: Color) {
    val cx = size.width / 2
    val groundY = size.height * 0.92f
    val sway = sin(phase * 2 * Math.PI).toFloat() * 0.12f
    val hip = Offset(cx, groundY - size.height * 0.4f)
    val legLength = size.height * 0.38f
    val armLength = size.height * 0.3f
    val torsoLength = size.height * 0.28f
    val headRadius = size.height * 0.07f
    val stroke = size.minDimension * 0.035f

    val shoulder = Offset(hip.x + sway * size.width, hip.y - torsoLength)
    val head = Offset(shoulder.x, shoulder.y - headRadius * 2.2f)
    val frontFoot = Offset(hip.x + legLength * 0.55f, groundY)
    val backFoot = Offset(hip.x - legLength * 0.45f, groundY)
    val reachUp = 1f + sway
    val handL = Offset(shoulder.x - armLength * 0.5f, shoulder.y - armLength * reachUp)
    val handR = Offset(shoulder.x + armLength * 0.5f, shoulder.y - armLength * reachUp)

    drawLine(color, hip, frontFoot, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, hip, backFoot, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, shoulder, handL, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
    drawLine(color, shoulder, handR, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
    drawLine(color, hip, shoulder, strokeWidth = stroke, cap = StrokeCap.Round)
    drawCircle(color, radius = headRadius, center = head)
}
