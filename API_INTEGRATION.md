# Integración con API - Información del Operador

## 📡 Estado Actual

**Actualmente la aplicación usa datos fijos/mock** para mostrar información del operador en el navigation drawer:

- **RUTA**: C30-C75 (fijo)
- **UNIDAD**: 00001 (fijo)
- **FECHA Y HORA**: Actualización en tiempo real cada segundo ✅

## 🔄 Preparación para API Real

La arquitectura está lista para integrar una API REST cuando esté disponible.

### Archivos Preparados

1. **`data/OperatorRepository.kt`** - Repositorio con métodos preparados para API
2. **`MainActivity.kt`** - Ya consume el repositorio para obtener datos

### 📋 Contrato de API Esperado

#### Endpoint Sugerido
```
GET /api/operator/{operatorCode}/info
```

#### Request Headers
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

#### Response Body Esperado
```json
{
  "success": true,
  "data": {
    "operatorCode": "12345",
    "route": "C30-C75",
    "unitNumber": "00001",
    "routeDescription": "Centro - Terminal Norte",
    "status": "active",
    "lastUpdate": "2025-10-21T14:30:00Z"
  }
}
```

#### Error Response
```json
{
  "success": false,
  "error": {
    "code": "OPERATOR_NOT_FOUND",
    "message": "No se encontró información para el operador"
  }
}
```

## 🔧 Pasos para Integrar API Real

### 1. Agregar Dependencias (build.gradle.kts)

```kotlin
dependencies {
    // Retrofit para llamadas HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp para logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines para operaciones asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Lifecycle y ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

### 2. Crear Servicio de API

Crear archivo: `data/api/OperatorApiService.kt`

```kotlin
package com.example.controloperador.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OperatorApiService {
    
    @GET("/api/operator/{operatorCode}/info")
    suspend fun getOperatorInfo(
        @Path("operatorCode") operatorCode: String,
        @Header("Authorization") authToken: String
    ): Response<OperatorInfoResponse>
}

data class OperatorInfoResponse(
    val success: Boolean,
    val data: OperatorInfoData?,
    val error: ErrorData?
)

data class OperatorInfoData(
    val operatorCode: String,
    val route: String,
    val unitNumber: String,
    val routeDescription: String?,
    val status: String,
    val lastUpdate: String
)

data class ErrorData(
    val code: String,
    val message: String
)
```

### 3. Configurar Retrofit

Crear archivo: `data/api/RetrofitClient.kt`

```kotlin
package com.example.controloperador.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private const val BASE_URL = "https://api.tu-empresa.com/" // Cambiar por URL real
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val operatorApiService: OperatorApiService by lazy {
        retrofit.create(OperatorApiService::class.java)
    }
}
```

### 4. Actualizar OperatorRepository

En `data/OperatorRepository.kt`, reemplazar:

```kotlin
package com.example.controloperador.data

import com.example.controloperador.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperatorRepository {
    
    private val apiService = RetrofitClient.operatorApiService
    
    /**
     * Obtiene información del operador desde la API
     */
    suspend fun getOperatorInfo(operatorCode: String, authToken: String): Result<OperatorInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOperatorInfo(operatorCode, "Bearer $authToken")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        val operatorInfo = OperatorInfo(
                            operatorCode = data.operatorCode,
                            route = data.route,
                            unitNumber = data.unitNumber
                        )
                        Result.success(operatorInfo)
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    val errorMessage = response.body()?.error?.message ?: "Error desconocido"
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### 5. Actualizar MainActivity

En `MainActivity.kt`, cambiar el método `updateNavHeader()`:

```kotlin
private fun updateNavHeader() {
    val operatorCode = sessionManager.getOperatorCode()
    if (operatorCode != null) {
        // Lanzar coroutine para llamada asíncrona
        lifecycleScope.launch {
            try {
                // TODO: Obtener token de autenticación desde SessionManager
                val authToken = "tu_token_aqui"
                
                val result = operatorRepository.getOperatorInfo(operatorCode, authToken)
                
                result.onSuccess { info ->
                    operatorInfo = info
                    textViewOperatorCode?.text = "Operador: ${info.operatorCode}"
                    textViewRoute?.text = "RUTA: ${info.route}"
                    textViewUnit?.text = "UNIDAD: ${info.unitNumber}"
                    startDateTimeUpdates()
                }
                
                result.onFailure { error ->
                    // Manejar error - mostrar mensaje o usar datos en caché
                    Snackbar.make(
                        binding.root,
                        "Error al cargar información: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Manejar excepción
            }
        }
    }
}
```

### 6. Agregar Permisos (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🔒 Seguridad

### Token de Autenticación
- Guardar token JWT en `SessionManager` después del login
- Incluir token en todas las peticiones API
- Renovar token antes de que expire

### Ejemplo en SessionManager:
```kotlin
fun saveAuthToken(token: String) {
    prefs.edit().apply {
        putString(KEY_AUTH_TOKEN, token)
        apply()
    }
}

fun getAuthToken(): String? {
    return prefs.getString(KEY_AUTH_TOKEN, null)
}
```

## 📱 Manejo de Errores

### Casos a Manejar
1. **Sin conexión a internet** - Mostrar datos en caché si existen
2. **Token expirado** - Redirigir a login
3. **Servidor no disponible** - Mostrar mensaje y reintentar
4. **Datos no encontrados** - Mostrar mensaje apropiado

## 🧪 Testing

### Datos de Prueba Actuales
```kotlin
// Valores mock en OperatorRepository.kt
route = "C30-C75"
unitNumber = "00001"
```

### Para Probar con API Real
1. Actualizar `BASE_URL` en `RetrofitClient`
2. Obtener token válido del backend
3. Verificar que el endpoint esté disponible
4. Probar con diferentes códigos de operador

## 📊 Actualización de Datos

### Frecuencia Sugerida
- **Al iniciar sesión**: Cargar datos
- **onResume()**: Refrescar si han pasado > 5 minutos
- **Pull to refresh**: Permitir actualización manual
- **WebSocket**: Para actualizaciones en tiempo real (avanzado)

## 🎯 Checklist de Integración

- [ ] Agregar dependencias de Retrofit y Coroutines
- [ ] Crear modelos de respuesta de API
- [ ] Configurar RetrofitClient con URL base
- [ ] Implementar OperatorApiService
- [ ] Actualizar OperatorRepository con llamadas reales
- [ ] Modificar MainActivity para usar coroutines
- [ ] Implementar manejo de errores robusto
- [ ] Agregar cache de datos (Room Database opcional)
- [ ] Implementar renovación de token
- [ ] Agregar logs para debugging
- [ ] Testing con backend real
- [ ] Optimizar para modo offline

## 📞 Contacto Backend Team

Cuando la API esté lista, necesitarás:
1. URL base del servidor
2. Especificación completa de endpoints
3. Formato de autenticación (JWT, OAuth, etc.)
4. Códigos de error posibles
5. Ambiente de pruebas/staging

---

**Nota**: Por ahora la app funciona con datos fijos. La arquitectura está preparada para transición suave a API real cuando esté disponible.
