package com.example.controloperador.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.controloperador.MainActivity
import com.example.controloperador.R
import com.example.controloperador.data.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que verifica actualizaciones desde GitHub cada 10 minutos
 * Se ejecuta en background incluso cuando la app está cerrada
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UpdateCheckWorker"
        const val WORK_NAME = "update_check_work"
        
        // IDs para notificaciones
        private const val NOTIFICATION_CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
    }
    
    private val updateRepository = UpdateRepository(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Iniciando verificación de actualizaciones...")
            
            // Verificar si hay actualización disponible
            val result = updateRepository.checkForUpdates()
            
            result.onSuccess { release ->
                if (release != null) {
                    Log.d(TAG, "🆕 Nueva versión encontrada: ${release.name}")
                    
                    // Mostrar notificación silenciosa
                    showUpdateNotification(
                        versionName = release.extractVersionName(),
                        releaseNotes = release.body ?: "Nueva versión disponible"
                    )
                    
                    // Descargar APK automáticamente
                    Log.d(TAG, "⬇️ Iniciando descarga automática...")
                    val downloadResult = updateRepository.downloadApk(release) { progress ->
                        Log.d(TAG, "📥 Descarga: $progress%")
                        // Actualizar notificación con progreso
                        updateDownloadNotification(progress)
                    }
                    
                    downloadResult.onSuccess { apkFile ->
                        Log.d(TAG, "✅ Descarga completada: ${apkFile.name}")
                        showReadyToInstallNotification(release.extractVersionName())
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Error en descarga: ${error.message}")
                        showDownloadErrorNotification()
                    }
                } else {
                    Log.d(TAG, "✓ App actualizada, no hay nuevas versiones")
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Error verificando actualizaciones: ${error.message}")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado en UpdateCheckWorker", e)
            Result.retry()
        }
    }
    
    /**
     * Muestra notificación inicial cuando se detecta actualización
     */
    private fun showUpdateNotification(versionName: String, releaseNotes: String) {
        createNotificationChannel()
        
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_slideshow) // Cambiar por un ícono de actualización
            .setContentTitle("Nueva versión disponible")
            .setContentText("ControlOperador $versionName")
            .setStyle(NotificationCompat.BigTextStyle().bigText(releaseNotes.take(200)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Actualiza notificación con progreso de descarga
     */
    private fun updateDownloadNotification(progress: Int) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando actualización")
            .setContentText("Descargando ControlOperador... $progress%")
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Muestra notificación cuando APK está listo para instalar
     */
    private fun showReadyToInstallNotification(versionName: String) {
        createNotificationChannel()
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("trigger_update_install", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Actualización lista")
            .setContentText("Toca para instalar ControlOperador $versionName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Muestra notificación de error en descarga
     */
    private fun showDownloadErrorNotification() {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Error en actualización")
            .setContentText("No se pudo descargar la actualización. Se reintentará más tarde.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Crea el canal de notificaciones (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Actualizaciones de la App",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre actualizaciones de ControlOperador"
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
