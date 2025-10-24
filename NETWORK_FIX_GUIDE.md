# 🔧 Corrección de Errores de Red - HTTP CLEARTEXT

## ❌ Problemas encontrados:

### 1. URL duplicada
```
❌ http://172.16.20.10:8000/api/v1/v1/auth/login
                              ↑      ↑
                           duplicado
```

### 2. CLEARTEXT no permitido
```
java.net.UnknownServiceException: 
CLEARTEXT communication to 172.16.20.10 not permitted by network security policy
```

---

## ✅ Soluciones aplicadas:

### 1. Corregir rutas en ApiService.kt

**Antes:**
```kotlin
@POST("v1/auth/login")  // ❌ Se duplicaba con BASE_URL
```

**Ahora:**
```kotlin
@POST("auth/login")  // ✅ Correcto
```

**Resultado:**
```
✅ http://172.16.20.10:8000/api/v1/auth/login
```

### 2. Configuración de seguridad de red

**Archivo creado:** `res/xml/network_security_config.xml`

```xml
<network-security-config>
    <!-- Permitir HTTP para desarrollo -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">172.16.20.10</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    
    <!-- Solo HTTPS en producción -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**AndroidManifest.xml actualizado:**
```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config">
```

---

## 🎯 ¿Qué hace esta configuración?

### En Desarrollo (HTTP permitido):
- ✅ Permite HTTP a `172.16.20.10` (tu servidor)
- ✅ Permite HTTP a `10.0.2.2` (localhost del emulador)
- ✅ Permite HTTP a `localhost`

### En Producción (Solo HTTPS):
- ✅ Solo permite HTTPS para otros dominios
- ✅ `backtransportistas.tarjetasintegrales.mx` usará HTTPS automáticamente
- ✅ Mayor seguridad

---

## 🔍 Verificar que funciona

### Ejecuta la app de nuevo:

**Logcat mostrará:**
```
--> POST http://172.16.20.10:8000/api/v1/auth/login
Content-Type: application/json; charset=UTF-8
{"operator_code":"12345"}
<-- 200 OK (respuesta exitosa)
```

### Si ves error 404:

Verifica que tu backend Laravel esté corriendo:
```bash
php artisan serve --host=172.16.20.10 --port=8000
```

### Si ves error de conexión:

1. Verifica que estés en la misma red
2. Verifica que el firewall permita conexiones al puerto 8000
3. Prueba desde el navegador: `http://172.16.20.10:8000/api/v1/auth/login`

---

## 📝 Archivos modificados:

1. ✅ `ApiService.kt` - Rutas corregidas sin `/v1/` duplicado
2. ✅ `network_security_config.xml` - Configuración de red creada
3. ✅ `AndroidManifest.xml` - Referencia a configuración agregada

---

## ⚠️ Importante para producción

Esta configuración permite HTTP **solo** para las IPs específicas de desarrollo. Cuando uses la URL de producción con HTTPS, no habrá ningún problema:

```kotlin
// Producción usa HTTPS automáticamente
https://backtransportistas.tarjetasintegrales.mx:806/api/v1/
```

---

## 🚀 Próximos pasos

1. **Clean & Rebuild**: Build → Clean Project → Rebuild Project
2. **Ejecutar la app**: Presiona Run ▶️
3. **Login con 54321**: Debería funcionar offline
4. **Login con 12345**: Debería conectar al backend (si está corriendo)

---

**Fecha:** 24 de octubre de 2025
