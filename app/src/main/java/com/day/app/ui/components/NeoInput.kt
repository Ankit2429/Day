package com.day.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBorder
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DaySurface
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun NeoInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    errorMessage: String? = null,
    helperText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = errorMessage != null

    val surfaceAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 0.09f else 0.055f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "input_surface"
    )

    val borderTopAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            isError -> 0.8f
            isFocused -> 0.45f
            else -> 0.16f
        },
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "input_border"
    )

    val shape = RoundedCornerShape(12.dp)
    val surfaceBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.Color.White.copy(alpha = surfaceAlpha * 1.3f),
            androidx.compose.ui.graphics.Color.White.copy(alpha = surfaceAlpha * 0.7f)
        )
    )
    val borderBrush = if (isError) {
        androidx.compose.ui.graphics.SolidColor(DayDestructive)
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = borderTopAlpha),
                androidx.compose.ui.graphics.Color.White.copy(alpha = borderTopAlpha * 0.35f)
            )
        )
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(androidx.compose.ui.graphics.Color(0xCC0C0C0E), shape)
                .background(surfaceBrush, shape)
                .border(androidx.compose.foundation.BorderStroke(0.5.dp, borderBrush), shape)
                .padding(horizontal = 14.dp, vertical = if (singleLine) 11.dp else 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = DayTextDisabled,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = DayTextPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        singleLine = singleLine,
                        maxLines = maxLines,
                        cursorBrush = SolidColor(DayTextPrimary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = DayDestructive,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
