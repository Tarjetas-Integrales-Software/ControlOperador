package com.example.controloperador.data.api

import com.example.controloperador.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit configurado para comunicación con el backend Laravel
 * Implementa Singleton para una única instancia en toda la app
 */
object RetrofitClient {
    
    /**
     * URL base del backend Laravel
     * Se obtiene automáticamente según el tipo de build:
     * 
     * DEBUG (desarrollo):
     * - "http://172.16.20.10:8000/api/v1/"
     * 
     * RELEASE (producción):
     * - "https://backtransportistas.tarjetasintegrales.mx:806/api/v1/"
     * 
     * Configurado en build.gradle.kts
     */
    private val BASE_URL = BuildConfig.BASE_URL
    
    /**
     * Cliente OkHttp con interceptores y timeouts configurados
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC  // En producción solo headers y status
            }
        }
        
        // Interceptor para agregar headers comunes
        val headerInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)  // Headers primero
            .addInterceptor(loggingInterceptor)  // Logging después
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)  // Reintentar en caso de fallo
            .build()
    }
    
    /**
     * Configuración de Gson para serialización/deserialización JSON
     */
    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * Instancia de Retrofit configurada
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Servicio API para consumir endpoints
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    /**
     * Servicio API para chat
     */
    val chatApiService: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }
    
    /**
     * Servicio API para mensajes de voz
     * Endpoints específicos para recibir y gestionar mensajes de voz del operador
     */
    val voiceMessageApiService: VoiceMessageApiService by lazy {
        retrofit.create(VoiceMessageApiService::class.java)
    }
    
    /**
     * Cliente Retrofit específico para GitHub API
     * Base URL: https://api.github.com/
     */
    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Servicio API para GitHub Releases
     * Permite verificar actualizaciones de la app
     */
    val githubApiService: GitHubApiService by lazy {
        githubRetrofit.create(GitHubApiService::class.java)
    }
}
