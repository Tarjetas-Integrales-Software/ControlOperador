package com.example.controloperador.data.repository

import android.util.Log
import com.example.controloperador.data.api.RetrofitClient
import com.example.controloperador.data.api.VoiceMessageApiService
import com.example.controloperador.data.api.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository para gestionar mensajes de voz desde el backend
 * Implementa los 3 endpoints principales según ANDROID_VOICE_MESSAGES_API.md
 * 
 * Funcionalidades:
 * - Obtener lista de conversaciones con mensajes de voz
 * - Obtener mensajes de una conversación específica
 * - Marcar mensajes como leídos
 */
class VoiceMessagesRepositoryV2(
    private val api: VoiceMessageApiService = RetrofitClient.voiceMessageApiService
) {
    
    private val TAG = "VoiceMessagesRepoV2"
    
    companion object {
        /**
         * WORKAROUND: Corrige IPs incorrectas en URLs de audio del backend
         * El backend tiene APP_URL configurado con IP vieja en .env
         * TODO: Backend debe corregir APP_URL=http://172.16.22.78:8000 en su .env
         */
        private fun fixAudioUrl(url: String, correctBaseUrl: String): String {
            return if (url.contains("172.16.24.15") || url.contains("172.16.20.10")) {
                // Reemplazar IP vieja con la correcta
                url.replace("http://172.16.24.15:8000", correctBaseUrl)
                   .replace("http://172.16.20.10:8000", correctBaseUrl)
            } else {
                url
            }
        }
    }
    
    /**
     * Resultado de operaciones de red
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
        object NetworkError : Result<Nothing>()
        object Timeout : Result<Nothing>()
    }
    
    /**
     * Obtiene todas las conversaciones del operador
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @return Result con lista de conversaciones o error
     */
    suspend fun getConversations(operatorCode: String): Result<List<VoiceConversation>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Obteniendo conversaciones de mensajes de voz para operador: $operatorCode")
                
                val response = api.getConversations(operatorCode)
                
                when {
                    response.isSuccessful -> {
                        val body = response.body()
                        if (body?.response == true && body.data != null) {
                            // WORKAROUND: Obtener base URL correcta del endpoint actual
                            val correctBaseUrl = response.raw().request.url.let { url ->
                                "${url.scheme}://${url.host}:${url.port}"
                            }
                            
                            // Backend devuelve UN objeto, lo convertimos a lista con una conversación
                            val conversation = VoiceConversation(
                                conversationId = body.data.conversationId,
                                operatorCode = body.data.operatorCode,
                                operatorName = body.data.operatorName,
                                operatorApellidoPaterno = body.data.operatorApellidoPaterno,
                                operatorApellidoMaterno = body.data.operatorApellidoMaterno,
                                analystName = "Analista", // No viene en la respuesta
                                lastMessageDate = body.data.messages.firstOrNull()?.createdAt ?: "",
                                lastAudioDuration = body.data.messages.firstOrNull()?.duration ?: 0,
                                unreadCount = body.data.unreadCount,
                                totalMessages = body.data.totalMessages
                            )
                            Log.d(TAG, "✅ Conversación obtenida con ${body.data.totalMessages} mensajes (base: $correctBaseUrl)")
                            Result.Success(listOf(conversation))
                        } else {
                            val errorMsg = body?.message ?: "Error desconocido"
                            Log.e(TAG, "❌ Error en respuesta: $errorMsg")
                            Result.Error(errorMsg)
                        }
                    }
                    
                    response.code() == 401 -> {
                        Log.e(TAG, "❌ No autorizado - Token inválido")
                        Result.Error("Sesión expirada o token inválido", 401)
                    }
                    
                    response.code() == 404 -> {
                        Log.e(TAG, "❌ No se encontraron conversaciones")
                        Result.Success(emptyList()) // Sin conversaciones es válido
                    }
                    
                    response.code() in 500..599 -> {
                        Log.e(TAG, "❌ Error del servidor: ${response.code()}")
                        Result.Error("Error en el servidor", response.code())
                    }
                    
                    else -> {
                        Log.e(TAG, "❌ Error HTTP: ${response.code()}")
                        Result.Error("Error al obtener conversaciones", response.code())
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "❌ Error de red: ${e.message}")
                Result.NetworkError
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado: ${e.message}", e)
                Result.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Obtiene todos los mensajes de una conversación específica
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación
     * @return Result con lista de mensajes o error
     */
    suspend fun getConversationMessages(
        operatorCode: String,
        conversationId: String
    ): Result<List<VoiceMessageDetail>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📥 Obteniendo mensajes de conversación: $conversationId para operador: $operatorCode")
                
                // Según la documentación, los mensajes vienen en la primera llamada
                // Este endpoint NO existe en el backend, usamos getConversations
                val response = api.getConversations(operatorCode)
                
                when {
                    response.isSuccessful -> {
                        val body = response.body()
                        if (body?.response == true && body.data != null) {
                            // WORKAROUND: Corregir URLs de audio con IP incorrecta
                            val correctBaseUrl = response.raw().request.url.let { url ->
                                "${url.scheme}://${url.host}:${url.port}"
                            }
                            
                            val messages = body.data.messages.map { message ->
                                message.copy(
                                    audioUrl = fixAudioUrl(message.audioUrl, correctBaseUrl)
                                )
                            }
                            Log.d(TAG, "✅ ${messages.size} mensajes obtenidos (URLs corregidas)")
                            Result.Success(messages)
                        } else {
                            val errorMsg = body?.message ?: "Error desconocido"
                            Log.e(TAG, "❌ Error en respuesta: $errorMsg")
                            Result.Error(errorMsg)
                        }
                    }
                    
                    response.code() == 401 -> {
                        Log.e(TAG, "❌ No autorizado")
                        Result.Error("Sesión expirada o token inválido", 401)
                    }
                    
                    response.code() == 404 -> {
                        Log.e(TAG, "❌ Conversación no encontrada")
                        Result.Error("Conversación no encontrada", 404)
                    }
                    
                    response.code() in 500..599 -> {
                        Log.e(TAG, "❌ Error del servidor: ${response.code()}")
                        Result.Error("Error en el servidor", response.code())
                    }
                    
                    else -> {
                        Log.e(TAG, "❌ Error HTTP: ${response.code()}")
                        Result.Error("Error al obtener mensajes", response.code())
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "❌ Error de red: ${e.message}")
                Result.NetworkError
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado: ${e.message}", e)
                Result.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Marca los mensajes de una conversación como leídos
     * 
     * El backend actualiza automáticamente todos los mensajes no leídos
     * de la conversación especificada
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación
     * @return Result con cantidad de mensajes marcados o error
     */
    suspend fun markAsRead(
        operatorCode: String,
        conversationId: String
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "✓ Marcando mensajes como leídos: $conversationId para operador: $operatorCode")
                
                val response = api.markAsRead(operatorCode, conversationId)
                
                when {
                    response.isSuccessful -> {
                        val body = response.body()
                        if (body?.response == true && body.data != null) {
                            val markedCount = body.data.markedCount
                            Log.d(TAG, "✅ $markedCount mensajes marcados como leídos")
                            Result.Success(markedCount)
                        } else {
                            val errorMsg = body?.message ?: "Error desconocido"
                            Log.e(TAG, "❌ Error en respuesta: $errorMsg")
                            Result.Error(errorMsg)
                        }
                    }
                    
                    response.code() == 401 -> {
                        Log.e(TAG, "❌ No autorizado")
                        Result.Error("Sesión expirada o token inválido", 401)
                    }
                    
                    response.code() == 404 -> {
                        Log.e(TAG, "❌ Conversación no encontrada")
                        Result.Error("Conversación no encontrada", 404)
                    }
                    
                    response.code() in 500..599 -> {
                        Log.e(TAG, "❌ Error del servidor: ${response.code()}")
                        Result.Error("Error en el servidor", response.code())
                    }
                    
                    else -> {
                        Log.e(TAG, "❌ Error HTTP: ${response.code()}")
                        Result.Error("Error al marcar como leído", response.code())
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "❌ Error de red: ${e.message}")
                Result.NetworkError
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado: ${e.message}", e)
                Result.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Cuenta el total de mensajes sin leer del operador
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @return Total de mensajes sin leer
     */
    suspend fun getTotalUnreadCount(operatorCode: String): Int {
        return when (val result = getConversations(operatorCode)) {
            is Result.Success -> result.data.sumOf { it.unreadCount }
            else -> 0
        }
    }
}
