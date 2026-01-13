package com.example.controloperador.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.controloperador.data.database.chat.ChatMessage
import com.example.controloperador.data.database.chat.ChatMessageDao
import com.example.controloperador.data.database.chat.Conversation
import com.example.controloperador.data.database.chat.ConversationDao

/**
 * Base de datos Room para la aplicación ControlOperador
 * 
 * @property attendanceLogDao DAO para acceder a registros de asistencia
 * @property conversationDao DAO para acceder a conversaciones de chat
 * @property chatMessageDao DAO para acceder a mensajes de chat
 */
@Database(
    entities = [
        AttendanceLog::class,
        Conversation::class,
        ChatMessage::class
    ],
    version = 2, // Incrementado por nuevas tablas de chat
    exportSchema = false
)
@TypeConverters(Converters::class) // Un solo TypeConverter consolidado
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    
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
