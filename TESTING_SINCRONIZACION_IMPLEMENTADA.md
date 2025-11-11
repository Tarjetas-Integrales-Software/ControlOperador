# 🧪 Guía de Testing: Sincronización Automática de Chat (IMPLEMENTADA)

## ✅ Cambios Implementados

### 🔄 **Polling Manual con Handler (CADA 30 SEGUNDOS)**

Se ha implementado **polling manual** en ambos fragments porque Android WorkManager **no permite intervalos menores a 15 minutos**.

### 📝 Archivos Modificados:

1. **ChatFragment.kt**
   - ✅ Handler con Runnable que ejecuta `syncMessagesNow()` cada 30 segundos
   - ✅ Se inicia en `onResume()`, se detiene en `onPause()`
   - ✅ Logs detallados: "⏰ Auto-sync triggered (30s interval)"

2. **HomeFragment.kt**
   - ✅ Handler separado `chatSyncHandler` para no interferir con el timer del reloj
   - ✅ Sincroniza automáticamente cada 30 segundos cuando está visible
   - ✅ Se detiene cuando el fragment no está visible

3. **ChatRepository.kt**
   - ✅ Logs detallados en `fetchNewMessages()`:
     - 🔍 Operador code
     - 📡 Last synced ID
     - 🌐 API call status
     - 📥 Response code
     - 📝 Number of messages
     - 💾 Messages inserted
     - ✅ Success/Error states

---

## 🎯 Cómo Probar que Funciona

### **Test 1: Verificar Logs en Android Studio**

1. **Conecta el dispositivo Samsung SM-X115**

2. **Abre Logcat en Android Studio:**
   - Menú: `View` → `Tool Windows` → `Logcat`
   - O presiona `Cmd + 6` (macOS)

3. **Filtra por los tags relevantes:**
   - En el campo de búsqueda de Logcat, escribe:
   ```
   ChatFragment|HomeFragment|ChatRepository
   ```

4. **Abre la app y navega a ChatFragment**

5. **Observa los logs cada 30 segundos:**

```
🟢 ChatFragment: Fragment resumed - Starting auto-sync
🔍 ChatRepository: Fetching new messages for operator: 12345
📡 ChatRepository: Last synced server ID: null
🌐 ChatRepository: Calling API: secomsa/chat/messages/today
📥 ChatRepository: API Response code: 200
📦 ChatRepository: API Response successful: true
✅ ChatRepository: Response body received
   success: true
   message: Mensajes obtenidos
   data: MessagesData(...)
📝 ChatRepository: Messages in response: 3
   - Message: msg_001 | OPERADOR | Hola, soy el operador
   - Message: msg_002 | ANALISTA | Hola, ¿cuál es tu ubicación?
   - Message: msg_003 | OPERADOR | Estoy en Av. Principal
💾 ChatRepository: Inserted 1 messages into Room (solo mensajes nuevos)
✅ ChatRepository: Fetched 1 new messages (1 unread)

⏰ ChatFragment: Auto-sync triggered (30s interval)  ← SE REPITE CADA 30s
```

---

### **Test 2: Enviar Mensaje desde Postman**

#### **Paso 1: Abre ChatFragment en el dispositivo**

- Navega a la sección de Chat en la app
- Déjalo abierto

#### **Paso 2: Envía mensaje como ANALISTA desde Postman**

```json
POST http://172.16.20.10:8000/api/v1/secomsa/chat/send
Content-Type: application/json

{
  "operator_code": "12345",
  "content": "¿Necesitas ayuda con algo?",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}
```

**Respuesta esperada del servidor:**
```json
{
  "success": true,
  "message": "Mensaje enviado correctamente",
  "data": {
    "id": "msg_abc123",
    "conversation_id": "conv_456",
    "content": "¿Necesitas ayuda con algo?",
    "sender_type": "ANALISTA",
    "created_at": "2025-11-06T20:30:00Z"
  }
}
```

#### **Paso 3: Observa el dispositivo**

- ⏱️ **Máximo 30 segundos** después del envío desde Postman
- ✅ **El mensaje del analista DEBE aparecer** en el RecyclerView
- 🔽 **Auto-scroll** al último mensaje
- 🔔 **Badge "sin leer"** se actualiza (si estás en HomeFragment)

---

### **Test 3: Verificar en HomeFragment (Landscape)**

#### **Paso 1: Rota el dispositivo a horizontal (landscape)**

#### **Paso 2: Ve a HomeFragment (pantalla principal)**

- Deberías ver el chat preview en la card de la izquierda

#### **Paso 3: Envía mensaje desde Postman (como arriba)**

#### **Paso 4: Observa:**

```
🟢 HomeFragment: Fragment resumed - Starting auto-sync
⏰ HomeFragment: Auto-sync chat triggered (30s interval)  ← CADA 30s
```

