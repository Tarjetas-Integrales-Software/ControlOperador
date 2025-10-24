package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}
