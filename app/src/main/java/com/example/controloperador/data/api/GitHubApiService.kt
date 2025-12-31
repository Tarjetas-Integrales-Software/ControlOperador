package com.example.controloperador.data.api

import com.example.controloperador.data.api.model.GitHubRelease
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * API Service para interactuar con GitHub Releases
 * Documentación: https://docs.github.com/en/rest/releases/releases
 */
interface GitHubApiService {
    
    /**
     * Obtiene el release más reciente (latest) del repositorio
     * 
     * Endpoint: GET /repos/{owner}/{repo}/releases/latest
     * Ejemplo: https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest
     * 
     * @param owner Propietario del repositorio (ej: "Tarjetas-Integrales-Software")
     * @param repo Nombre del repositorio (ej: "ControlOperador")
     * @return GitHubRelease con información del release más reciente
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>
    
    /**
     * Descarga un archivo desde una URL arbitraria
     * Se usa para descargar el APK desde browser_download_url
     * 
     * @Streaming indica que el archivo se descargará en chunks (no todo en memoria)
     * 
     * @param url URL completa del archivo (ej: browser_download_url del asset)
     * @return ResponseBody con el contenido del archivo
     */
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>
}
