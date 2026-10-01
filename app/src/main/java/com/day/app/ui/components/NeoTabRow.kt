package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurface
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DaySurfaceHighlight
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

data class NeoTabItem(
    val title: String,
    val count: Int? = null
)

@Composable
fun NeoTabRow(
    tabs: List<NeoTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val pillShape = RoundedCornerShape(999.dp)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val tabScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1.0f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 500f),
                label = "tab_scale"
            )

            val pillBg by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0x0EFFFFFF),
                label = "tab_bg"
            )
            val pillText by animateColorAsState(
                targetValue = if (isSelected) DayBackground else DayTextSecondary,
                label = "tab_text"
            )
            val borderAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.12f,
                label = "tab_border"
            )

            Box(
                modifier = Modifier
                    .scale(tabScale)
                    .clip(pillShape)
                    .background(pillBg, pillShape)
                    .border(
                        0.5.dp,
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = borderAlpha),
                                Color.White.copy(alpha = borderAlpha * 0.3f)
                            )
                        ),
                        pillShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.title,
                        color = pillText,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    if (tab.count != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${tab.count}",
                            color = if (isSelected) DayBackground.copy(alpha = 0.7f) else DayTextTertiary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
