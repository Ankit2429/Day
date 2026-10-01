package com.day.app.data.repository

import com.day.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getActiveTasks(): Flow<List<Task>>
    suspend fun getActiveTasksSync(): List<Task>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getTaskById(id: Long): Flow<Task?>
    suspend fun getTaskByIdSync(id: Long): Task?
    fun getTodayTasks(): Flow<List<Task>>
    suspend fun saveTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: Long)
    suspend fun completeTask(id: Long, completed: Boolean): Task?
}
