package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.chat.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Servicio API para chat entre operadores y analistas
 */
interface ChatApiService {
    
    /**
     * Envía un mensaje del operador a los analistas
     * POST /api/chat/send
     */
    @POST("secomsa/chat/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>
    
    /**
     * Obtiene los mensajes del día actual para un operador
     * GET /api/chat/messages/today?operator_code=12345&last_id=abc123
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param lastId ID del último mensaje recibido (para paginación incremental)
     */
    @GET("secomsa/chat/messages/today")
    suspend fun getTodayMessages(
        @Query("operator_code") operatorCode: String,
        @Query("last_id") lastId: String? = null
    ): Response<TodayMessagesResponse>
    
    /**
     * Marca mensajes como leídos por el operador
     * POST /api/chat/mark-read
     */
    @POST("secomsa/chat/mark-read")
    suspend fun markMessagesAsRead(
        @Body request: MarkAsReadRequest
    ): Response<MarkAsReadResponse>
    
    /**
     * Obtiene las respuestas predefinidas disponibles
     * GET /api/chat/predefined-responses
     */
    @GET("secomsa/chat/predefined-responses")
    suspend fun getPredefinedResponses(): Response<PredefinedResponsesResponse>
}
