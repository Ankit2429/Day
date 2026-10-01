package com.day.app.data.repository

import com.day.app.data.local.TaskDao
import com.day.app.data.local.TaskEntity
import com.day.app.domain.model.RepeatType
import com.day.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getActiveTasksSync(): List<Task> {
        return taskDao.getActiveTasksSync().map { it.toDomain() }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTaskById(id: Long): Flow<Task?> {
        return taskDao.getTaskById(id).map { it?.toDomain() }
    }

    override suspend fun getTaskByIdSync(id: Long): Task? {
        return taskDao.getTaskByIdSync(id)?.toDomain()
    }

    override fun getTodayTasks(): Flow<List<Task>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis - 1

        return taskDao.getTasksForToday(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveTask(task: Task): Long {
        val entity = TaskEntity.fromDomain(task)
        return if (task.id == 0L) {
            taskDao.insertTask(entity)
        } else {
            taskDao.updateTask(entity)
            task.id
        }
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(TaskEntity.fromDomain(task))
    }

    override suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun completeTask(id: Long, completed: Boolean): Task? {
        val existing = taskDao.getTaskByIdSync(id) ?: return null
        val now = System.currentTimeMillis()
        val completedAt = if (completed) now else null

        taskDao.setTaskCompleted(id, completed, completedAt, now)
        val updatedTask = existing.copy(
            completed = completed,
            completedAt = completedAt,
            updatedAt = now
        ).toDomain()

        // Handle recurring tasks on completion:
        // When completed is true and task has repeatType != NONE, spawn the next occurrence
        if (completed && existing.repeatType != RepeatType.NONE) {
            val nextScheduledTime = computeNextOccurrence(existing.scheduledTime, existing.repeatType)
            val nextDeadline = computeNextOccurrence(existing.deadline, existing.repeatType)

            val nextTask = existing.copy(
                id = 0L, // new row
                completed = false,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
                scheduledTime = nextScheduledTime,
                deadline = nextDeadline
            )
            taskDao.insertTask(nextTask)
        }

        return updatedTask
    }

    private fun computeNextOccurrence(baseTime: Long?, repeatType: RepeatType): Long? {
        if (baseTime == null) return null
        val cal = Calendar.getInstance().apply { timeInMillis = baseTime }

        when (repeatType) {
            RepeatType.NONE -> return baseTime
            RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                )
            }
            RepeatType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.CUSTOM -> cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
