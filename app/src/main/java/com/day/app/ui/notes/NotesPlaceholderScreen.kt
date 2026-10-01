package com.day.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.day.app.ui.components.NeoBadge
import com.day.app.ui.components.NeoBadgeVariant
import com.day.app.ui.components.NeoEmptyState
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.theme.NeoPaper

@Composable
fun NotesPlaceholderScreen(
    onNavigateToTasks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoPaper)
    ) {
        NeoHeader(
            title = "NOTES",
            actionButton = {
                NeoBadge(
                    text = "V2 ROADMAP",
                    variant = NeoBadgeVariant.MAGENTA
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(40.dp))
                NeoEmptyState(
                    title = "COMING LATER",
                    description = "Notes and brutalist scratchpads are planned for a subsequent version.\nTasks, priority schedules, notifications, and ESP8266 matrix integration are active in V1.",
                    actionText = "VIEW ACTIVE TASKS",
                    onActionClick = onNavigateToTasks,
                    icon = Icons.Outlined.Description,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
