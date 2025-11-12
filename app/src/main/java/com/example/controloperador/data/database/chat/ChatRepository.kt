package com.example.controloperador.data.database.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.controloperador.data.api.ChatApiService
import com.example.controloperador.data.api.Result
import com.example.controloperador.data.api.model.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio para gestionar chat entre operadores y analistas
 * Maneja sincronización bidireccional entre Room y el servidor
 */
class ChatRepository(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val chatApiService: ChatApiService
) {
    
    companion object {
        private const val TAG = "ChatRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private val DATE_FORMAT_ISO8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    /**
     * Obtiene o crea la conversación del operador
     */
    suspend fun getOrCreateConversation(operatorCode: String): Conversation {
        return withContext(Dispatchers.IO) {
            var conversation = conversationDao.getConversationByOperatorCode(operatorCode)
            if (conversation == null) {
                conversation = Conversation(
                    operatorCode = operatorCode,
                    createdAt = Date(),
                    lastMessageAt = Date()
                )
                conversationDao.insertConversation(conversation)
                Log.d(TAG, "Created new conversation for operator: $operatorCode")
            }
            conversation
        }
    }
    
    /**
     * Obtiene la conversación como LiveData
     */
    fun getConversationLive(operatorCode: String): LiveData<Conversation?> {
        return conversationDao.getConversationByOperatorCodeLive(operatorCode)
    }
    
    /**
     * Obtiene los mensajes del día actual como LiveData
     */
    fun getTodayMessagesLive(conversationId: String): LiveData<List<ChatMessage>> {
        return chatMessageDao.getTodayMessagesLive(conversationId)
    }
    
    /**
     * Obtiene el conteo de mensajes no leídos como LiveData
     */
    fun getUnreadCountLive(conversationId: String): LiveData<Int> {
        return chatMessageDao.getUnreadCountLive(conversationId)
    }
    
    /**
     * Obtiene los últimos N mensajes del día para preview
     */
    suspend fun getRecentTodayMessages(conversationId: String, limit: Int = 3): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            chatMessageDao.getRecentTodayMessages(conversationId, limit).reversed()
        }
    }
    
    /**
     * Envía un mensaje del operador
     * 1. Guarda en Room con estado PENDING
     * 2. Intenta enviar al servidor
     * 3. Actualiza estado según resultado
     */
    suspend fun sendMessage(
        operatorCode: String,
        content: String,
        isPredefinedResponse: Boolean = false,
        predefinedResponseId: String? = null
    ): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Obtener o crear conversación
                val conversation = getOrCreateConversation(operatorCode)
                
                // 2. Crear mensaje local
                val localMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversation.id,
                    content = content,
                    senderType = SenderType.OPERADOR,
                    senderId = operatorCode,
                    senderName = "Yo",
                    createdAt = Date(),
                    syncStatus = SyncStatus.PENDING,
                    isPredefinedResponse = isPredefinedResponse,
                    predefinedResponseId = predefinedResponseId
                )
                
                // 3. Guardar en Room
                chatMessageDao.insertMessage(localMessage)
                conversationDao.updateLastMessageTimestamp(conversation.id, localMessage.createdAt.time)
                
                Log.d(TAG, "Message saved locally: ${localMessage.id}")
                
                // 4. Intentar enviar al servidor
                val sendResult = sendToServer(localMessage, operatorCode)
                
                when (sendResult) {
                    is Result.Success -> {
                        // Actualizar con ID del servidor y estado SENT
                        chatMessageDao.updateSyncStatus(
                            localMessage.id,
                            SyncStatus.SENT,
                            sendResult.data.data?.id
                        )
                        Log.d(TAG, "Message sent successfully: ${sendResult.data.data?.id}")
                        Result.Success(localMessage.copy(
                            syncStatus = SyncStatus.SENT,
                            serverId = sendResult.data.data?.id
                        ))
                    }
                    
                    is Result.Error -> {
                        // Marcar como FAILED
                        chatMessageDao.updateSyncStatus(localMessage.id, SyncStatus.FAILED)
                        Log.e(TAG, "Failed to send message: ${sendResult.message}")
                        Result.Error(sendResult.message, sendResult.code)
                    }
                    
                    is Result.NetworkError -> {
                        // Mantener como PENDING para retry
                        Log.w(TAG, "Network error sending message, will retry later")
                        Result.Success(localMessage) // Mensaje guardado, se intentará después
                    }
                    
                    is Result.Timeout -> {
                        Log.w(TAG, "Timeout sending message, will retry later")
                        Result.Success(localMessage)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                Result.Error("Error al enviar mensaje: ${e.message}")
            }
        }
    }
    
    /**
     * Envía un mensaje al servidor
     */
    private suspend fun sendToServer(message: ChatMessage, operatorCode: String): Result<SendMessageResponse> {
        return try {
            val request = SendMessageRequest(
                operatorCode = operatorCode,
                content = message.content,
                senderType = message.senderType.name, // "OPERADOR" o "ANALISTA"
                senderId = message.senderId, // operator_code para operador
                isPredefinedResponse = message.isPredefinedResponse,
                predefinedResponseId = message.predefinedResponseId,
                localId = message.id
            )
            
            val response = chatApiService.sendMessage(request)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.Success(body)
                } else {
                    Result.Error(body.message)
                }
            } else {
                Result.Error("Error del servidor: ${response.code()}", response.code())
            }
            
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout sending to server", e)
            Result.Timeout
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending to server", e)
            Result.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending to server", e)
            Result.Error("Error inesperado: ${e.message}")
        }
    }
    
    /**
     * Reintenta enviar mensajes pendientes
     */
    suspend fun retryPendingMessages(conversationId: String, operatorCode: String): Int {
        return withContext(Dispatchers.IO) {
            val pendingMessages = chatMessageDao.getPendingMessages(conversationId)
            var successCount = 0
            
            pendingMessages.forEach { message ->
                val result = sendToServer(message, operatorCode)
                if (result is Result.Success) {
                    chatMessageDao.updateSyncStatus(
                        message.id,
                        SyncStatus.SENT,
                        result.data.data?.id
                    )
                    successCount++
                }
            }
            
            Log.d(TAG, "Retried $successCount/${pendingMessages.size} pending messages")
            successCount
        }
    }
    
    /**
     * Obtiene mensajes nuevos del servidor (polling cada 30s)
     */
    suspend fun fetchNewMessages(conversationId: String, operatorCode: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener ID del último mensaje sincronizado
                val lastServerId = chatMessageDao.getLastSyncedServerId(conversationId)
                
                Log.d(TAG, "🔍 Fetching new messages for operator: $operatorCode")
                Log.d(TAG, "📡 Last synced server ID: $lastServerId")
                Log.d(TAG, "🌐 Calling API: GET secomsa/chat/messages/today")
                Log.d(TAG, "📝 Parameters: operator_code=$operatorCode, last_id=$lastServerId")
                
                // Llamar a la API con query parameters
                val response = chatApiService.getTodayMessages(operatorCode, lastServerId)
                
                Log.d(TAG, "📥 API Response code: ${response.code()}")
                Log.d(TAG, "📦 API Response successful: ${response.isSuccessful}")
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    Log.d(TAG, "✅ Response body received")
                    Log.d(TAG, "   success: ${body.success}")
                    Log.d(TAG, "   message: ${body.message}")
                    Log.d(TAG, "   data: ${body.data}")
                    
                    if (body.success && body.data != null) {
                        Log.d(TAG, "📝 Messages in response: ${body.data.messages.size}")
                        
                        val newMessages = body.data.messages.map { messageData ->
                            Log.d(TAG, "   - Message: ${messageData.id} | ${messageData.senderType} | ${messageData.content}")
                            mapApiMessageToLocal(messageData, conversationId, operatorCode)
                        }
                        
                        if (newMessages.isNotEmpty()) {
                            // Insertar nuevos mensajes
                            chatMessageDao.insertMessages(newMessages)
                            Log.d(TAG, "💾 Inserted ${newMessages.size} messages into Room")
                            
                            // Marcar mensajes de analista como no leídos (read_at = null)
                            // Se marcarán como leídos cuando el usuario los vea
                            
                            // Actualizar timestamp de conversación
                            val lastMessage = newMessages.maxByOrNull { it.createdAt }
                            lastMessage?.let {
                                conversationDao.updateLastMessageTimestamp(conversationId, it.createdAt.time)
                            }
                            
                            // Actualizar conteo de no leídos
                            val unreadFromAnalyst = newMessages.count { 
                                it.senderType == SenderType.ANALISTA && it.readAt == null 
                            }
                            
                            if (unreadFromAnalyst > 0) {
                                repeat(unreadFromAnalyst) {
                                    conversationDao.incrementUnreadCount(conversationId)
                                }
                            }
                            
                            Log.d(TAG, "✅ Fetched ${newMessages.size} new messages ($unreadFromAnalyst unread)")
                        } else {
                            Log.d(TAG, "ℹ️ No new messages to insert")
                        }
                        
                        Result.Success(newMessages.size)
                    } else {
                        Log.e(TAG, "❌ API returned success=false: ${body.message}")
                        Result.Error(body.message)
                    }
                } else {
                    Log.e(TAG, "❌ HTTP Error ${response.code()}: ${response.message()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "   Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "   Could not read error body")
                    }
                    Result.Error("Error del servidor: ${response.code()}", response.code())
                }
                
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "⏱️ Timeout fetching messages", e)
                Result.Timeout
            } catch (e: IOException) {
                Log.e(TAG, "🌐 Network error fetching messages", e)
                Result.NetworkError
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error fetching messages", e)
                Result.Error("Error inesperado: ${e.message}")
            }
        }
    }
    
    /**
     * Marca mensajes como leídos localmente y en el servidor
     */
    suspend fun markMessagesAsRead(conversationId: String, messageIds: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Marcar localmente primero
                val readAt = System.currentTimeMillis()
                messageIds.forEach { messageId ->
                    chatMessageDao.markAsRead(messageId, readAt)
                }
                
                // Resetear contador de no leídos
                conversationDao.resetUnreadCount(conversationId)
                
                Log.d(TAG, "Marked ${messageIds.size} messages as read locally")
                
                // Intentar sincronizar con el servidor
                val request = MarkAsReadRequest(messageIds)
                val response = chatApiService.markMessagesAsRead(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Log.d(TAG, "Marked ${body.data?.markedCount} messages as read on server")
                        Result.Success(Unit)
                    } else {
                        Result.Error(body.message)
                    }
                } else {
                    // Ya están marcados localmente, no es crítico
                    Log.w(TAG, "Failed to mark as read on server, but local marks persisted")
                    Result.Success(Unit)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error marking messages as read", e)
                // Ya están marcados localmente
                Result.Success(Unit)
            }
        }
    }
    
    /**
     * Marca todos los mensajes del día como leídos
     */
    suspend fun markAllTodayAsRead(conversationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                chatMessageDao.markAllTodayAsRead(conversationId)
                conversationDao.resetUnreadCount(conversationId)
                Log.d(TAG, "Marked all today messages as read")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all as read", e)
                Result.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Limpia mensajes más antiguos que 30 días
     */
    suspend fun cleanOldMessages(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val beforeDate = calendar.timeInMillis
                
                val deletedCount = chatMessageDao.deleteMessagesOlderThan(beforeDate)
                Log.d(TAG, "Cleaned $deletedCount messages older than 30 days")
                
                Result.Success(deletedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning old messages", e)
                Result.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Obtiene respuestas predefinidas del servidor
     */
    suspend fun getPredefinedResponses(): Result<List<PredefinedResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.getPredefinedResponses()
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success && body.data != null) {
                        Log.d(TAG, "Fetched ${body.data.responses.size} predefined responses")
                        Result.Success(body.data.responses.filter { it.activo })
                    } else {
                        Result.Error(body.message)
                    }
                } else {
                    Result.Error("Error del servidor: ${response.code()}", response.code())
                }
                
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout fetching predefined responses", e)
                Result.Timeout
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching predefined responses", e)
                Result.NetworkError
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching predefined responses", e)
                Result.Error("Error inesperado: ${e.message}")
            }
        }
    }
    
    /**
     * Mapea un mensaje de la API a la entidad local
     */
    private fun mapApiMessageToLocal(
        apiMessage: MessageData,
        conversationId: String,
        currentOperatorCode: String
    ): ChatMessage {
        val createdAt = try {
            DATE_FORMAT_ISO8601.parse(apiMessage.createdAt) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val readAt = apiMessage.readAt?.let {
            try {
                DATE_FORMAT_ISO8601.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        val senderType = if (apiMessage.senderType == "OPERADOR") {
            SenderType.OPERADOR
        } else {
            SenderType.ANALISTA
        }
        
        return ChatMessage(
            id = UUID.randomUUID().toString(), // ID local único
            conversationId = conversationId,
            content = apiMessage.content,
            senderType = senderType,
            senderId = apiMessage.senderId,
            senderName = if (senderType == SenderType.OPERADOR) "Yo" else "Soporte",
            createdAt = createdAt,
            syncStatus = SyncStatus.SENT, // Ya está en el servidor
            readAt = readAt,
            serverId = apiMessage.id // ID del servidor
        )
    }
}
