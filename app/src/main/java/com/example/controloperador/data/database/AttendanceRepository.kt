package com.example.controloperador.data.database

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.controloperador.data.api.ApiService
import com.example.controloperador.data.api.model.ReporteData
import com.example.controloperador.data.api.model.ReportesRequest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio para manejar operaciones de asistencia (entrada/salida)
 * Actúa como intermediario entre el DAO y el ViewModel
 */
class AttendanceRepository(
    private val attendanceLogDao: AttendanceLogDao,
    private val apiService: ApiService
) {
    
    companion object {
        private const val TAG = "AttendanceRepository"
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    /**
     * Obtiene todos los registros como LiveData (se actualiza automáticamente)
     */
    val allLogs: LiveData<List<AttendanceLog>> = attendanceLogDao.getAllLogs()
    
    /**
     * Registra la entrada de un operador (inicio de sesión)
     * 
     * @param operatorCode Código del operador
     * @param nombre Nombre
     * @param apellidoPaterno Apellido paterno
     * @param apellidoMaterno Apellido materno
     * @return ID del registro creado
     */
    suspend fun registerEntry(
        operatorCode: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String
    ): Long {
        // Verificar si ya hay un registro abierto
        val openLog = attendanceLogDao.getOpenLog(operatorCode)
        if (openLog != null) {
            Log.w(TAG, "Ya existe un registro abierto para $operatorCode, cerrándolo primero")
            registerExit(operatorCode)
        }
        
        val log = AttendanceLog(
            operatorCode = operatorCode,
            nombre = nombre,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            entrada = Date(),
            salida = null,
            tiempoOperando = 0.0,
            enviado = 0  // No enviado
        )
        
        val id = attendanceLogDao.insert(log)
        Log.d(TAG, "Entrada registrada: $operatorCode a las ${log.entrada}, ID: $id")
        return id
    }
    
    /**
     * Registra la salida de un operador (cierre de sesión)
     * Busca el último registro abierto y lo actualiza con la hora de salida
     * 
     * @param operatorCode Código del operador
     * @return AttendanceLog actualizado si se registró exitosamente, null si no había registro abierto
     */
    suspend fun registerExit(operatorCode: String): AttendanceLog? {
        val openLog = attendanceLogDao.getOpenLog(operatorCode)
        
        return if (openLog != null) {
            val salidaTime = Date()
            val tiempoOperando = (salidaTime.time - openLog.entrada.time) / (1000.0 * 60 * 60)
            
            val updatedLog = openLog.copy(
                salida = salidaTime,
                tiempoOperando = tiempoOperando,
                enviado = 0  // Pendiente de enviar
            )
            
            attendanceLogDao.update(updatedLog)
            Log.d(TAG, "Salida registrada: $operatorCode a las $salidaTime, Total: ${String.format("%.2f", tiempoOperando)}h")
            updatedLog
        } else {
            Log.w(TAG, "No se encontró registro abierto para $operatorCode")
            null
        }
    }
    
    /**
     * Obtiene registros por operador
     */
    fun getLogsByOperator(operatorCode: String): LiveData<List<AttendanceLog>> {
        return attendanceLogDao.getLogsByOperator(operatorCode)
    }
    
    /**
     * Obtiene registros de los últimos 7 días
     */
    fun getLogsLastWeek(): LiveData<List<AttendanceLog>> {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }
        return attendanceLogDao.getLogsLastWeek(calendar.time)
    }
    
    /**
     * Obtiene estadísticas diarias de la última semana
     */
    suspend fun getWeeklyStats(): List<DailyStats> {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }
        return attendanceLogDao.getDailyStats(calendar.time)
    }
    
    /**
     * Obtiene registros no sincronizados para enviar al servidor
     */
    suspend fun getUnsyncedLogs(): List<AttendanceLog> {
        return attendanceLogDao.getUnsyncedLogs()
    }
    
    /**
     * Marca un registro como sincronizado después de enviarlo al servidor
     */
    suspend fun markAsSynced(logId: Long) {
        attendanceLogDao.markAsSynced(logId)
        Log.d(TAG, "Registro $logId marcado como sincronizado")
    }
    
    /**
     * Obtiene el total de horas operadas en un rango de fechas
     */
    suspend fun getTotalHours(startDate: Date, endDate: Date): Double {
        return attendanceLogDao.getTotalHours(startDate, endDate) ?: 0.0
    }
    
    /**
     * Limpia registros antiguos sincronizados (más de 90 días)
     */
    suspend fun cleanOldLogs() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -90)
        }
        attendanceLogDao.deleteOldSyncedLogs(calendar.time)
        Log.d(TAG, "Registros antiguos eliminados (anteriores a ${calendar.time})")
    }
    
    /**
     * Verifica si hay un registro abierto para un operador
     */
    suspend fun hasOpenLog(operatorCode: String): Boolean {
        return attendanceLogDao.getOpenLog(operatorCode) != null
    }
    
    /**
     * Sincroniza todos los registros no enviados con el servidor
     * Se llama cuando el usuario cierra sesión
     * 
     * @return Pair con (cantidad exitosa, cantidad fallida)
     */
    suspend fun syncUnsentReports(): Pair<Int, Int> {
        val unsentLogs = getUnsyncedLogs()
        
        if (unsentLogs.isEmpty()) {
            Log.d(TAG, "No hay reportes pendientes de enviar")
            return Pair(0, 0)
        }
        
        Log.d(TAG, "Intentando enviar ${unsentLogs.size} reportes pendientes")
        
        try {
            // Convertir AttendanceLog a ReporteData
            val reportesData = unsentLogs.map { log ->
                ReporteData(
                    id = log.id,
                    operator_code = log.operatorCode,
                    nombre = log.nombre,
                    apellido_paterno = log.apellidoPaterno,
                    apellido_materno = log.apellidoMaterno,
                    entrada = ISO_DATE_FORMAT.format(log.entrada),
                    salida = log.salida?.let { ISO_DATE_FORMAT.format(it) },
                    tiempo_operando = log.tiempoOperando
                )
            }
            
            val request = ReportesRequest(reportes = reportesData)
            val response = apiService.sendReportes(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val processed = data?.processed ?: 0
                val failed = data?.failed ?: 0
                
                // Marcar como sincronizados los que fueron exitosos
                if (processed > 0) {
                    // Si el servidor no devuelve qué IDs fallaron, asumir que los primeros fueron exitosos
                    val successfulIds = if (data?.errors.isNullOrEmpty()) {
                        unsentLogs.take(processed).map { it.id }
                    } else {
                        // Excluir los IDs que fallaron
                        val failedIds = data?.errors?.map { it.id } ?: emptyList()
                        unsentLogs.filter { it.id !in failedIds }.map { it.id }
                    }
                    
                    successfulIds.forEach { logId ->
                        markAsSynced(logId)
                    }
                    
                    Log.d(TAG, "Reportes sincronizados: $processed exitosos, $failed fallidos")
                }
                
                return Pair(processed, failed)
            } else {
                val errorMsg = response.body()?.message ?: "Error ${response.code()}"
                Log.e(TAG, "Error en respuesta del servidor: $errorMsg")
                return Pair(0, unsentLogs.size)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar reportes", e)
            return Pair(0, unsentLogs.size)
        }
    }
    
    /**
     * Intenta sincronizar un registro específico inmediatamente después de cerrar sesión
     * Se usa cuando el operador acaba de hacer logout
     * 
     * @param log Registro recién cerrado
     * @return true si se sincronizó exitosamente, false en caso contrario
     */
    suspend fun syncSingleReport(log: AttendanceLog): Boolean {
        if (log.salida == null) {
            Log.w(TAG, "No se puede sincronizar reporte sin hora de salida")
            return false
        }
        
        try {
            val reporteData = ReporteData(
                id = log.id,
                operator_code = log.operatorCode,
                nombre = log.nombre,
                apellido_paterno = log.apellidoPaterno,
                apellido_materno = log.apellidoMaterno,
                entrada = ISO_DATE_FORMAT.format(log.entrada),
                salida = ISO_DATE_FORMAT.format(log.salida!!),
                tiempo_operando = log.tiempoOperando
            )
            
            val request = ReportesRequest(reportes = listOf(reporteData))
            val response = apiService.sendReportes(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val processed = response.body()?.data?.processed ?: 0
                if (processed > 0) {
                    markAsSynced(log.id)
                    Log.d(TAG, "Reporte ${log.id} sincronizado exitosamente")
                    return true
                }
            }
            
            Log.w(TAG, "No se pudo sincronizar reporte ${log.id}")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar reporte ${log.id}", e)
            return false
        }
    }
}
