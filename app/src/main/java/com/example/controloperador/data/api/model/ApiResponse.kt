package com.example.controloperador.data.api.model

/**
 * Respuesta genérica de la API
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>>? = null
)

/**
 * Respuesta de login exitoso
 */
data class LoginResponse(
    val operator: OperatorData,
    val session: SessionData
)

/**
 * Datos del operador
 */
data class OperatorData(
    val id: Long,
    val operator_code: String,
    val name: String,
    val nombre: String? = null,
    val apellido_paterno: String? = null,
    val apellido_materno: String? = null,
    val corredor: CorredorData? = null,
    val last_login: String?
)

/**
 * Datos del corredor (ruta)
 */
data class CorredorData(
    val id: Int,
    val nombre: String,
    val transportista: String
)

/**
 * Datos de sesión
 */
data class SessionData(
    val expires_in: Int // Duración en segundos (8 horas = 28800)
)

/**
 * Request de login
 */
data class LoginRequest(
    val operator_code: String
)

/**
 * Respuesta de verificación
 */
data class VerifyResponse(
    val exists: Boolean,
    val is_active: Boolean
)

/**
 * Respuesta de mensajes predeterminados
 */
data class PredefinedMessagesResponse(
    val operator: OperatorInfoData,
    val text_messages: List<TextMessage>,
    val voice_messages: List<VoiceMessage>,
    val total_text_messages: Int,
    val total_voice_messages: Int
)

/**
 * Información del operador en mensajes predeterminados
 */
data class OperatorInfoData(
    val id: Long,
    val operator_code: String,
    val corredor_id: String,
    val corredor_nombre: String
)

/**
 * Mensaje de texto predeterminado
 */
data class TextMessage(
    val id: String,
    val nombre: String,
    val mensaje: String,
    val descripcion: String,
    val created_at: String,
    val updated_at: String
)

/**
 * Mensaje de voz predeterminado
 */
data class VoiceMessage(
    val id: String,
    val nombre: String,
    val archivo_url: String,
    val descripcion: String,
    val duracion: Int? = null,
    val created_at: String,
    val updated_at: String
)

/**
 * Mensaje predeterminado (legacy - para compatibilidad)
 * @deprecated Usar TextMessage o VoiceMessage según corresponda
 */
@Deprecated("Use TextMessage or VoiceMessage instead")
data class PredefinedMessage(
    val id: Int,
    val title: String,
    val message: String,
    val category: String? = null,
    val order: Int? = null
)

/**
 * Request para enviar reportes al servidor
 */
data class ReportesRequest(
    val reportes: List<ReporteData>
)

/**
 * Datos de un reporte individual
 */
data class ReporteData(
    val id: Long,  // ID local del reporte
    val operator_code: String,
    val nombre: String,
    val apellido_paterno: String,
    val apellido_materno: String,
    val entrada: String,  // Formato ISO 8601: "2024-01-15T08:30:00Z"
    val salida: String?,  // Formato ISO 8601: "2024-01-15T17:45:00Z"
    val tiempo_operando: Double  // Horas trabajadas
)

/**
 * Respuesta del servidor al enviar reportes
 */
data class ReportesResponse(
    val processed: Int,  // Cantidad de reportes procesados exitosamente
    val failed: Int,  // Cantidad de reportes fallidos
    val errors: List<ReporteError>? = null  // Detalles de errores (si hay)
)

/**
 * Error individual en un reporte
 */
data class ReporteError(
    val id: Long,  // ID local del reporte que falló
    val message: String  // Mensaje de error
)

/**
 * Respuesta de mensajes de voz del día
 */
data class VoiceMessagesResponse(
    val voice_messages: List<VoiceMessageData>,
    val count: Int,
    val fecha_consulta: String
)

/**
 * Mensaje de voz del día (desde backend)
 */
data class VoiceMessageData(
    val id: Long,
    val titulo: String,
    val descripcion: String?,
    val archivo_url: String,
    val duracion: Int?,  // Duración en segundos
    val fecha: String,  // Formato: "2025-11-13"
    val hora: String,   // Formato: "08:30:00"
    val created_at: String,
    val updated_at: String
)
