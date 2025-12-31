package com.example.controloperador.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

/**
 * DAO (Data Access Object) para operaciones de base de datos de AttendanceLog
 */
@Dao
interface AttendanceLogDao {
    
    /**
     * Inserta un nuevo registro de asistencia
     * @return ID del registro insertado
     */
    @Insert
    suspend fun insert(attendanceLog: AttendanceLog): Long
    
    /**
     * Actualiza un registro existente
     */
    @Update
    suspend fun update(attendanceLog: AttendanceLog)
    
    /**
     * Elimina un registro
     */
    @Delete
    suspend fun delete(attendanceLog: AttendanceLog)
    
    /**
     * Obtiene todos los registros ordenados por fecha de entrada descendente
     */
    @Query("SELECT * FROM reportes ORDER BY entrada DESC")
    fun getAllLogs(): LiveData<List<AttendanceLog>>
    
    /**
     * Obtiene todos los registros de forma síncrona (para operaciones no UI)
     */
    @Query("SELECT * FROM reportes ORDER BY entrada DESC")
    suspend fun getAllLogsSync(): List<AttendanceLog>
    
    /**
     * Obtiene el último registro abierto (sin salida) de un operador
     */
    @Query("SELECT * FROM reportes WHERE operatorCode = :operatorCode AND salida IS NULL ORDER BY entrada DESC LIMIT 1")
    suspend fun getOpenLog(operatorCode: String): AttendanceLog?
    
    /**
     * Obtiene registros por operador
     */
    @Query("SELECT * FROM reportes WHERE operatorCode = :operatorCode ORDER BY entrada DESC")
    fun getLogsByOperator(operatorCode: String): LiveData<List<AttendanceLog>>
    
    /**
     * Obtiene registros de la última semana
     */
    @Query("SELECT * FROM reportes WHERE entrada >= :startDate ORDER BY entrada DESC")
    fun getLogsLastWeek(startDate: Date): LiveData<List<AttendanceLog>>
    
    /**
     * Obtiene registros no enviados al servidor (enviado = 0) y con salida registrada
     */
    @Query("SELECT * FROM reportes WHERE enviado = 0 AND salida IS NOT NULL ORDER BY entrada ASC")
    suspend fun getUnsyncedLogs(): List<AttendanceLog>
    
    /**
     * Marca un registro como enviado (enviado = 1)
     */
    @Query("UPDATE reportes SET enviado = 1 WHERE id = :logId")
    suspend fun markAsSynced(logId: Long)
    
    /**
     * Obtiene el total de horas operadas en un rango de fechas
     */
    @Query("SELECT SUM(tiempoOperando) FROM reportes WHERE entrada BETWEEN :startDate AND :endDate AND salida IS NOT NULL")
    suspend fun getTotalHours(startDate: Date, endDate: Date): Double?
    
    /**
     * Elimina registros antiguos (más de 90 días) que ya fueron enviados
     */
    @Query("DELETE FROM reportes WHERE entrada < :cutoffDate AND enviado = 1")
    suspend fun deleteOldSyncedLogs(cutoffDate: Date)
    
    /**
     * Obtiene registros agrupados por día para estadísticas
     * Incluye tanto registros cerrados (con salida) como abiertos (calculando tiempo hasta ahora)
     */
    @Query("""
        SELECT DATE(entrada / 1000, 'unixepoch', 'localtime') as date,
               SUM(CASE 
                   WHEN salida IS NOT NULL THEN tiempoOperando
                   ELSE CAST((strftime('%s', 'now') * 1000 - entrada) AS REAL) / 3600000.0
               END) as totalHours
        FROM reportes 
        WHERE entrada >= :startDateMillis 
          AND (:operatorCode IS NULL OR operatorCode = :operatorCode)
        GROUP BY DATE(entrada / 1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
    """)
    suspend fun getDailyStats(startDateMillis: Long, operatorCode: String?): List<DailyStats>
}

/**
 * Clase de datos para estadísticas diarias
 */
data class DailyStats(
    val date: String,
    val totalHours: Double
)
