package com.example.controloperador.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Modelos de datos para la API de mensajes de voz
 * Basado en ANDROID_VOICE_MESSAGES_API.md
 */

/**
 * Conversación con mensajes de voz
 */
data class VoiceConversation(
    @SerializedName("conversation_id")
    val conversationId: String,
    
    @SerializedName("operator_code")
    val operatorCode: String,
    
    @SerializedName("operator_name")
    val operatorName: String?,
    
    @SerializedName("operator_apellido_paterno")
    val operatorApellidoPaterno: String?,
    
    @SerializedName("operator_apellido_materno")
    val operatorApellidoMaterno: String?,
    
    @SerializedName("analyst_name")
    val analystName: String?,
    
    @SerializedName("last_message_date")
    val lastMessageDate: String,
    
    @SerializedName("last_audio_duration")
    val lastAudioDuration: Int,
    
    @SerializedName("unread_count")
    val unreadCount: Int,
    
    @SerializedName("total_messages")
    val totalMessages: Int
) {
    /**
     * Obtiene el nombre completo del operador
     */
    val fullName: String
        get() {
            val parts = listOfNotNull(
                operatorName,
                operatorApellidoPaterno,
                operatorApellidoMaterno
            )
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Operador $operatorCode"
        }
    
    /**
     * Indica si hay mensajes sin leer
     */
    val hasUnreadMessages: Boolean
        get() = unreadCount > 0
}

/**
 * Mensaje de voz individual
 */
data class VoiceMessageDetail(
    val id: Int,
    
    @SerializedName("conversation_id")
    val conversationId: String,
    
    @SerializedName("operator_code")
    val operatorCode: String,
    
    @SerializedName("sender_type")
    val senderType: String, // Siempre "ANALISTA" para el operador
    
    @SerializedName("audio_path")
    val audioPath: String,
    
    @SerializedName("audio_url")
    val audioUrl: String,
    
    val duration: Int, // Duración en segundos
    
    @SerializedName("file_size")
    val fileSize: Int,
    
    @SerializedName("mime_type")
    val mimeType: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("read_at")
    val readAt: String?
) {
    /**
     * Indica si el mensaje ya fue leído
     */
    val isRead: Boolean
        get() = readAt != null
    
    /**
     * Duración formateada como "MM:SS"
     */
    val formattedDuration: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    
    /**
     * Tamaño del archivo formateado (KB o MB)
     */
    val formattedFileSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize bytes"
                fileSize < 1024 * 1024 -> String.format("%.1f KB", fileSize / 1024.0)
                else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
            }
        }
}

/**
 * Respuesta de la API para obtener conversaciones
 * Backend devuelve UN objeto con los datos, no un array
 */
data class ConversationsResponse(
    val response: Boolean,
    val data: ConversationData,
    val message: String,
    val status: Int
)

/**
 * Datos de la conversación del operador
 */
data class ConversationData(
    @SerializedName("operator_code")
    val operatorCode: String,
    
    @SerializedName("operator_name")
    val operatorName: String?,
    
    @SerializedName("operator_apellido_paterno")
    val operatorApellidoPaterno: String?,
    
    @SerializedName("operator_apellido_materno")
    val operatorApellidoMaterno: String?,
    
    @SerializedName("conversation_id")
    val conversationId: String,
    
    val messages: List<VoiceMessageDetail>,
    
    @SerializedName("unread_count")
    val unreadCount: Int,
    
    @SerializedName("total_messages")
    val totalMessages: Int
)

/**
 * Respuesta de la API para obtener mensajes de una conversación
 */
data class ConversationMessagesResponse(
    val response: Boolean,
    val data: MessagesData,
    val message: String,
    val status: Int
)

/**
 * Datos de mensajes dentro de la respuesta
 */
data class MessagesData(
    val messages: List<VoiceMessageDetail>
)

/**
 * Request para marcar mensajes como leídos
 */
data class MarkReadRequest(
    @SerializedName("conversation_id")
    val conversationId: String
)

/**
 * Respuesta de la API al marcar como leído
 */
data class MarkReadResponse(
    val response: Boolean,
    val data: MarkReadData,
    val message: String,
    val status: Int
)

/**
 * Datos de respuesta al marcar como leído
 */
data class MarkReadData(
    @SerializedName("marked_count")
    val markedCount: Int
)
