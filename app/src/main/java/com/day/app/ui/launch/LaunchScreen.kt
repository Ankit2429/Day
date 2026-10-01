package com.day.app.ui.launch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextTertiary
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(1250L)
        onTimeout()
    }

    val coreScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.82f,
        animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "core_scale"
    )
    val coreAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(350),
        label = "core_alpha"
    )

    val titleAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(500, delayMillis = 200),
        label = "title_alpha"
    )

    val subAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(500, delayMillis = 400),
        label = "sub_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DayBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official monochrome emblem from user's design
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.day.app.R.drawable.app_logo),
                contentDescription = "DAY Emblem",
                modifier = Modifier
                    .size(96.dp)
                    .scale(coreScale)
                    .alpha(coreAlpha)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "DAY",
                color = DayTextPrimary.copy(alpha = titleAlpha),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "YOUR DAILY OPERATING SYSTEM",
                color = DayTextTertiary.copy(alpha = subAlpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(subAlpha)
            )
        }
    }
}
