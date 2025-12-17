package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Servicio API para mensajes de voz
 * Endpoints específicos para que los operadores reciban y gestionen mensajes de voz
 * 
 * Basado en: ANDROID_VOICE_MESSAGES_API.md
 * 
 * IMPORTANTE: Los operadores solo pueden RECIBIR y REPRODUCIR mensajes.
 * NO pueden enviar mensajes de voz.
 */
interface VoiceMessageApiService {
    
    /**
     * Obtener todas las conversaciones con mensajes de voz del operador
     * 
     * GET /api/v1/secomsa/voice-messages/operator/conversations
     * 
     * Retorna una lista de conversaciones que incluyen:
     * - ID de conversación
     * - Información del operador y analista
     * - Último mensaje
     * - Contador de mensajes sin leer
     * - Total de mensajes
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @return Response con lista de conversaciones
     */
    @GET("secomsa/voice-messages/operator/conversations")
    suspend fun getConversations(
        @Query("operator_code") operatorCode: String
    ): Response<ConversationsResponse>
    
    /**
     * Obtener todos los mensajes de una conversación específica
     * 
     * GET /api/v1/secomsa/voice-messages/operator/conversation
     * 
     * Retorna todos los mensajes de voz de una conversación, incluyendo:
     * - ID del mensaje
     * - URL del audio
     * - Duración
     * - Estado de lectura
     * - Metadata del archivo
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación
     * @return Response con lista de mensajes de la conversación
     */
    @GET("secomsa/voice-messages/operator/conversation")
    suspend fun getConversationMessages(
        @Query("operator_code") operatorCode: String,
        @Query("conversation_id") conversationId: String
    ): Response<ConversationMessagesResponse>
    
    /**
     * Marcar mensajes de una conversación como leídos
     * 
     * POST /api/v1/secomsa/voice-messages/operator/mark-read
     * 
     * Actualiza el estado de los mensajes no leídos a leídos.
     * El backend registra la fecha/hora en que se marcaron como leídos.
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación a marcar como leída
     * @return Response con cantidad de mensajes marcados
     */
    @POST("secomsa/voice-messages/operator/mark-read")
    suspend fun markAsRead(
        @Query("operator_code") operatorCode: String,
        @Query("conversation_id") conversationId: String
    ): Response<MarkReadResponse>
}
