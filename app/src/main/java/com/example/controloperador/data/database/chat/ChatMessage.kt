package com.example.controloperador.data.database.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Entidad de mensaje de chat
 * Representa un mensaje enviado por un operador o recibido de un analista
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversation_id"]),
        Index(value = ["created_at"]),
        Index(value = ["sync_status"])
    ]
)
data class ChatMessage(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "sender_type")
    val senderType: SenderType,
    
    /**
     * ID del remitente:
     * - Si es OPERADOR: código de 5 dígitos (ej: "12345")
     * - Si es ANALISTA: users.id del backend (ej: "42")
     */
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    
    /**
     * Nombre del remitente para mostrar en UI:
     * - Si es OPERADOR: "Yo"
     * - Si es ANALISTA: "Soporte" (sin revelar identidad específica)
     */
    @ColumnInfo(name = "sender_name")
    val senderName: String = if (senderType == SenderType.OPERADOR) "Yo" else "Soporte",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),
    
    /**
     * Estado de sincronización con el servidor
     */
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    /**
     * Fecha en que el mensaje fue leído por el destinatario
     * - Si OPERADOR envió: cuando analista lo leyó
     * - Si ANALISTA envió: cuando operador lo leyó (auto al cargar)
     */
    @ColumnInfo(name = "read_at")
    val readAt: Date? = null,
    
    /**
     * ID del mensaje en el servidor (después de sincronizar)
     */
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    
    /**
     * Indica si este mensaje es una respuesta predefinida
     */
    @ColumnInfo(name = "is_predefined_response")
    val isPredefinedResponse: Boolean = false,
    
    /**
     * ID de la respuesta predefinida (si aplica)
     */
    @ColumnInfo(name = "predefined_response_id")
    val predefinedResponseId: String? = null
) {
    /**
     * Verifica si el mensaje fue enviado por el operador actual
     */
    fun isFromCurrentOperator(operatorCode: String): Boolean {
        return senderType == SenderType.OPERADOR && senderId == operatorCode
    }
    
    /**
     * Verifica si el mensaje está pendiente de sincronizar
     */
    fun isPending(): Boolean = syncStatus == SyncStatus.PENDING
    
    /**
     * Verifica si el mensaje fue enviado exitosamente
     */
    fun isSent(): Boolean = syncStatus == SyncStatus.SENT
    
    /**
     * Verifica si el mensaje falló al enviarse
     */
    fun isFailed(): Boolean = syncStatus == SyncStatus.FAILED
    
    /**
     * Verifica si el mensaje fue leído
     */
    fun isRead(): Boolean = readAt != null
}

/**
 * Tipo de remitente del mensaje
 */
enum class SenderType {
    OPERADOR,  // Mensaje enviado por el operador
    ANALISTA   // Mensaje enviado por un analista desde el panel web
}

/**
 * Estado de sincronización del mensaje con el servidor
 */
enum class SyncStatus {
    PENDING,   // Esperando ser enviado al servidor
    SENT,      // Enviado exitosamente al servidor
    FAILED     // Falló el envío (se reintentará)
}
