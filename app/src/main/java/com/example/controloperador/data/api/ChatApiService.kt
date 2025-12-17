package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.chat.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
     * POST /api/chat/messages/today (cambiado a POST por problemas con query params en producción)
     * 
     * @param request Contiene operator_code y last_id
     */
    @POST("secomsa/chat/messages/today")
    suspend fun getTodayMessages(
        @Body request: GetMessagesRequest
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
