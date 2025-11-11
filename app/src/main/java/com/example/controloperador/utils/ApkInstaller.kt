package com.example.controloperador.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidad para instalar APKs descargados desde GitHub
 */
object ApkInstaller {
    
    private const val TAG = "ApkInstaller"
    
    /**
     * Verifica si la app tiene permisos para instalar APKs desconocidos
     * Solo requerido en Android 8.0 (API 26) y superior
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // En versiones anteriores no se requiere permiso especial
        }
    }
    
    /**
     * Abre la configuración para permitir instalar APKs desconocidos
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Instala un APK descargado
     * 
     * @param context Contexto de la aplicación
     * @param apkFile Archivo APK a instalar
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "❌ APK no existe: ${apkFile.absolutePath}")
                return
            }
            
            Log.d(TAG, "📲 Instalando APK: ${apkFile.name}")
            Log.d(TAG, "   Ruta: ${apkFile.absolutePath}")
            Log.d(TAG, "   Tamaño: ${apkFile.length()} bytes")
            
            // Verificar permisos
            if (!canInstallPackages(context)) {
                Log.w(TAG, "⚠️ Sin permisos para instalar APKs")
                openInstallPermissionSettings(context)
                return
            }
            
            // Crear URI con FileProvider
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            
            Log.d(TAG, "📎 URI generada: $apkUri")
            
            // Crear intent de instalación
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            // Lanzar instalador del sistema
            context.startActivity(intent)
            Log.d(TAG, "✅ Intent de instalación lanzado")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error instalando APK", e)
        }
    }
    
    /**
     * Verifica si un APK es válido (puede ser instalado)
     */
    fun isValidApk(context: Context, apkFile: File): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                0
            )
            packageInfo != null
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando APK", e)
            false
        }
    }
}
