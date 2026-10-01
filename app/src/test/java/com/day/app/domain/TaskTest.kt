package com.day.app.domain

import com.day.app.domain.model.Priority
import com.day.app.domain.model.RepeatType
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun testUrgencyCalculation_overdueDeadline() {
        val now = 1000000000L
        val pastDeadline = now - 60000L // 1 minute ago

        val urgency = Urgency.calculate(
            priority = Priority.LOW,
            scheduledTime = null,
            deadline = pastDeadline,
            isCompleted = false,
            now = now
        )
        assertEquals(Urgency.OVERDUE, urgency)
    }

    @Test
    fun testUrgencyCalculation_withinTwoHours() {
        val now = 1000000000L
        val deadlineInOneHour = now + (3600 * 1000L)

        val urgency = Urgency.calculate(
            priority = Priority.LOW,
            scheduledTime = null,
            deadline = deadlineInOneHour,
            isCompleted = false,
            now = now
        )
        assertEquals(Urgency.URGENT, urgency)
    }

    @Test
    fun testUrgencyCalculation_completedTaskAlwaysNormal() {
        val now = 1000000000L
        val pastDeadline = now - 60000L

        val urgency = Urgency.calculate(
            priority = Priority.HIGH,
            scheduledTime = null,
            deadline = pastDeadline,
            isCompleted = true,
            now = now
        )
        assertEquals(Urgency.NORMAL, urgency)
    }

    @Test
    fun testUrgencyBlocks_representation() {
        val now = 1000000000L
        val urgentTask = Task(
            title = "Urgent assignment",
            deadline = now + (20 * 60 * 1000L), // 20 mins remaining -> 8 blocks
            priority = Priority.HIGH
        )
        assertEquals("████████", urgentTask.urgencyBlocks(now))
        assertEquals(8, urgentTask.matrixLedCount(now))

        val sixHourTask = Task(
            title = "Project",
            deadline = now + (5 * 3600 * 1000L), // 5 hours remaining -> 4 blocks
            priority = Priority.MEDIUM
        )
        assertEquals("████", sixHourTask.urgencyBlocks(now))

        val overdueTask = Task(
            title = "Late item",
            deadline = now - 1000L,
            priority = Priority.HIGH
        )
        assertEquals("ALERT", overdueTask.urgencyBlocks(now))
        assertEquals(8, overdueTask.matrixLedCount(now))
    }

    @Test
    fun testDeadlineProgress() {
        val start = 1000000000L
        val deadline = start + 100000L
        val now = start + 50000L // 50%

        val task = Task(
            title = "Test",
            scheduledTime = start,
            deadline = deadline
        )

        val progress = task.deadlineProgress(now)
        assertEquals(0.5f, progress, 0.01f)
    }
}
