package com.example.controloperador.data

import com.example.controloperador.data.model.MessageType
import com.example.controloperador.data.model.MessagesSummary
import com.example.controloperador.data.model.TextMessage
import com.example.controloperador.data.model.VoiceMessage
import java.util.Date
import java.util.UUID

/**
 * Repositorio para gestionar mensajes de texto y voz
 * TODO: Implementar con API REST y base de datos local (Room)
 */
class MessageRepository {
    
    // Datos mock para desarrollo
    private val textMessages = mutableListOf(
        TextMessage(
            id = "1",
            content = "Buenos días operador. Favor reportar status de ruta.",
            timestamp = Date(System.currentTimeMillis() - 3600000), // Hace 1 hora
            isFromOperator = false,
            senderName = "Central de Control",
            isRead = false
        ),
        TextMessage(
            id = "2",
            content = MessageType.getFormattedMessage(MessageType.TRAFFIC_STOPPED),
            timestamp = Date(System.currentTimeMillis() - 7200000), // Hace 2 horas
            isFromOperator = true,
            senderName = "Yo",
            messageType = MessageType.TRAFFIC_STOPPED,
            isRead = true
        ),
        TextMessage(
            id = "3",
            content = "Entendido. Mantenga comunicación constante.",
            timestamp = Date(System.currentTimeMillis() - 7260000),
            isFromOperator = false,
            senderName = "Central de Control",
            isRead = true
        ),
        TextMessage(
            id = "4",
            content = "¿Cuál es su ubicación actual?",
            timestamp = Date(System.currentTimeMillis() - 600000), // Hace 10 min
            isFromOperator = false,
            senderName = "Despachador",
            isRead = false
        )
    )
    
    private val voiceMessages = mutableListOf(
        VoiceMessage(
            id = "v1",
            audioUrl = null,
            audioFilePath = null,
            duration = 45,
            timestamp = Date(System.currentTimeMillis() - 1800000), // Hace 30 min
            senderName = "Central de Control",
            isPlayed = false,
            transcription = "Favor de confirmar recepción de pasajeros en parada 15."
        ),
        VoiceMessage(
            id = "v2",
            audioUrl = null,
            audioFilePath = null,
            duration = 32,
            timestamp = Date(System.currentTimeMillis() - 5400000), // Hace 1.5 horas
            senderName = "Supervisor",
            isPlayed = true,
            transcription = "Buen trabajo en la ruta de hoy. Continúa así."
        ),
        VoiceMessage(
            id = "v3",
            audioUrl = null,
            audioFilePath = null,
            duration = 58,
            timestamp = Date(System.currentTimeMillis() - 300000), // Hace 5 min
            senderName = "Despachador",
            isPlayed = false,
            transcription = "Atención: Cambio de ruta debido a manifestación."
        )
    )
    
    /**
     * Obtiene el resumen de mensajes para la pantalla principal
     */
    fun getMessagesSummary(): MessagesSummary {
        val unreadText = textMessages.count { !it.isRead && !it.isFromOperator }
        val unplayedVoice = voiceMessages.count { !it.isPlayed }
        
        // Últimos 3 mensajes de texto
        val recentText = textMessages
            .sortedByDescending { it.timestamp }
            .take(3)
        
        // Últimos 3 mensajes de voz
        val recentVoice = voiceMessages
            .sortedByDescending { it.timestamp }
            .take(3)
        
        return MessagesSummary(
            unreadTextMessages = unreadText,
            recentTextMessages = recentText,
            unplayedVoiceMessages = unplayedVoice,
            recentVoiceMessages = recentVoice
        )
    }
    
    /**
     * Obtiene todos los mensajes de texto ordenados por fecha
     */
    fun getAllTextMessages(): List<TextMessage> {
        return textMessages.sortedBy { it.timestamp }
    }
    
    /**
     * Obtiene todos los mensajes de voz ordenados por fecha
     */
    fun getAllVoiceMessages(): List<VoiceMessage> {
        return voiceMessages.sortedByDescending { it.timestamp }
    }
    
    /**
     * Envía un mensaje de respuesta predeterminada
     * @param messageType Tipo de respuesta seleccionada
     * @return El mensaje enviado
     * 
     * TODO: Implementar con llamada a API
     */
    fun sendPredefinedResponse(messageType: MessageType): TextMessage {
        val newMessage = TextMessage(
            id = UUID.randomUUID().toString(),
            content = MessageType.getFormattedMessage(messageType),
            timestamp = Date(),
            isFromOperator = true,
            senderName = "Yo",
            messageType = messageType,
            isRead = true
        )
        
        textMessages.add(newMessage)
        
        // TODO: Enviar a API
        // apiService.sendMessage(newMessage)
        
        return newMessage
    }
    
    /**
     * Marca un mensaje de texto como leído
     */
    fun markTextMessageAsRead(messageId: String) {
        val index = textMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            textMessages[index] = textMessages[index].copy(isRead = true)
            // TODO: Actualizar en API
        }
    }
    
    /**
     * Marca un mensaje de voz como reproducido
     */
    fun markVoiceMessageAsPlayed(messageId: String) {
        val index = voiceMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            voiceMessages[index] = voiceMessages[index].copy(isPlayed = true)
            // TODO: Actualizar en API
        }
    }
    
    /**
     * Obtiene las respuestas predeterminadas disponibles
     */
    fun getPredefinedResponses(): List<MessageType> {
        return MessageType.getAllResponses()
    }
}
