package com.example.controloperador.data

/**
 * Modelo de datos para información del operador
 * Contiene los datos que se mostrarán en el navigation drawer
 */
data class OperatorInfo(
    val operatorCode: String,
    val route: String,
    val unitNumber: String
)

/**
 * Repositorio para obtener información del operador
 * TODO: Implementar llamadas a API REST cuando esté disponible
 */
class OperatorRepository {
    
    /**
     * Obtiene la información del operador desde el servidor
     * @param operatorCode Código del operador autenticado
     * @return Información del operador
     * 
     * TODO: Reemplazar con llamada real a API
     * Ejemplo de implementación con Retrofit:
     * suspend fun getOperatorInfo(operatorCode: String): Result<OperatorInfo> {
     *     return try {
     *         val response = apiService.getOperatorInfo(operatorCode)
     *         Result.success(response)
     *     } catch (e: Exception) {
     *         Result.failure(e)
     *     }
     * }
     */
    fun getOperatorInfo(operatorCode: String): OperatorInfo {
        // Datos mock/fijos para desarrollo
        // En producción esto vendría de una API REST
        return OperatorInfo(
            operatorCode = operatorCode,
            route = "C30-C75",
            unitNumber = "00001"
        )
    }
    
    /**
     * Actualiza la información del operador desde el servidor
     * @param operatorCode Código del operador
     * @return Nueva información actualizada
     */
    suspend fun refreshOperatorInfo(operatorCode: String): OperatorInfo {
        // TODO: Implementar con llamada a API
        // Por ahora retorna datos fijos
        return getOperatorInfo(operatorCode)
    }
}
