package com.example.controloperador.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Modelos para Chat Unificado (Texto y Voz)
 * Reemplaza los modelos separados de TextMessage y VoiceMessage
 */

/**
 * Mensaje unificado que puede ser de texto o voz
 */
data class UnifiedMessage(
    @SerializedName("id") val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("operator_code") val operatorCode: String,
    @SerializedName("message_type") val messageType: String, // "TEXT" o "VOICE"
    @SerializedName("sender_type") val senderType: String, // "ANALISTA" o "OPERADOR"
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("read_at") val readAt: String?,
    
    // Campos para mensajes de TEXTO
    @SerializedName("content") val content: String? = null,
    
    // Campos para mensajes de VOZ
    @SerializedName("audio_path") val audioPath: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("mime_type") val mimeType: String? = null
) {
    /**
     * Verifica si el mensaje es de tipo texto
     */
    fun isTextMessage(): Boolean = messageType == "TEXT"
    
    /**
     * Verifica si el mensaje es de tipo voz
     */
    fun isVoiceMessage(): Boolean = messageType == "VOICE"
    
    /**
     * Verifica si el mensaje fue enviado por el analista
     */
    fun isFromAnalyst(): Boolean = senderType == "ANALISTA"
    
    /**
     * Verifica si el mensaje fue enviado por el operador
     */
    fun isFromOperator(): Boolean = senderType == "OPERADOR"
    
    /**
     * Verifica si el mensaje ha sido leído
     */
    fun isRead(): Boolean = readAt != null
    
    /**
     * Obtiene el contenido del mensaje (texto o indicador de voz)
     */
    fun getDisplayContent(): String {
        return when {
            isTextMessage() -> content ?: ""
            isVoiceMessage() -> "🎙️ Mensaje de voz (${formatDuration(duration ?: 0)})"
            else -> "Mensaje desconocido"
        }
    }
    
    /**
     * Formatea la duración en formato MM:SS
     */
    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", mins, secs)
    }
}

/**
 * Respuesta de la API con todos los mensajes del operador
 */
data class OperatorMessagesResponse(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("operator_code") val operatorCode: String,
    @SerializedName("messages") val messages: List<UnifiedMessage>
)

/**
 * Respuesta de mensajes sin leer
 */
data class UnreadMessagesResponse(
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("messages") val messages: List<UnifiedMessage>
)

/**
 * Request para marcar mensajes como leídos (chat unificado operador)
 */
data class OperatorMarkReadRequest(
    @SerializedName("operator_code") val operatorCode: String,
    @SerializedName("message_ids") val messageIds: List<String>? = null // null = marcar todos
)

/**
 * Respuesta de marcar como leído (chat unificado operador)
 */
data class OperatorMarkReadResponse(
    @SerializedName("marked_count") val markedCount: Int,
    @SerializedName("remaining_unread") val remainingUnread: Int
)

/**
 * Request para enviar respuesta del operador
 */
data class SendResponseRequest(
    @SerializedName("operator_code") val operatorCode: String,
    @SerializedName("content") val content: String
)
