package com.example.controloperador.data.database.chat

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Date

/**
 * DAO para acceder a mensajes de chat
 */
@Dao
interface ChatMessageDao {
    
    /**
     * Obtiene todos los mensajes de una conversación del día actual
     * Ordenados por fecha ascendente (más antiguo arriba, más nuevo abajo)
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
        ORDER BY created_at ASC
    """)
    fun getTodayMessagesLive(conversationId: String): LiveData<List<ChatMessage>>
    
    /**
     * Obtiene todos los mensajes de una conversación del día actual (suspend)
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
        ORDER BY created_at ASC
    """)
    suspend fun getTodayMessages(conversationId: String): List<ChatMessage>
    
    /**
     * Obtiene los últimos N mensajes del día para preview (ej: HomeFragment)
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentTodayMessages(conversationId: String, limit: Int = 3): List<ChatMessage>
    
    /**
     * Obtiene el último mensaje de una conversación
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    suspend fun getLastMessage(conversationId: String): ChatMessage?
    
    /**
     * Obtiene mensajes pendientes de sincronizar (para reintento)
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND sync_status = 'PENDING'
        ORDER BY created_at ASC
    """)
    suspend fun getPendingMessages(conversationId: String): List<ChatMessage>
    
    /**
     * Obtiene mensajes fallidos (para mostrar error en UI)
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND sync_status = 'FAILED'
        ORDER BY created_at ASC
    """)
    suspend fun getFailedMessages(conversationId: String): List<ChatMessage>
    
    /**
     * Obtiene el conteo de mensajes no leídos del día actual
     * (mensajes de ANALISTA que no tienen read_at)
     */
    @Query("""
        SELECT COUNT(*) FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND sender_type = 'ANALISTA'
        AND read_at IS NULL
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
    """)
    suspend fun getUnreadCount(conversationId: String): Int
    
    /**
     * Obtiene el conteo de mensajes no leídos como LiveData
     */
    @Query("""
        SELECT COUNT(*) FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND sender_type = 'ANALISTA'
        AND read_at IS NULL
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
    """)
    fun getUnreadCountLive(conversationId: String): LiveData<Int>
    
    /**
     * Obtiene mensajes más antiguos que N días
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE created_at < :beforeDate
    """)
    suspend fun getMessagesOlderThan(beforeDate: Long): List<ChatMessage>
    
    /**
     * Inserta un nuevo mensaje
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
    
    /**
     * Inserta múltiples mensajes (para sincronización en lote)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)
    
    /**
     * Actualiza un mensaje existente
     */
    @Update
    suspend fun updateMessage(message: ChatMessage)
    
    /**
     * Actualiza el estado de sincronización de un mensaje
     */
    @Query("""
        UPDATE chat_messages 
        SET sync_status = :status, server_id = :serverId
        WHERE id = :messageId
    """)
    suspend fun updateSyncStatus(messageId: String, status: SyncStatus, serverId: String? = null)
    
    /**
     * Marca un mensaje como leído
     */
    @Query("""
        UPDATE chat_messages 
        SET read_at = :readAt 
        WHERE id = :messageId
    """)
    suspend fun markAsRead(messageId: String, readAt: Long = System.currentTimeMillis())
    
    /**
     * Marca todos los mensajes de ANALISTA del día como leídos
     */
    @Query("""
        UPDATE chat_messages 
        SET read_at = :readAt 
        WHERE conversation_id = :conversationId 
        AND sender_type = 'ANALISTA'
        AND read_at IS NULL
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
    """)
    suspend fun markAllTodayAsRead(conversationId: String, readAt: Long = System.currentTimeMillis())
    
    /**
     * Elimina mensajes más antiguos que N días (limpieza automática)
     */
    @Query("""
        DELETE FROM chat_messages 
        WHERE created_at < :beforeDate
    """)
    suspend fun deleteMessagesOlderThan(beforeDate: Long): Int
    
    /**
     * Elimina un mensaje específico
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    /**
     * Elimina todos los mensajes de una conversación
     */
    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteAllMessages(conversationId: String)
    
    /**
     * Obtiene el ID del último mensaje sincronizado (para paginación en API)
     */
    @Query("""
        SELECT server_id FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND server_id IS NOT NULL
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    suspend fun getLastSyncedServerId(conversationId: String): String?
}
