package com.example.controloperador.data.model

import java.util.Date

/**
 * Modelo de mensaje de texto
 * Para comunicación entre operador y central/despachador
 */
data class TextMessage(
    val id: String,
    val content: String,
    val timestamp: Date,
    val isFromOperator: Boolean, // true = enviado por operador, false = recibido
    val senderName: String,
    val messageType: MessageType? = null, // Tipo de respuesta predeterminada si aplica
    val isRead: Boolean = false
)

/**
 * Modelo de mensaje de voz
 * Solo recepción para el operador
 */
data class VoiceMessage(
    val id: String,
    val audioUrl: String?, // URL del archivo de audio (futuro: desde API)
    val audioFilePath: String?, // Path local del archivo
    val duration: Int, // Duración en segundos
    val timestamp: Date,
    val senderName: String,
    val isPlayed: Boolean = false,
    val transcription: String? = null // Transcripción automática (futuro)
)

/**
 * Tipos de respuestas predeterminadas para el operador
 * El operador solo puede responder con estas opciones
 */
enum class MessageType(val displayName: String, val icon: String) {
    MECHANICAL_FAILURE("FALLA MECÁNICA", "🔧"),
    FLAT_TIRE("NEUMÁTICO PONCHADO", "🛞"),
    ACCIDENT("SINIESTRO", "🚨"),
    TRAFFIC_STOPPED("TRÁFICO DETENIDO", "🚦"),
    DETOUR("DESVIACIÓN", "↪️"),
    PREPAID_EQUIPMENT_FAILURE("FALLA EN EQUIPO DE PREPAGO", "💳");
    
    companion object {
        /**
         * Obtiene el mensaje completo formateado
         */
        fun getFormattedMessage(type: MessageType): String {
            return "${type.icon} ${type.displayName}"
        }
        
        /**
         * Lista de todas las respuestas disponibles
         */
        fun getAllResponses(): List<MessageType> {
            return values().toList()
        }
    }
}

/**
 * Resumen de mensajes para mostrar en HomeFragment
 */
data class MessagesSummary(
    val unreadTextMessages: Int,
    val recentTextMessages: List<TextMessage>,
    val unplayedVoiceMessages: Int,
    val recentVoiceMessages: List<VoiceMessage>
)
