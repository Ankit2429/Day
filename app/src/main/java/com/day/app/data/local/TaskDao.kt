package com.day.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY completed ASC, priority DESC, COALESCE(deadline, scheduledTime, createdAt) ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY priority DESC, COALESCE(deadline, scheduledTime, createdAt) ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY priority DESC, COALESCE(deadline, scheduledTime, createdAt) ASC")
    suspend fun getActiveTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND ((scheduledTime BETWEEN :startOfDay AND :endOfDay) OR (deadline BETWEEN :startOfDay AND :endOfDay)) ORDER BY COALESCE(scheduledTime, deadline) ASC")
    fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET completed = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean, completedAt: Long?, updatedAt: Long)
}
