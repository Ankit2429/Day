package com.day.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * THE DAY — Motion Design System
 * Physical, intentional, soft spring and easing specifications.
 */
object DayMotion {
    const val DURATION_FAST = 160
    const val DURATION_NORMAL = 240
    const val DURATION_SLOW = 400

    val EasingSmooth = FastOutSlowInEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fastTween(): androidx.compose.animation.core.TweenSpec<T> = tween(durationMillis = DURATION_FAST, easing = EasingSmooth)
    fun <T> normalTween(): androidx.compose.animation.core.TweenSpec<T> = tween(durationMillis = DURATION_NORMAL, easing = EasingSmooth)
    fun <T> slowTween(): androidx.compose.animation.core.TweenSpec<T> = tween(durationMillis = DURATION_SLOW, easing = EasingEmphasized)

    // Spring specs for physical touch interactions
    fun <T> springTouch(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.75f,
        stiffness = 500f
    )

    fun <T> springSelection(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springBouncy(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.65f,
        stiffness = 450f
    )
}
