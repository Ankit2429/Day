package com.day.app.ui.esp8266

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.esp8266.repository.ConnectionStatus
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoCard
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoInput
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBackgroundDark
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayMonoTechnicalStyle
import com.day.app.ui.theme.DaySuccess
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun Esp8266Screen(
    viewModel: Esp8266ViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPatternName by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DayBackground)
    ) {
        NeoHeader(
            title = "ESP8266 Device",
            subtitle = "MAX7219 Matrix · WebSocket (Port 81)",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Error banner
            state.errorMessage?.let { errorMsg ->
                NeoAlertBanner(
                    title = "Connection Error",
                    message = errorMsg,
                    variant = NeoAlertVariant.WARNING,
                    onDismiss = { viewModel.dismissError() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Success / feedback banner
            state.feedbackMessage?.let { feedbackMsg ->
                NeoAlertBanner(
                    title = "Device Status",
                    message = feedbackMsg,
                    variant = NeoAlertVariant.SUCCESS,
                    onDismiss = { viewModel.dismissFeedback() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. Technical Status Card
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DaySurfaceElevated,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val isConnected = state.connectionStatus == ConnectionStatus.CONNECTED
                    val isConnecting = state.connectionStatus == ConnectionStatus.CONNECTING
                    val isReconnecting = state.connectionStatus == ConnectionStatus.RECONNECTING

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "DEVICE",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "ESP8266",
                                color = DayTextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        // Live status pill badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    when {
                                        isConnected -> DaySuccess.copy(alpha = 0.15f)
                                        isConnecting || isReconnecting -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isConnected -> DaySuccess.copy(alpha = 0.4f)
                                        isConnecting || isReconnecting -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                        else -> Color(0xFFEF4444).copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isConnected -> DaySuccess
                                                isConnecting || isReconnecting -> Color(0xFFF59E0B)
                                                else -> Color(0xFF888888)
                                            }
                                        )
                                )
                                Text(
                                    text = when {
                                        isConnected -> "CONNECTED"
                                        isConnecting -> "CONNECTING"
                                        isReconnecting -> "RECONNECTING"
                                        else -> "DISCONNECTED"
                                    },
                                    color = when {
                                        isConnected -> DaySuccess
                                        isConnecting || isReconnecting -> Color(0xFFF59E0B)
                                        else -> Color(0xFF888888)
                                    },
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IP ADDRESS",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = state.ipAddress.ifBlank { "Not set" },
                                color = DayTextPrimary,
                                style = DayMonoTechnicalStyle,
                                fontSize = 13.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WI-FI",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isConnected) "Connected" else "Offline",
                                color = if (isConnected) DaySuccess else DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WEBSOCKET",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "ws://${state.ipAddress}:81",
                                color = DayTextPrimary,
                                style = DayMonoTechnicalStyle,
                                fontSize = 12.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT MODE",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = state.activeMode.name,
                                color = DayTextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LAST SYNC",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = state.lastSyncFormatted,
                                color = DayTextSecondary,
                                style = DayMonoTechnicalStyle,
                                fontSize = 12.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RUNNING CLOCK",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = state.runningClockFormatted,
                                color = DayTextPrimary,
                                style = DayMonoTechnicalStyle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Interactive Digital 8x8 Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "INTERACTIVE 8x8 MATRIX",
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )

                val isConnected = state.connectionStatus == ConnectionStatus.CONNECTED
                val isLive = isConnected && state.isLiveSynced

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLive -> DaySuccess
                                    isConnected -> Color(0xFFF59E0B).copy(alpha = pulseAlpha)
                                    else -> DayTextTertiary
                                }
                            )
                    )
                    Text(
                        text = when {
                            isLive -> "● LIVE SYNC"
                            isConnected -> "● CONNECTED"
                            else -> "○ OFFLINE"
                        },
                        color = when {
                            isLive -> DaySuccess
                            isConnected -> Color(0xFFF59E0B)
                            else -> DayTextTertiary
                        },
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large 280dp Interactive Matrix Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DayBackgroundDark)
                    .border(1.dp, DayBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentTool = state.currentTool
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C0C0E))
                        .border(1.dp, Color(0xFF1F1F24), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                        .pointerInput(currentTool) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                down.consume()
                                val cellSizePx = size.width / 8f
                                var lastCol = (down.position.x / cellSizePx).toInt().coerceIn(0, 7)
                                var lastRow = (down.position.y / cellSizePx).toInt().coerceIn(0, 7)

                                val isDraw = currentTool == MatrixTool.DRAW
                                viewModel.paintPixel(lastRow, lastCol, isDraw)

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        change.consume()
                                        val col = (change.position.x / cellSizePx).toInt().coerceIn(0, 7)
                                        val row = (change.position.y / cellSizePx).toInt().coerceIn(0, 7)
                                        if (row != lastRow || col != lastCol) {
                                            lastRow = row
                                            lastCol = col
                                            viewModel.paintPixel(row, col, isDraw)
                                        }
                                    }
                                } finally {
                                    viewModel.onMatrixTouchRelease()
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (r in 0 until 8) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (c in 0 until 8) {
                                    val idx = r * 8 + c
                                    val isLit = state.matrixPixels.getOrElse(idx) { false }
                                    val cellColor by animateColorAsState(
                                        targetValue = if (isLit) DayTextPrimary else Color(0xFF141417),
                                        animationSpec = tween(durationMillis = 100),
                                        label = "cellColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cellColor)
                                            .border(
                                                1.dp,
                                                if (isLit) Color.White.copy(alpha = 0.8f) else Color(0xFF26262C),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Drawing Tools Bar (DRAW, ERASE, CLEAR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = "DRAW",
                    onClick = { viewModel.setTool(MatrixTool.DRAW) },
                    modifier = Modifier.weight(1f),
                    variant = if (state.currentTool == MatrixTool.DRAW) NeoButtonVariant.FILLED_PRIMARY else NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
                NeoButton(
                    text = "ERASE",
                    onClick = { viewModel.setTool(MatrixTool.ERASE) },
                    modifier = Modifier.weight(1f),
                    variant = if (state.currentTool == MatrixTool.ERASE) NeoButtonVariant.FILLED_PRIMARY else NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
                NeoButton(
                    text = "CLEAR",
                    onClick = { viewModel.clearMatrix() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (TEST MATRIX, INVERT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = "TEST MATRIX",
                    onClick = { viewModel.applyTestMatrix() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
                NeoButton(
                    text = "INVERT",
                    onClick = { viewModel.invertMatrix() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset & Saved Patterns horizontal chips
            Text(
                text = "PRESET & SAVED PATTERNS",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset chips
                viewModel.presetPatterns.forEach { (name, rows) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF18181B))
                            .border(1.dp, DayBorderSubtle, RoundedCornerShape(20.dp))
                            .clickable { viewModel.loadPattern(rows) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = name,
                            color = DayTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Custom saved patterns
                state.savedPatterns.forEach { (name, rows) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF222226))
                            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(20.dp))
                            .clickable { viewModel.loadPattern(rows) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "★ $name",
                            color = DayTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Save Current Button chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF27272A))
                        .border(1.dp, Color(0xFF52525B), RoundedCornerShape(20.dp))
                        .clickable {
                            newPatternName = ""
                            showSaveDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ SAVE CURRENT",
                        color = DayTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Selector (CLOCK, TASKS, ALERT, CUSTOM)
            Text(
                text = "DEVICE MODE",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceMode.values().forEach { mode ->
                    val isSelected = state.activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) DayTextPrimary else Color(0xFF141416))
                            .border(
                                1.dp,
                                if (isSelected) DayTextPrimary else DayBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setDeviceMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) Color.Black else DayTextSecondary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brightness Control Slider (0 - 15)
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DaySurfaceElevated,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BRIGHTNESS",
                            color = DayTextSecondary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${state.brightness} / 15",
                            color = DayTextPrimary,
                            style = DayMonoTechnicalStyle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = state.brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 0f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = DayTextPrimary,
                            activeTrackColor = DayTextPrimary,
                            inactiveTrackColor = Color(0xFF27272A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Network Configuration
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DaySurfaceElevated,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NETWORK CONFIGURATION",
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeoInput(
                        value = state.ipAddress,
                        onValueChange = { viewModel.updateIpAddress(it) },
                        label = "ESP8266 IP",
                        placeholder = "192.168.29.16",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
                            NeoButton(
                                text = "DISCONNECT",
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.weight(1f),
                                variant = NeoButtonVariant.OUTLINE_SECONDARY
                            )
                        } else {
                            NeoButton(
                                text = "CONNECT",
                                onClick = { viewModel.connect() },
                                modifier = Modifier.weight(1f),
                                variant = NeoButtonVariant.FILLED_PRIMARY
                            )
                        }

                        NeoButton(
                            text = "TEST CONNECTION",
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.weight(1f),
                            variant = NeoButtonVariant.FILLED_PRIMARY
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Test Buttons (TEST DISPLAY, CLOCK, CLEAR, CLOCK SYNC)
            Text(
                text = "TEST BUTTONS",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = "TEST DISPLAY",
                    onClick = { viewModel.testDisplay() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 11.dp)
                )
                NeoButton(
                    text = "CLOCK SYNC",
                    onClick = { viewModel.sendClockSync() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 11.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = "USE CLOCK",
                    onClick = { viewModel.sendClock() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
                NeoButton(
                    text = "CLEAR",
                    onClick = { viewModel.clearDisplay() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Development Test Actions (SEND TEST TASK, SEND TEST ALERT)
            Text(
                text = "DEVELOPMENT TEST ACTIONS",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = "SEND TEST TASK",
                    onClick = { viewModel.sendTestTask() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
                NeoButton(
                    text = "SEND TEST ALERT",
                    onClick = { viewModel.sendTestAlert() },
                    modifier = Modifier.weight(1f),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Save Pattern Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Matrix Pattern",
                    color = DayTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name to save the current 8x8 drawing locally:",
                        color = DayTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeoInput(
                        value = newPatternName,
                        onValueChange = { newPatternName = it },
                        placeholder = "e.g. My Logo"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPatternName.isNotBlank()) {
                            viewModel.saveCurrentPattern(newPatternName)
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("SAVE", color = DayTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = DayTextSecondary)
                }
            },
            containerColor = DaySurfaceElevated
        )
    }
}
