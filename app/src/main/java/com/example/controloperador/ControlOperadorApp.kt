package com.example.controloperador

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.controloperador.data.AppContainer
import com.example.controloperador.workers.ChatSyncWorker
import com.example.controloperador.workers.CleanupChatWorker
import com.example.controloperador.workers.UpdateCheckWorker
import java.util.concurrent.TimeUnit

/**
 * Clase Application personalizada para inicializar dependencias globales
 */
class ControlOperadorApp : Application() {
    
    // Contenedor de dependencias accesible desde toda la aplicación
    lateinit var appContainer: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar contenedor de dependencias
        appContainer = AppContainer(this)
        
        // Programar workers de chat
        scheduleChatSync()
        scheduleCleanupWork()
        
        // Programar verificación de actualizaciones
        scheduleUpdateCheck()
    }
    
    /**
     * Programa la sincronización de chat cada 30 segundos
     * Solo se ejecuta cuando hay conexión a internet
     */
    private fun scheduleChatSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val chatSyncRequest = PeriodicWorkRequestBuilder<ChatSyncWorker>(
            30, TimeUnit.SECONDS // Cada 30 segundos (polling automático)
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ChatSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Mantener el existente si ya está programado
            chatSyncRequest
        )
    }
    
    /**
     * Programa la limpieza de mensajes antiguos una vez al día
     */
    private fun scheduleCleanupWork() {
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupChatWorker>(
            1, TimeUnit.DAYS // Una vez al día
        )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CleanupChatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
    
    /**
     * Programa la verificación de actualizaciones cada 10 minutos
     * Solo se ejecuta cuando hay conexión a internet
     */
    private fun scheduleUpdateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            10, TimeUnit.MINUTES // Cada 10 minutos
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }
}

