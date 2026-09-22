package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM persistent_memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<PersistentMemoryEntity>>

    @Query("SELECT * FROM persistent_memories WHERE isUserApproved = 1 ORDER BY updatedAt DESC")
    suspend fun getApprovedMemoriesSync(): List<PersistentMemoryEntity>

    @Query("SELECT * FROM persistent_memories WHERE keyName = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): PersistentMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: PersistentMemoryEntity): Long

    @Update
    suspend fun update(memory: PersistentMemoryEntity)

    @Delete
    suspend fun delete(memory: PersistentMemoryEntity)

    @Query("DELETE FROM persistent_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM persistent_memories")
    suspend fun clearAll()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_turns WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSessionTurns(sessionId: String): Flow<List<ConversationTurnEntity>>

    @Query("SELECT * FROM conversation_turns ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<ConversationTurnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ConversationTurnEntity): Long

    @Query("DELETE FROM conversation_turns WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM conversation_turns")
    suspend fun clearAllHistory()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSessionLogs(sessionId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}
