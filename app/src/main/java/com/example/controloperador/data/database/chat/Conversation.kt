package com.example.controloperador.data.database.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Entidad de conversación para chat
 * Cada operador tiene UNA conversación única con el sistema de soporte (analistas)
 */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "operator_code")
    val operatorCode: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),
    
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Date = Date(),
    
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0
)
