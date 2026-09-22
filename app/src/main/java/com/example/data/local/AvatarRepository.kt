package com.example.data.local

import com.example.model.AuditLogEntry
import com.example.model.ConversationTurn
import com.example.model.EvidenceClassification
import com.example.model.OperationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AvatarRepository(private val database: AvatarDatabase) {
    private val memoryDao = database.memoryDao()
    private val conversationDao = database.conversationDao()
    private val auditLogDao = database.auditLogDao()

    val allMemories: Flow<List<PersistentMemoryEntity>> = memoryDao.getAllMemories()
    val allHistory: Flow<List<ConversationTurnEntity>> = conversationDao.getAllHistory()
    val allLogs: Flow<List<AuditLogEntry>> = auditLogDao.getAllLogs().map { entities ->
        entities.map { entity ->
            AuditLogEntry(
                id = entity.id,
                timestamp = entity.timestamp,
                eventType = entity.eventType,
                details = entity.details,
                status = try {
                    OperationStatus.valueOf(entity.status)
                } catch (e: Exception) {
                    OperationStatus.SUCCEEDED
                },
                latencyMs = entity.latencyMs
            )
        }
    }

    suspend fun getApprovedMemoriesSync(): List<PersistentMemoryEntity> {
        return memoryDao.getApprovedMemoriesSync()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Long {
        val existing = memoryDao.getMemoryByKey(key)
        return if (existing != null) {
            val updated = existing.copy(
                valueContent = value,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
            memoryDao.update(updated)
            existing.id
        } else {
            memoryDao.insert(
                PersistentMemoryEntity(
                    keyName = key,
                    valueContent = value,
                    category = category,
                    isUserApproved = true
                )
            )
        }
    }

    suspend fun updateMemory(memory: PersistentMemoryEntity) {
        memoryDao.update(memory.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemoryById(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun clearMemories() {
        memoryDao.clearAll()
    }

    suspend fun saveConversationTurn(sessionId: String, turn: ConversationTurn): Long {
        return conversationDao.insertTurn(
            ConversationTurnEntity(
                sessionId = sessionId,
                role = turn.role,
                content = turn.content,
                language = turn.language,
                evidenceLevel = turn.evidenceLevel.name,
                timestamp = turn.timestamp,
                asrLatencyMs = turn.asrLatencyMs,
                llmLatencyMs = turn.llmLatencyMs,
                ttsLatencyMs = turn.ttsLatencyMs,
                renderLatencyMs = turn.renderLatencyMs
            )
        )
    }

    fun getSessionTurns(sessionId: String): Flow<List<ConversationTurn>> {
        return conversationDao.getSessionTurns(sessionId).map { entities ->
            entities.map { entity ->
                ConversationTurn(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    language = entity.language,
                    evidenceLevel = try {
                        EvidenceClassification.valueOf(entity.evidenceLevel)
                    } catch (e: Exception) {
                        EvidenceClassification.SUPPORTED
                    },
                    timestamp = entity.timestamp,
                    asrLatencyMs = entity.asrLatencyMs,
                    llmLatencyMs = entity.llmLatencyMs,
                    ttsLatencyMs = entity.ttsLatencyMs,
                    renderLatencyMs = entity.renderLatencyMs
                )
            }
        }
    }

    suspend fun clearHistory() {
        conversationDao.clearAllHistory()
    }

    suspend fun logAudit(
        sessionId: String,
        eventType: String,
        details: String,
        status: OperationStatus = OperationStatus.SUCCEEDED,
        latencyMs: Long = 0
    ) {
        auditLogDao.insertLog(
            AuditLogEntity(
                sessionId = sessionId,
                eventType = eventType,
                details = details,
                status = status.name,
                timestamp = System.currentTimeMillis(),
                latencyMs = latencyMs
            )
        )
    }

    suspend fun clearLogs() {
        auditLogDao.clearLogs()
    }
}
