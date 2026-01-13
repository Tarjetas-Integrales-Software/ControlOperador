package com.example.controloperador.data.api

import android.util.Log
import com.example.controloperador.data.api.model.LoginRequest
import com.example.controloperador.data.api.model.TextMessage
import com.example.controloperador.data.api.model.VoiceMessage
import com.example.controloperador.data.api.model.PredefinedMessagesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repositorio para manejo de mensajes y comunicación con operadores
 */
class MessagesRepository {
    
    companion object {
        private const val TAG = "MessagesRepository"
    }
    
    /**
     * Obtiene los mensajes predeterminados desde el backend
     * @param operatorCode Código del operador (5 dígitos)
     * @return Result con la respuesta completa de mensajes o error
     */
    suspend fun getPredefinedMessages(operatorCode: String): Result<PredefinedMessagesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching predefined messages for operator: $operatorCode")
                
                val response = RetrofitClient.apiService.getPredefinedMessages(
                    LoginRequest(operatorCode)
                )
                
                Log.d(TAG, "Response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        Log.d(TAG, "Predefined messages loaded:")
                        Log.d(TAG, "  - Text messages: ${body.data.total_text_messages}")
                        Log.d(TAG, "  - Voice messages: ${body.data.total_voice_messages}")
                        Log.d(TAG, "  - Corredor: ${body.data.operator.corredor_nombre}")
                        Result.Success(body.data)
                    } else {
                        val errorMessage = body?.message ?: "Error desconocido"
                        Log.e(TAG, "API returned error: $errorMessage")
                        Result.Error(errorMessage)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "No autorizado"
                        404 -> "No se encontraron mensajes predeterminados"
                        500 -> "Error del servidor"
                        else -> "Error al cargar mensajes (${response.code()})"
                    }
                    Log.e(TAG, "HTTP error: ${response.code()} - $errorMessage")
                    Result.Error(errorMessage, response.code())
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout error", e)
                Result.Timeout
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                Result.NetworkError
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception", e)
                Result.Error("Error de conexión: ${e.message()}", e.code())
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                Result.Error("Error inesperado: ${e.message}")
            }
        }
    }
    
    /**
     * Mensajes de texto predeterminados locales (fallback)
     * Se usan si no hay conexión al backend
     */
    fun getLocalTextMessages(): List<TextMessage> {
        return listOf(
            TextMessage(
                id = "1",
                nombre = "Falla Mecánica",
                mensaje = "Unidad con falla mecánica, requiero asistencia inmediata",
                descripcion = "Mensaje para reportar fallas mecánicas en la unidad",
                created_at = "",
                updated_at = ""
            ),
            TextMessage(
                id = "2",
                nombre = "Neumático Ponchado",
                mensaje = "Llanta ponchada, en proceso de cambio",
                descripcion = "Notificación de neumático ponchado",
                created_at = "",
                updated_at = ""
            ),
            TextMessage(
                id = "3",
                nombre = "Siniestro",
                mensaje = "Reporto siniestro, requiero apoyo urgente",
                descripcion = "Alerta de accidente o siniestro",
                created_at = "",
                updated_at = ""
            ),
            TextMessage(
                id = "4",
                nombre = "Tráfico Detenido",
                mensaje = "Tráfico completamente detenido en mi ruta. Retrasaré el itinerario",
                descripcion = "Notificación de retraso por tráfico",
                created_at = "",
                updated_at = ""
            ),
            TextMessage(
                id = "5",
                nombre = "Desviación por Obras",
                mensaje = "Tomé desviación debido a obras en el camino",
                descripcion = "Notificación de cambio de ruta",
                created_at = "",
                updated_at = ""
            ),
            TextMessage(
                id = "6",
                nombre = "Falla en Equipo de Prepago",
                mensaje = "El equipo de prepago no está funcionando correctamente",
                descripcion = "Reporte de falla técnica en equipo",
                created_at = "",
                updated_at = ""
            )
        )
    }
}

/**
 * Clase sellada para resultados de operaciones del repositorio
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object NetworkError : Result<Nothing>()
    object Timeout : Result<Nothing>()
}
