package com.example.controloperador.data.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Convertidores de tipo para Room Database
 * Convierte tipos complejos a tipos primitivos que Room puede almacenar
 */
class Converters {
    
    /**
     * Convierte timestamp (Long) a Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Convierte Date a timestamp (Long)
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
