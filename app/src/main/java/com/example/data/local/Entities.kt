package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persistent_memories")
data class PersistentMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyName: String,
    val valueContent: String,
    val category: String = "general", // "preference", "fact", "topic"
    val isUserApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_turns")
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val language: String,
    val evidenceLevel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val asrLatencyMs: Long = 0,
    val llmLatencyMs: Long = 0,
    val ttsLatencyMs: Long = 0,
    val renderLatencyMs: Long = 0
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val eventType: String,
    val details: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0
)
