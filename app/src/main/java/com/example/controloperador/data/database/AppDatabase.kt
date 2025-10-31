package com.example.controloperador.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room para la aplicación ControlOperador
 * 
 * @property attendanceLogDao DAO para acceder a registros de asistencia
 */
@Database(
    entities = [AttendanceLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun attendanceLogDao(): AttendanceLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Obtiene la instancia singleton de la base de datos
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controloperador_database"
                )
                    .fallbackToDestructiveMigration() // En producción, usar migraciones
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
