package com.example.controloperador.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo que representa un Release de GitHub
 * Basado en: https://api.github.com/repos/{owner}/{repo}/releases/latest
 */
data class GitHubRelease(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("tag_name")
    val tagName: String,  // Ejemplo: "v1.0.7"
    
    @SerializedName("name")
    val name: String?,  // Ejemplo: "Control Operador 1.0.7"
    
    @SerializedName("body")
    val body: String?,  // Release notes en markdown
    
    @SerializedName("draft")
    val draft: Boolean,
    
    @SerializedName("prerelease")
    val prerelease: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("published_at")
    val publishedAt: String?,
    
    @SerializedName("assets")
    val assets: List<GitHubAsset>,
    
    @SerializedName("html_url")
    val htmlUrl: String,
    
    @SerializedName("author")
    val author: GitHubAuthor?
) {
    /**
     * Extrae el versionCode del tag_name
     * Ejemplos: "v1.0.7" -> 7, "v2.3.15" -> 15
     * Asume que el último número después del último punto es el versionCode
     */
    fun extractVersionCode(): Int? {
        return try {
            val versionPart = tagName.removePrefix("v")
            val parts = versionPart.split(".")
            parts.lastOrNull()?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extrae el versionName del tag_name
     * Ejemplos: "v1.0.7" -> "1.0.7"
     */
    fun extractVersionName(): String {
        return tagName.removePrefix("v")
    }
    
    /**
     * Busca el APK principal en los assets
     * Prioriza archivos que contengan "release" en el nombre
     */
    fun findApkAsset(): GitHubAsset? {
        return assets.firstOrNull { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) && 
            asset.contentType == "application/vnd.android.package-archive"
        }
    }
}

/**
 * Modelo que representa un Asset (archivo adjunto) del Release
 */
data class GitHubAsset(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,  // Ejemplo: "ControlOperador-v1.0.7-release.apk"
    
    @SerializedName("label")
    val label: String?,
    
    @SerializedName("content_type")
    val contentType: String,  // "application/vnd.android.package-archive"
    
    @SerializedName("state")
    val state: String,  // "uploaded"
    
    @SerializedName("size")
    val size: Long,  // Tamaño en bytes
    
    @SerializedName("download_count")
    val downloadCount: Int,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String,
    
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,  // URL directa para descargar
    
    @SerializedName("uploader")
    val uploader: GitHubAuthor?
) {
    /**
     * Formatea el tamaño del archivo para mostrar al usuario
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }
}

/**
 * Modelo que representa al autor del Release
 */
data class GitHubAuthor(
    @SerializedName("login")
    val login: String,
    
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    
    @SerializedName("html_url")
    val htmlUrl: String
)
