package com.day.app.notifications

import com.day.app.domain.model.Priority
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationTest {

    @Test
    fun testNotificationChannelsConstants() {
        assertEquals("com.day.app.channel.normal", NotificationChannels.CHANNEL_NORMAL)
        assertEquals("com.day.app.channel.important", NotificationChannels.CHANNEL_IMPORTANT)
        assertEquals("com.day.app.channel.critical", NotificationChannels.CHANNEL_CRITICAL)

        assertArrayEquals(longArrayOf(0, 150), NotificationChannels.VIBRATION_NORMAL)
        assertArrayEquals(longArrayOf(0, 250, 150, 250), NotificationChannels.VIBRATION_IMPORTANT)
        assertArrayEquals(longArrayOf(0, 300, 150, 300, 150, 600), NotificationChannels.VIBRATION_CRITICAL)
    }

    @Test
    fun testReminderStages() {
        val stages = ReminderStage.values()
        assertEquals(4, stages.size)
        assertNotNull(ReminderStage.valueOf("UPCOMING_30M"))
        assertNotNull(ReminderStage.valueOf("UPCOMING_10M"))
        assertNotNull(ReminderStage.valueOf("AT_TIME"))
        assertNotNull(ReminderStage.valueOf("DEADLINE_OVERDUE"))
    }

    @Test
    fun testNotificationPriorityLevel() {
        val levels = NotificationPriorityLevel.values()
        assertEquals(3, levels.size)
        assertNotNull(NotificationPriorityLevel.valueOf("NORMAL"))
        assertNotNull(NotificationPriorityLevel.valueOf("IMPORTANT"))
        assertNotNull(NotificationPriorityLevel.valueOf("CRITICAL"))
    }
}
