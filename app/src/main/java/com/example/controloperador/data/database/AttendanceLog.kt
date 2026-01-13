package com.example.controloperador.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad de base de datos para registros de asistencia (entrada/salida)
 * Tabla: reportes
 * 
 * @property id ID único autogenerado
 * @property operatorCode Código del operador (5 dígitos) - operator_code
 * @property nombre Nombre del operador
 * @property apellidoPaterno Apellido paterno
 * @property apellidoMaterno Apellido materno
 * @property entrada Fecha y hora de entrada (inicio de sesión)
 * @property salida Fecha y hora de salida (cierre de sesión) - nullable
 * @property tiempoOperando Total de horas operadas (calculado: salida - entrada)
 * @property enviado 0 = No enviado, 1 = Enviado al servidor
 */
@Entity(tableName = "reportes")
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val operatorCode: String,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    
    val entrada: Date,
    val salida: Date? = null,
    
    val tiempoOperando: Double = 0.0,
    val enviado: Int = 0  // 0 = no enviado, 1 = enviado
) {
    /**
     * Obtiene el nombre completo del operador
     */
    fun getFullName(): String {
        return "$nombre $apellidoPaterno $apellidoMaterno".trim()
    }
    
    /**
     * Calcula las horas operadas si hay salida
     */
    fun calculateHours(): Double {
        return if (salida != null) {
            val diffInMillis = salida.time - entrada.time
            diffInMillis / (1000.0 * 60 * 60) // Convertir a horas
        } else {
            0.0
        }
    }
    
    /**
     * Verifica si el registro está abierto (sin salida)
     */
    fun isOpen(): Boolean = salida == null
    
    /**
     * Verifica si fue enviado al servidor
     */
    fun isEnviado(): Boolean = enviado == 1
    
    /**
     * Formatea el tiempo operado como "Xh Ym"
     */
    fun getFormattedDuration(): String {
        val hours = tiempoOperando.toInt()
        val minutes = ((tiempoOperando - hours) * 60).toInt()
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
