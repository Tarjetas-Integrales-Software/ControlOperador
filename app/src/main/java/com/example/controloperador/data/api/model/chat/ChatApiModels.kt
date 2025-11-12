package com.example.controloperador.data.api.model.chat

import com.google.gson.annotations.SerializedName

/**
 * Modelo para respuesta predefinida desde el servidor
 */
data class PredefinedResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("mensaje")
    val mensaje: String,
    
    @SerializedName("categoria")
    val categoria: String? = null,
    
    @SerializedName("orden")
    val orden: Int = 0,
    
    @SerializedName("activo")
    val activo: Boolean = true
)

/**
 * Request para enviar un mensaje
 */
data class SendMessageRequest(
    @SerializedName("operator_code")
    val operatorCode: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("sender_type")
    val senderType: String, // "OPERADOR" o "ANALISTA"
    
    @SerializedName("sender_id")
    val senderId: String, // operator_code para operador, user_id para analista
    
    @SerializedName("is_predefined_response")
    val isPredefinedResponse: Boolean = false,
    
    @SerializedName("predefined_response_id")
    val predefinedResponseId: String? = null,
    
    @SerializedName("local_id")
    val localId: String // UUID local para tracking
)

/**
 * Request para obtener mensajes del día
 */
data class TodayMessagesRequest(
    @SerializedName("operator_code")
    val operatorCode: String,
    
    @SerializedName("last_id")
    val lastId: String? = null
)

/**
 * Response al enviar un mensaje
 */
data class SendMessageResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: MessageData?
)

data class MessageData(
    @SerializedName("id")
    val id: String, // ID del mensaje en el servidor
    
    @SerializedName("conversation_id")
    val conversationId: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("sender_type")
    val senderType: String, // "OPERADOR" o "ANALISTA"
    
    @SerializedName("sender_id")
    val senderId: String,
    
    @SerializedName("created_at")
    val createdAt: String, // ISO 8601 UTC
    
    @SerializedName("read_at")
    val readAt: String? = null
)

/**
 * Response de mensajes del día
 */
data class TodayMessagesResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: TodayMessagesData?
)

data class TodayMessagesData(
    @SerializedName("conversation_id")
    val conversationId: String,
    
    @SerializedName("messages")
    val messages: List<MessageData>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("unread_count")
    val unreadCount: Int
)

/**
 * Request para marcar mensaje como leído
 */
data class MarkAsReadRequest(
    @SerializedName("message_ids")
    val messageIds: List<String>
)

/**
 * Response de marcar como leído
 */
data class MarkAsReadResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: MarkAsReadData?
)

data class MarkAsReadData(
    @SerializedName("marked_count")
    val markedCount: Int,
    
    @SerializedName("read_at")
    val readAt: String // ISO 8601 UTC
)

/**
 * Response de respuestas predefinidas
 */
data class PredefinedResponsesResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: PredefinedResponsesData?
)

data class PredefinedResponsesData(
    @SerializedName("responses")
    val responses: List<PredefinedResponse>,
    
    @SerializedName("total")
    val total: Int
)
