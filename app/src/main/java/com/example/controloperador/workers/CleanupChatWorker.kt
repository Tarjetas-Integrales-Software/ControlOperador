package com.example.controloperador.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.controloperador.ControlOperadorApp
import com.example.controloperador.data.api.Result

/**
 * Worker para limpiar mensajes de chat más antiguos que 30 días
 * Se ejecuta una vez al día
 */
class CleanupChatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "CleanupChatWorker"
        const val WORK_NAME = "cleanup_chat_work"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting cleanup of old chat messages...")
        
        try {
            val app = applicationContext as ControlOperadorApp
            val chatRepository = app.appContainer.chatRepository
            
            val result = chatRepository.cleanOldMessages()
            
            when (result) {
                is com.example.controloperador.data.api.Result.Success -> {
                    Log.d(TAG, "Cleanup completed: ${result.data} messages deleted")
                    return Result.success()
                }
                
                is com.example.controloperador.data.api.Result.Error -> {
                    Log.e(TAG, "Cleanup error: ${result.message}")
                    return Result.retry()
                }
                
                else -> {
                    Log.w(TAG, "Unexpected result from cleanup")
                    return Result.retry()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in cleanup", e)
            return Result.retry()
        }
    }
}