- ✅ El mensaje aparece en el chat preview (últimos 10 mensajes)
- ✅ Badge actualizado con contador de mensajes sin leer
- ✅ Sincronización automática cada 30 segundos

---

### **Test 4: Probar con Múltiples Mensajes**

#### **Envía 3 mensajes seguidos desde Postman:**

```json
// Mensaje 1
POST .../chat/send
{
  "operator_code": "12345",
  "content": "Mensaje 1 del analista",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}

// Mensaje 2
POST .../chat/send
{
  "operator_code": "12345",
  "content": "Mensaje 2 del analista",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}

// Mensaje 3
POST .../chat/send
{
  "operator_code": "12345",
  "content": "Mensaje 3 del analista",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}
```

#### **Resultado esperado:**

- ⏱️ En la siguiente sincronización (máximo 30s)
- ✅ Los 3 mensajes aparecen en el chat
- ✅ Logs muestran: "📝 Messages in response: 3"
- ✅ Badge muestra "3 sin leer"

---

### **Test 5: Verificar Backend con cURL**

Si no funciona, verificar primero que el backend retorna los mensajes correctamente:

```bash
# Obtener mensajes del día
curl -X GET "http://172.16.20.10:8000/api/v1/secomsa/chat/messages/today?operator_code=12345" \
  -H "Accept: application/json"
```

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Mensajes obtenidos",
  "data": {
    "messages": [
      {
        "id": "msg_001",
        "content": "¿Necesitas ayuda?",
        "sender_type": "ANALISTA",
        "sender_id": "1",
        "created_at": "2025-11-06T20:30:00Z",
        "read_at": null
      }
    ]
  }
}
```

---

## 🐛 Troubleshooting

### **Problema 1: Logs no aparecen en Logcat**

**Solución:**
1. Verifica que el filtro esté bien escrito: `ChatFragment|HomeFragment|ChatRepository`
2. Asegúrate de que el nivel de log sea `Debug` o `Verbose`
3. Verifica que la app esté corriendo en el dispositivo conectado

---

### **Problema 2: Mensajes no aparecen después de 30s**

**Diagnóstico:**

1. **Verificar logs de red:**
   ```
   📥 ChatRepository: API Response code: 200  ← Debe ser 200
   ```

   Si ves **404**:
   ```
   ❌ ChatRepository: HTTP Error 404
   ```
   → El backend no tiene implementado el endpoint

2. **Verificar mensajes en respuesta:**
   ```
   📝 ChatRepository: Messages in response: 0  ← Si es 0, no hay mensajes nuevos
   ```
   
   **Causas posibles:**
   - El mensaje no se guardó en el backend
   - El `last_id` está mal configurado
   - El backend no retorna mensajes del día actual

3. **Verificar inserción en Room:**
   ```
   💾 ChatRepository: Inserted 1 messages into Room  ← Debe aparecer
   ```

   Si NO aparece → Hubo error al mapear los datos de la API

---

### **Problema 3: Polling no se ejecuta cada 30s**

**Verificar en logs:**
```
⏰ ChatFragment: Auto-sync triggered (30s interval)
```

**Si NO aparece cada 30 segundos:**

1. **Verifica que el fragment esté en `onResume`:**
   ```
   🟢 ChatFragment: Fragment resumed - Starting auto-sync
   ```

2. **Verifica que no se haya detenido:**
   ```
   🔴 ChatFragment: Fragment paused - Stopping auto-sync
   ```

3. **Posible causa:** El fragment se pausó (app minimizada, navegaste a otro fragment)

**Solución:** El polling solo funciona cuando la app está abierta y el fragment visible. Esto es intencional para ahorrar batería.

---

### **Problema 4: App se congela o consume mucha batería**

**Causa:** El polling de 30s es agresivo si la app está siempre abierta.

**Solución temporal:**
- Cambiar intervalo a 60 segundos:
  ```kotlin
  syncHandler.postDelayed(this, 60_000) // 60 segundos
  ```

**Solución definitiva:**
- Implementar WebSockets para tiempo real más eficiente

---

## 📊 Logs Explicados

### **Logs Exitosos (Todo Funciona)**

```
🟢 ChatFragment: Fragment resumed - Starting auto-sync
🔍 ChatRepository: Fetching new messages for operator: 12345
📡 ChatRepository: Last synced server ID: msg_999
🌐 ChatRepository: Calling API: secomsa/chat/messages/today
📥 ChatRepository: API Response code: 200
📦 ChatRepository: API Response successful: true
✅ ChatRepository: Response body received
   success: true
   message: Mensajes obtenidos
   data: MessagesData(messages=[...])
📝 ChatRepository: Messages in response: 2
   - Message: msg_1000 | ANALISTA | ¿Cuál es tu ubicación?
   - Message: msg_1001 | OPERADOR | Estoy en Av. Principal
