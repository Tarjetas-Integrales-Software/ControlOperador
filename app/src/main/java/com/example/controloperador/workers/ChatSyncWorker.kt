package com.example.controloperador.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.controloperador.ControlOperadorApp
import com.example.controloperador.data.api.Result
import com.example.controloperador.ui.login.SessionManager

/**
 * Worker para sincronizar mensajes de chat cada 15 segundos
 * Solo se ejecuta cuando la app está en foreground y hay conexión a internet
 */
class ChatSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "ChatSyncWorker"
        const val WORK_NAME = "chat_sync_work"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting chat sync...")
        
        try {
            // Obtener dependencias
            val app = applicationContext as ControlOperadorApp
            val chatRepository = app.appContainer.chatRepository
            val sessionManager = SessionManager(applicationContext)
            
            // Verificar si hay sesión activa
            val operatorCode = sessionManager.getOperatorCode()
            if (operatorCode.isNullOrEmpty()) {
                Log.d(TAG, "No active session, skipping sync")
                return Result.success()
            }
            
            // Obtener o crear conversación
            val conversation = chatRepository.getOrCreateConversation(operatorCode)
            
            // 1. Reintentar mensajes pendientes
            val retriedCount = chatRepository.retryPendingMessages(conversation.id, operatorCode)
            if (retriedCount > 0) {
                Log.d(TAG, "Retried $retriedCount pending messages")
            }
            
            // 2. Obtener mensajes nuevos del servidor
            val fetchResult = chatRepository.fetchNewMessages(conversation.id, operatorCode)
            
            when (fetchResult) {
                is com.example.controloperador.data.api.Result.Success -> {
                    Log.d(TAG, "Sync completed: ${fetchResult.data} new messages")
                    return Result.success()
                }
                
                is com.example.controloperador.data.api.Result.Error -> {
                    Log.e(TAG, "Sync error: ${fetchResult.message}")
                    return Result.retry() // Reintentar en el próximo ciclo
                }
                
                is com.example.controloperador.data.api.Result.NetworkError -> {
                    Log.w(TAG, "Network error, will retry")
                    return Result.retry()
                }
                
                is com.example.controloperador.data.api.Result.Timeout -> {
                    Log.w(TAG, "Timeout, will retry")
                    return Result.retry()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in chat sync", e)
            return Result.retry()
        }
    }
}
