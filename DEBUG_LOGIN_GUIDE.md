# 🐛 Debug Guide - Login Issue

## ✅ Cambios implementados:

1. ✅ **Modelo actualizado**: `OperatorData` ahora soporta todos los campos del backend
2. ✅ **Logs agregados**: Para rastrear el flujo de autenticación

---

## 📋 Pasos para debug:

### 1. Limpia y recompila el proyecto

```bash
Build → Clean Project
Build → Rebuild Project
```

### 2. Ejecuta la app

Presiona Run ▶️

### 3. Intenta hacer login con `12345`

### 4. Filtra los logs en Logcat

**En Android Studio:**
1. Abre la pestaña **Logcat** (abajo)
2. En el filtro, pon: `AuthRepository|LoginViewModel`
3. Nivel: **Debug**

### 5. Busca estas líneas en Logcat:

```
AuthRepository: Response code: 200
AuthRepository: Response successful: true
AuthRepository: Response body: ApiResponse(...)
AuthRepository: Body success: true
AuthRepository: Body data: LoginResponse(...)
AuthRepository: Login successful for operator: 12345
AuthRepository: Operator name: Juan Pérez García

LoginViewModel: Authentication successful
LoginViewModel: Operator code: 12345
LoginViewModel: Operator name: Juan Pérez García
LoginViewModel: LoginState updated to Success
```

---

## ❓ Posibles problemas:

### Problema 1: Parsing error

Si ves:
```
AuthRepository: Response body: null
```

**Causa**: Gson no puede parsear la respuesta  
**Solución**: Verificar que los nombres de campos coincidan

### Problema 2: Success = false

Si ves:
```
AuthRepository: Body success: false
```

**Causa**: El backend devuelve `success: false`  
**Solución**: Verificar la lógica del backend

### Problema 3: Data = null

Si ves:
```
AuthRepository: Body data: null
```

**Causa**: El campo `data` está vacío  
**Solución**: Verificar estructura de respuesta del backend

---

## 📊 Respuesta esperada del backend:

```json
{
    "success": true,
    "message": "Autenticación exitosa.",
    "data": {
        "operator": {
            "id": 0,
            "operator_code": "12345",
            "name": "Juan Pérez García",
            "nombre": "Juan",
            "apellido_paterno": "Pérez",
            "apellido_materno": "García",
            "corredor": {
                "id": 1,
                "nombre": "Ruta México-Guadalajara",
                "transportista": "Transportes del Norte SA"
            },
            "last_login": "2025-10-24T17:15:04.940000Z"
        },
        "session": {
            "expires_in": 28800
        }
    }
}
```

---

## 🔧 Modelo actualizado (OperatorData):

```kotlin
data class OperatorData(
    val id: Long,
    val operator_code: String,
    val name: String,
    val nombre: String? = null,
    val apellido_paterno: String? = null,
    val apellido_materno: String? = null,
    val corredor: CorredorData? = null,
    val last_login: String?
)

data class CorredorData(
    val id: Int,
    val nombre: String,
    val transportista: String
)
```

---

## 📝 Qué hacer después:

1. Ejecuta la app
2. Intenta login con `12345`
3. **Copia TODOS los logs** que veas en Logcat con el filtro `AuthRepository|LoginViewModel`
4. **Comparte los logs** completos

Con esos logs podré identificar exactamente dónde está el problema. 🔍

---

**Fecha:** 24 de octubre de 2025
