package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaz del servicio API REST
 * Define los endpoints disponibles para comunicación con el backend Laravel
 * Todas las rutas usan el prefijo 'secomsa/'
 */
interface ApiService {
    
    /**
     * Login de operador con clave numérica
     * POST /api/v1/secomsa/auth/login
     */
    @POST("secomsa/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>
    
    /**
     * Verificar si una clave de operador existe y está activa
     * POST /api/v1/secomsa/auth/verify
     */
    @POST("secomsa/auth/verify")
    suspend fun verify(@Body request: LoginRequest): Response<ApiResponse<VerifyResponse>>
    
    /**
     * Cerrar sesión
     * POST /api/v1/secomsa/auth/logout
     */
    @POST("secomsa/auth/logout")
    suspend fun logout(@Body request: LoginRequest): Response<ApiResponse<Unit>>
    
    /**
     * Health check - Verificar que la API está funcionando
     * GET /api/v1/secomsa/health
     */
    @GET("secomsa/health")
    suspend fun healthCheck(): Response<Map<String, Any>>
    
    /**
     * Obtener mensajes predeterminados para un operador
     * POST /api/v1/secomsa/messages/predefined
     */
    @POST("secomsa/messages/predefined")
    suspend fun getPredefinedMessages(@Body request: LoginRequest): Response<ApiResponse<PredefinedMessagesResponse>>
    
    /**
     * Enviar reportes de entrada/salida al servidor
     * POST /api/v1/secomsa/reportes
     */
    @POST("secomsa/reportes")
    suspend fun sendReportes(@Body request: ReportesRequest): Response<ApiResponse<ReportesResponse>>
    
    /**
     * ============================================
     * CHAT UNIFICADO - Mensajes de Texto y Voz
     * ============================================
     */
    
    /**
     * Obtener todos los mensajes del operador (texto y voz unificados)
     * GET /api/v1/secomsa/chat/operador/messages?operator_code=12345
     */
    @GET("secomsa/chat/operador/messages")
    suspend fun getUnifiedMessages(
        @Query("operator_code") operatorCode: String
    ): Response<ApiResponse<OperatorMessagesResponse>>
    
    /**
     * Obtener solo mensajes sin leer
     * GET /api/v1/secomsa/chat/operador/unread?operator_code=12345
     */
    @GET("secomsa/chat/operador/unread")
    suspend fun getUnreadMessages(
        @Query("operator_code") operatorCode: String
    ): Response<ApiResponse<UnreadMessagesResponse>>
    
    /**
     * Marcar mensajes como leídos
     * POST /api/v1/secomsa/chat/operador/mark-read
     */
    @POST("secomsa/chat/operador/mark-read")
    suspend fun markMessagesAsRead(@Body request: OperatorMarkReadRequest): Response<ApiResponse<OperatorMarkReadResponse>>
    
    /**
     * Enviar respuesta del operador (mensaje de texto)
     * POST /api/v1/secomsa/chat/operador/send
     */
    @POST("secomsa/chat/operador/send")
    suspend fun sendOperatorResponse(@Body request: SendResponseRequest): Response<ApiResponse<UnifiedMessage>>
    
    /**
     * ============================================
     * LEGACY - Mantener compatibilidad
     * ============================================
     */
    
    /**
     * Obtener mensajes de voz del día actual (LEGACY)
     * GET /api/v1/secomsa/voice-messages
     */
    @GET("secomsa/voice-messages")
    suspend fun getVoiceMessages(
        @Query("operator_code") operatorCode: String,
        @Query("last_id") lastId: Int = 0
    ): Response<ApiResponse<VoiceMessagesResponse>>
}
