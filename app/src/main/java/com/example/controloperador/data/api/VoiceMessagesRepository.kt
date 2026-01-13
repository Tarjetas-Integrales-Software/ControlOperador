package com.example.controloperador.data.api

import android.util.Log
import com.example.controloperador.data.api.model.VoiceMessageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository para gestionar mensajes de voz desde el backend
 * Los mensajes solo se obtienen, no se envían (solo lectura)
 */
class VoiceMessagesRepository(
    private val apiService: ApiService
) {
    
    private val TAG = "VoiceMessagesRepo"
    
    /**
     * Resultado de operación de red
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
        object NetworkError : Result<Nothing>()
        object Timeout : Result<Nothing>()
    }
    
    /**
     * Obtiene los mensajes de voz del día actual desde el backend
     * 
     * @param operatorCode Código de 5 dígitos del operador
     * @param lastId ID del último mensaje recibido (para sincronización incremental)
     * @return Result con lista de mensajes o error
     */
    suspend fun getTodayVoiceMessages(
        operatorCode: String,
        lastId: Int = 0
    ): Result<List<VoiceMessageData>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo mensajes de voz para operador: $operatorCode, lastId: $lastId")
            
            val response = apiService.getVoiceMessages(operatorCode, lastId)
            
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        val messages = body.data.voice_messages
                        Log.d(TAG, "✅ ${messages.size} mensajes de voz obtenidos")
                        Result.Success(messages)
                    } else {
                        val errorMsg = body?.message ?: "Error desconocido al obtener mensajes"
                        Log.e(TAG, "❌ Error en respuesta: $errorMsg")
                        Result.Error(errorMsg)
                    }
                }
                
                response.code() == 401 -> {
                    Log.e(TAG, "❌ Operador no autorizado")
                    Result.Error("Operador no encontrado o inactivo", 401)
                }
                
                response.code() == 422 -> {
                    Log.e(TAG, "❌ Datos inválidos")
                    Result.Error("Código de operador inválido", 422)
                }
                
                response.code() in 500..599 -> {
                    Log.e(TAG, "❌ Error del servidor: ${response.code()}")
                    Result.Error("Error en el servidor", response.code())
                }
                
                else -> {
                    Log.e(TAG, "❌ Error HTTP: ${response.code()}")
                    Result.Error("Error al obtener mensajes de voz", response.code())
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error de red: ${e.message}")
            Result.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado: ${e.message}")
            Result.Error(e.message ?: "Error desconocido")
        }
    }
    
    /**
     * Verifica si hay nuevos mensajes de voz
     * 
     * @param operatorCode Código del operador
     * @param lastId ID del último mensaje conocido
     * @return true si hay nuevos mensajes
     */
    suspend fun hasNewMessages(
        operatorCode: String,
        lastId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val result = getTodayVoiceMessages(operatorCode, lastId)
        return@withContext when (result) {
            is Result.Success -> result.data.isNotEmpty()
            else -> false
        }
    }
}
