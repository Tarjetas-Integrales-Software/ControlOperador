package com.example.controloperador.data.repository

import android.content.Context
import android.util.Log
import com.example.controloperador.BuildConfig
import com.example.controloperador.data.api.RetrofitClient
import com.example.controloperador.data.api.model.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Repository para gestionar actualizaciones de la app desde GitHub Releases
 */
class UpdateRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateRepository"
        private const val UPDATES_DIR = "updates"
    }
    
    /**
     * Verifica si hay una nueva versión disponible en GitHub
     * 
     * @return GitHubRelease si hay actualización, null si no hay o hubo error
     */
    suspend fun checkForUpdates(): Result<GitHubRelease?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Verificando actualizaciones desde GitHub...")
            Log.d(TAG, "📦 Versión actual: ${BuildConfig.VERSION_NAME} (code: ${BuildConfig.VERSION_CODE})")
            
            val response = RetrofitClient.githubApiService.getLatestRelease(
                owner = BuildConfig.GITHUB_REPO_OWNER,
                repo = BuildConfig.GITHUB_REPO_NAME
            )
            
            if (!response.isSuccessful) {
                val errorMsg = "Error HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "❌ $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            val release = response.body()
            if (release == null) {
                Log.w(TAG, "⚠️ Respuesta vacía de GitHub API")
                return@withContext Result.failure(Exception("Respuesta vacía"))
            }
            
            Log.d(TAG, "📋 Release encontrado: ${release.name} (${release.tagName})")
            Log.d(TAG, "   - Draft: ${release.draft}, Prerelease: ${release.prerelease}")
            Log.d(TAG, "   - Assets: ${release.assets.size}")
            
            // Filtrar releases en draft o prerelease
            if (release.draft || release.prerelease) {
                Log.d(TAG, "⏭️ Ignorando draft/prerelease")
                return@withContext Result.success(null)
            }
            
            // Verificar que tenga un APK
            val apkAsset = release.findApkAsset()
            if (apkAsset == null) {
                Log.w(TAG, "⚠️ Release sin APK adjunto")
                return@withContext Result.success(null)
            }
            
            Log.d(TAG, "📱 APK encontrado: ${apkAsset.name} (${apkAsset.getFormattedSize()})")
            
            // Comparar versiones
            val releaseVersionCode = release.extractVersionCode()
            val currentVersionCode = BuildConfig.VERSION_CODE
            
            Log.d(TAG, "🔢 Comparando versiones:")
            Log.d(TAG, "   - Actual: $currentVersionCode")
            Log.d(TAG, "   - Disponible: $releaseVersionCode")
            
            if (releaseVersionCode == null) {
                Log.w(TAG, "⚠️ No se pudo extraer versionCode del tag: ${release.tagName}")
                return@withContext Result.success(null)
            }
            
            if (releaseVersionCode > currentVersionCode) {
                Log.d(TAG, "✅ ¡Nueva versión disponible! $currentVersionCode -> $releaseVersionCode")
                return@withContext Result.success(release)
            } else {
                Log.d(TAG, "✓ App está actualizada (versión $currentVersionCode)")
                return@withContext Result.success(null)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando actualizaciones", e)
            Result.failure(e)
        }
    }
    
    /**
     * Descarga el APK desde GitHub
     * 
     * @param release Release de GitHub que contiene el APK
     * @param onProgress Callback para reportar progreso (0-100)
     * @return File del APK descargado o null si falla
     */
    suspend fun downloadApk(
        release: GitHubRelease,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val apkAsset = release.findApkAsset()
                ?: return@withContext Result.failure(Exception("No se encontró APK en el release"))
            
            Log.d(TAG, "⬇️ Descargando APK desde: ${apkAsset.browserDownloadUrl}")
            Log.d(TAG, "📦 Tamaño esperado: ${apkAsset.getFormattedSize()}")
            
            val response = RetrofitClient.githubApiService.downloadFile(apkAsset.browserDownloadUrl)
            
            if (!response.isSuccessful) {
                val errorMsg = "Error descargando APK: ${response.code()}"
                Log.e(TAG, "❌ $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            val body = response.body()
                ?: return@withContext Result.failure(Exception("Respuesta vacía"))
            
            // Crear directorio de actualizaciones
            val updatesDir = File(context.filesDir, UPDATES_DIR)
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
                Log.d(TAG, "📁 Directorio creado: ${updatesDir.absolutePath}")
            }
            
            // Limpiar APKs antiguos
            updatesDir.listFiles()?.forEach { oldApk ->
                if (oldApk.name.endsWith(".apk")) {
                    oldApk.delete()
                    Log.d(TAG, "🗑️ APK antiguo eliminado: ${oldApk.name}")
                }
            }
            
            // Guardar APK con nombre descriptivo
            val apkFileName = "ControlOperador-${release.extractVersionName()}.apk"
            val apkFile = File(updatesDir, apkFileName)
            
            // Escribir archivo en chunks con progreso
            var downloadedBytes = 0L
            val totalBytes = apkAsset.size
            
            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // Reportar progreso cada ~1MB
                        if (downloadedBytes % (1024 * 1024) == 0L || downloadedBytes == totalBytes) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            onProgress(progress)
                            Log.d(TAG, "⏳ Progreso: $progress% ($downloadedBytes / $totalBytes bytes)")
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ APK descargado exitosamente: ${apkFile.absolutePath}")
            Log.d(TAG, "📊 Tamaño final: ${apkFile.length()} bytes")
            
            Result.success(apkFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando APK", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene información sobre el directorio de actualizaciones
     */
    fun getUpdatesDirectory(): File {
        return File(context.filesDir, UPDATES_DIR)
    }
    
    /**
     * Limpia todos los APKs descargados
     */
    fun cleanupDownloadedApks() {
        try {
            val updatesDir = getUpdatesDirectory()
            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk")) {
                    file.delete()
                    Log.d(TAG, "🗑️ APK eliminado: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando APKs", e)
        }
    }
}
