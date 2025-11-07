package com.example.controloperador.data.database

import androidx.room.TypeConverter
import com.example.controloperador.data.database.chat.SenderType
import com.example.controloperador.data.database.chat.SyncStatus
import java.util.Date

/**
 * Convertidores de tipo para Room Database
 * Convierte tipos complejos a tipos primitivos que Room puede almacenar
 * 
 * Incluye conversores para:
 * - Date <-> Long (AttendanceLog y ChatMessage)
 * - SenderType <-> String (ChatMessage)
 * - SyncStatus <-> String (ChatMessage)
 */
class Converters {
    
    // ==================== Date Converters ====================
    
    /**
     * Convierte timestamp (Long) a Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Convierte Date a timestamp (Long)
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    // ==================== Chat Enums Converters ====================
    
    /**
     * Convierte SenderType enum a String
     */
    @TypeConverter
    fun fromSenderType(value: SenderType): String {
        return value.name
    }
    
    /**
     * Convierte String a SenderType enum
     */
    @TypeConverter
    fun toSenderType(value: String): SenderType {
        return SenderType.valueOf(value)
    }
    
    /**
     * Convierte SyncStatus enum a String
     */
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }
    
    /**
     * Convierte String a SyncStatus enum
     */
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}
