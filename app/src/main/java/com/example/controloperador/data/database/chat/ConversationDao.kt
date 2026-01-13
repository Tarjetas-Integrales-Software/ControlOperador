package com.example.controloperador.data.database.chat

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO para acceder a conversaciones de chat
 */
@Dao
interface ConversationDao {
    
    /**
     * Obtiene la conversación del operador (debería ser única)
     */
    @Query("SELECT * FROM conversations WHERE operator_code = :operatorCode LIMIT 1")
    suspend fun getConversationByOperatorCode(operatorCode: String): Conversation?
    
    /**
     * Obtiene la conversación del operador como LiveData para observar cambios
     */
    @Query("SELECT * FROM conversations WHERE operator_code = :operatorCode LIMIT 1")
    fun getConversationByOperatorCodeLive(operatorCode: String): LiveData<Conversation?>
    
    /**
     * Inserta una nueva conversación
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)
    
    /**
     * Actualiza una conversación existente
     */
    @Update
    suspend fun updateConversation(conversation: Conversation)
    
    /**
     * Actualiza el timestamp del último mensaje
     */
    @Query("UPDATE conversations SET last_message_at = :timestamp WHERE id = :conversationId")
    suspend fun updateLastMessageTimestamp(conversationId: String, timestamp: Long)
    
    /**
     * Incrementa el contador de mensajes no leídos
     */
    @Query("UPDATE conversations SET unread_count = unread_count + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)
    
    /**
     * Resetea el contador de mensajes no leídos
     */
    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :conversationId")
    suspend fun resetUnreadCount(conversationId: String)
    
    /**
     * Elimina una conversación
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
    
    /**
     * Elimina todas las conversaciones (para testing)
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
