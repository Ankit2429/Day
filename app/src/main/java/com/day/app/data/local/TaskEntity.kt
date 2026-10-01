package com.day.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.day.app.domain.model.Priority
import com.day.app.domain.model.ReminderEndCondition
import com.day.app.domain.model.RepeatType
import com.day.app.domain.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val scheduledTime: Long?,
    val deadline: Long?,
    val priority: Priority,
    val completed: Boolean,
    val completedAt: Long?,
    val repeatType: RepeatType,
    val repeatRule: String?,
    val reminderEnabled: Boolean,
    val reminderInterval: Long?,
    val reminderEndCondition: ReminderEndCondition,
    val linkedDocumentId: Long? = null,
    val linkedNoteId: Long? = null
) {
    fun toDomain(): Task = Task(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        scheduledTime = scheduledTime,
        deadline = deadline,
        priority = priority,
        completed = completed,
        completedAt = completedAt,
        repeatType = repeatType,
        repeatRule = repeatRule,
        reminderEnabled = reminderEnabled,
        reminderInterval = reminderInterval,
        reminderEndCondition = reminderEndCondition,
        linkedDocumentId = linkedDocumentId,
        linkedNoteId = linkedNoteId
    )

    companion object {
        fun fromDomain(task: Task): TaskEntity = TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            scheduledTime = task.scheduledTime,
            deadline = task.deadline,
            priority = task.priority,
            completed = task.completed,
            completedAt = task.completedAt,
            repeatType = task.repeatType,
            repeatRule = task.repeatRule,
            reminderEnabled = task.reminderEnabled,
            reminderInterval = task.reminderInterval,
            reminderEndCondition = task.reminderEndCondition,
            linkedDocumentId = task.linkedDocumentId,
            linkedNoteId = task.linkedNoteId
        )
    }
}
