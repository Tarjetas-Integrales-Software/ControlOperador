package com.example.controloperador.data

import android.content.Context
import com.example.controloperador.data.api.ApiService
import com.example.controloperador.data.api.ChatApiService
import com.example.controloperador.data.api.RetrofitClient
import com.example.controloperador.data.database.AppDatabase
import com.example.controloperador.data.database.AttendanceRepository
import com.example.controloperador.data.database.chat.ChatRepository

/**
 * Contenedor de dependencias de la aplicación
 * Proporciona instancias singleton de servicios y repositorios
 */
class AppContainer(context: Context) {
    
    // Instancia de la base de datos
    private val database = AppDatabase.getDatabase(context)
    
    // DAO de asistencia
    private val attendanceLogDao = database.attendanceLogDao()
    
    // DAOs de chat
    private val conversationDao = database.conversationDao()
    private val chatMessageDao = database.chatMessageDao()
    
    // Servicios API
    private val apiService: ApiService = RetrofitClient.apiService
    private val chatApiService: ChatApiService = RetrofitClient.chatApiService
    
    // Repositorio de asistencia
    val attendanceRepository: AttendanceRepository = AttendanceRepository(
        attendanceLogDao = attendanceLogDao,
        apiService = apiService
    )
    
    // Repositorio de chat
    val chatRepository: ChatRepository = ChatRepository(
        conversationDao = conversationDao,
        chatMessageDao = chatMessageDao,
        chatApiService = chatApiService
    )
}