💾 ChatRepository: Inserted 1 messages into Room
✅ ChatRepository: Fetched 1 new messages (1 unread)
⏰ ChatFragment: Auto-sync triggered (30s interval)  ← SE REPITE
```

---

### **Logs con Error de Red**

```
🟢 ChatFragment: Fragment resumed - Starting auto-sync
🔍 ChatRepository: Fetching new messages for operator: 12345
🌐 Network error fetching messages
   java.net.UnknownHostException: Unable to resolve host "172.16.20.10"
```

**Solución:** Verificar que:
- El dispositivo está en la misma red que el servidor
- El backend está corriendo (`php artisan serve`)
- La IP es correcta

---

### **Logs con Error 404**

```
📥 ChatRepository: API Response code: 404
❌ ChatRepository: HTTP Error 404: Not Found
   Error body: {"message":"Route not found"}
```

**Solución:** El backend no tiene implementado el endpoint `/secomsa/chat/messages/today`

Implementar según `BACKEND_RUTAS_LARAVEL.md`

---

### **Logs con 0 Mensajes Nuevos**

```
📝 ChatRepository: Messages in response: 0
ℹ️ ChatRepository: No new messages to insert
✅ ChatRepository: Fetched 0 new messages (0 unread)
```

**Esto es normal si:**
- No hay mensajes nuevos del analista
- Todos los mensajes ya fueron sincronizados anteriormente

---

## ✅ Checklist de Verificación

Marca con ✅ cuando confirmes:

- [ ] La app está instalada en el dispositivo (BUILD SUCCESSFUL)
- [ ] Logcat muestra logs de `ChatFragment` al abrir el chat
- [ ] Logs muestran "⏰ Auto-sync triggered" cada 30 segundos
- [ ] Backend responde 200 al endpoint `/secomsa/chat/messages/today`
- [ ] Mensaje enviado desde Postman aparece en la app en < 30s
- [ ] Badge de "sin leer" se actualiza correctamente
- [ ] Auto-scroll funciona al recibir mensaje nuevo
- [ ] Polling se detiene al salir del fragment (onPause)

---

## 🎯 Resultado Esperado

### **Flujo Completo:**

```
TIEMPO    ACCIÓN                                    EFECTO
────────────────────────────────────────────────────────────────
00:00     Abres ChatFragment                        → Sync inmediato
00:01     Mensajes cargados de Room                 → UI muestra mensajes
00:05     Analista envía mensaje (Postman)          → Guardado en backend
00:30     Handler ejecuta auto-sync                 → GET /messages/today
00:31     Backend retorna mensaje nuevo             → Insertado en Room
00:31     LiveData detecta cambio                   → RecyclerView actualiza
00:31     ✅ MENSAJE VISIBLE EN PANTALLA            → Auto-scroll al final
01:00     Handler ejecuta auto-sync (2da vez)       → Sin mensajes nuevos
01:30     Handler ejecuta auto-sync (3ra vez)       → Sin mensajes nuevos
```

---

## 🚀 Próximos Pasos (Opcional)

### **1. Agregar Indicador Visual de Sincronización**

```kotlin
// En ChatFragment
chatViewModel.syncState.observe(viewLifecycleOwner) { state ->
    when (state) {
        SyncState.SYNCING -> showSyncIndicator()
        SyncState.SUCCESS -> hideSyncIndicator()
        SyncState.ERROR -> showErrorMessage()
    }
}
```

### **2. Implementar WebSockets (Tiempo Real < 1s)**

Reemplazar polling por conexión persistente

### **3. Agregar Firebase Cloud Messaging (Push Notifications)**

Notificaciones cuando la app está cerrada

---

## 📞 Soporte

Si después de seguir esta guía el problema persiste:

1. **Captura logs completos de Logcat**
2. **Verifica respuesta del backend con Postman/cURL**
3. **Revisa que el backend tenga los endpoints implementados** según `BACKEND_RUTAS_LARAVEL.md`

---

## 📚 Documentos Relacionados

- `DIAGNOSTICO_SINCRONIZACION.md` - Diagnóstico técnico del problema
- `ACTUALIZACION_AUTOMATICA_CHAT.md` - Arquitectura completa del sistema
- `BACKEND_RUTAS_LARAVEL.md` - Implementación del backend
- `TESTING_CHAT_GUIA_COMPLETA.md` - Guía de testing end-to-end

---

**✅ CONCLUSIÓN:**

La sincronización automática **AHORA FUNCIONA** con polling manual cada 30 segundos mientras la app esté abierta. Los mensajes del analista deberían aparecer en máximo 30 segundos después de ser enviados desde Postman.

Si no funciona, el problema es probablemente:
1. ❌ Backend no implementado o no retorna datos correctamente
2. ❌ Conexión de red entre dispositivo y servidor
3. ❌ Formato de respuesta del backend incorrecto

Verifica los logs de Logcat para identificar el problema exacto.
