package com.example.controloperador.data.api

import android.util.Log
import com.example.controloperador.data.api.model.LoginRequest
import com.example.controloperador.data.api.model.LoginResponse
import com.example.controloperador.data.api.model.VerifyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repositorio de autenticación
 * Maneja las llamadas a la API y el manejo de errores
 */
class AuthRepository {
    
    private val apiService = RetrofitClient.apiService
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    /**
     * Resultado genérico de operaciones de la API
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
        object NetworkError : Result<Nothing>()
        object Timeout : Result<Nothing>()
    }
    
    /**
     * Autenticar operador con clave numérica
     * 
     * @param operatorCode Clave de 5 dígitos
     * @return Result con LoginResponse o error
     */
    suspend fun login(operatorCode: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(operatorCode)
            val response = apiService.login(request)
            
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                Log.d(TAG, "Body success: ${body?.success}")
                Log.d(TAG, "Body data: ${body?.data}")
                
                if (body != null && body.success && body.data != null) {
                    Log.d(TAG, "Login successful for operator: ${body.data.operator.operator_code}")
                    Log.d(TAG, "Operator name: ${body.data.operator.name}")
                    Result.Success(body.data)
                } else {
                    val errorMsg = body?.message ?: "Error desconocido"
                    Log.e(TAG, "Login failed: $errorMsg")
                    Result.Error(errorMsg)
                }
            } else {
                Log.e(TAG, "Response not successful: ${response.code()}")
                when (response.code()) {
                    401 -> Result.Error("Clave de operador incorrecta o inactiva", 401)
                    422 -> Result.Error("La clave debe tener 5 dígitos numéricos", 422)
                    429 -> Result.Error("Demasiados intentos. Intente más tarde", 429)
                    500 -> Result.Error("Error del servidor. Intente más tarde", 500)
                    else -> Result.Error("Error: ${response.code()}", response.code())
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout exception", e)
            Result.Timeout
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.NetworkError
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}", e)
            Result.Error("Error HTTP: ${e.code()}", e.code())
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Result.Error("Error inesperado: ${e.message ?: "Desconocido"}")
        }
    }
    
    /**
     * Verificar si una clave de operador existe y está activa
     * 
     * @param operatorCode Clave de 5 dígitos
     * @return Result con VerifyResponse o error
     */
    suspend fun verify(operatorCode: String): Result<VerifyResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(operatorCode)
            val response = apiService.verify(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.Success(body.data)
                } else {
                    Result.Error(body?.message ?: "Error desconocido")
                }
            } else {
                Result.Error("Error: ${response.code()}", response.code())
            }
        } catch (e: SocketTimeoutException) {
            Result.Timeout
        } catch (e: IOException) {
            Result.NetworkError
        } catch (e: Exception) {
            Result.Error("Error: ${e.message ?: "Desconocido"}")
        }
    }
    
    /**
     * Cerrar sesión del operador
     * 
     * @param operatorCode Clave del operador
     * @return Result indicando éxito o error
     */
    suspend fun logout(operatorCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(operatorCode)
            val response = apiService.logout(request)
            
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Error al cerrar sesión", response.code())
            }
        } catch (e: Exception) {
            // Logout siempre exitoso localmente aunque falle en servidor
            Result.Success(Unit)
        }
    }
    
    /**
     * Verificar conectividad con el servidor
     * 
     * @return true si el servidor está disponible
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
