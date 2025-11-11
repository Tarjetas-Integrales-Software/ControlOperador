# 📨 Cómo Funciona la Carga Automática de Mensajes

**Proyecto**: ControlOperador  
**Fecha**: 4 de Noviembre de 2025  
**Característica**: Carga bidireccional automática de mensajes en tiempo real

---

## 🎯 Resumen Rápido

Los mensajes **SE CARGAN AUTOMÁTICAMENTE** tanto del operador como del analista mediante **3 mecanismos**:

1. ✅ **LiveData Reactivo** - Room notifica cambios en la base de datos local
2. ✅ **ChatSyncWorker** - Sincroniza con el servidor cada 15 segundos
3. ✅ **Sync Manual** - Cuando el usuario abre el chat (onResume)

---

## 🔄 Flujo Completo de Sincronización

### 📤 Cuando el OPERADOR Envía un Mensaje

```
┌─────────────────────────────────────────────────────────┐
│  OPERADOR ENVÍA MENSAJE                                 │
└─────────────────────────────────────────────────────────┘

1. Usuario escribe mensaje en ChatFragment
   ↓
2. ChatViewModel.sendMessage(content)
   ↓
3. ChatRepository.sendMessage()
   ├─ Guarda en Room con status = PENDING
   ├─ LiveData notifica → RecyclerView muestra mensaje (⏳)
   └─ Intenta enviar al servidor (POST /api/chat/send)
      ↓
   ✅ Si éxito:
      ├─ Actualiza Room: status = SENT + server_id
      ├─ LiveData notifica → UI cambia icono (⏳ → ✓)
      └─ Mensaje guardado en SQL Server
      
   ❌ Si falla:
      ├─ Actualiza Room: status = FAILED
      ├─ LiveData notifica → UI muestra error (❌)
      └─ ChatSyncWorker reintentará después

4. Usuario ve mensaje inmediatamente (aunque esté PENDING)
```

---

### 📥 Cuando el ANALISTA Envía un Mensaje

```
┌─────────────────────────────────────────────────────────┐
│  ANALISTA ENVÍA MENSAJE (desde API/Panel Web)          │
└─────────────────────────────────────────────────────────┘

1. Analista envía mensaje vía API
   POST /api/chat/send
   {
     "operator_code": "12345",
     "content": "Hola operador",
     "sender_type": "ANALISTA",
     "sender_id": "1"
   }
   ↓
2. Backend guarda en SQL Server (tabla messages)
   ├─ message_id: nuevo UUID
   ├─ sender_type: ANALISTA
   ├─ created_at: timestamp actual
   └─ read_at: NULL
   ↓
3. En la APP del operador (automático):
   
   OPCIÓN A - ChatSyncWorker (Cada 15 segundos)
   ============================================
   a. WorkManager ejecuta ChatSyncWorker
   b. ChatRepository.fetchNewMessages()
   c. GET /api/chat/messages/today?operator_code=12345
   d. Backend retorna mensajes nuevos
   e. ChatRepository inserta en Room
   f. LiveData notifica → RecyclerView actualizado
   g. Usuario VE mensaje nuevo automáticamente
   
   OPCIÓN B - Sync Manual (onResume)
   ==================================
   a. Usuario abre pantalla de Chat
   b. ChatFragment.onResume()
   c. ChatViewModel.syncMessagesNow()
   d. Mismo flujo que WorkManager
   e. Mensajes aparecen inmediatamente

4. Usuario ve badge "1 sin leer" en HomeFragment
5. Al abrir chat, badge desaparece (markAsRead)
```

---

## 🧩 Componentes Clave

### 1. **ChatFragment.kt** - Observa Cambios

```kotlin
private fun setupObservers() {
    // 🔴 CLAVE: Este observer se ejecuta AUTOMÁTICAMENTE cuando Room cambia
    viewModel.todayMessages.observe(viewLifecycleOwner) { messages ->
        Log.d("ChatFragment", "Received ${messages.size} messages")
        chatAdapter.submitList(messages)  // ← Actualiza RecyclerView
        
        // Auto-scroll al último mensaje
        if (messages.isNotEmpty()) {
            binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }
}

override fun onResume() {
    super.onResume()
    // Forzar sync inmediata al abrir el chat
    viewModel.syncMessagesNow()  // ← NUEVO: Descarga mensajes al abrir
    viewModel.markAllMessagesAsRead()
}
```

**¿Cómo funciona?**
- `todayMessages` es un **LiveData** que observa cambios en Room
- Cada vez que se inserta/actualiza un mensaje en Room, el observer se activa
- El RecyclerView se actualiza automáticamente con los nuevos datos
- `onResume()` fuerza una sincronización inmediata al abrir el chat

---

### 2. **ChatViewModel.kt** - LiveData Reactivo

```kotlin
// 🔴 LiveData que se actualiza automáticamente desde Room
val todayMessages: LiveData<List<ChatMessage>> = _operatorCode.switchMap { operatorCode ->
    _conversation.switchMap { conversation ->
        if (conversation != null) {
            chatRepository.getTodayMessagesLive(conversation.id)  // ← Room LiveData
        } else {
            MutableLiveData(emptyList())
        }
    }
}

// 🆕 NUEVO: Sincronización manual inmediata
fun syncMessagesNow() {
    val conversationId = _conversation.value?.id ?: return
    val operatorCode = _operatorCode.value ?: return
    
    viewModelScope.launch {
        // Reintentar mensajes pendientes
        chatRepository.retryPendingMessages(conversationId, operatorCode)
        
        // Obtener mensajes nuevos del servidor
        chatRepository.fetchNewMessages(conversationId, operatorCode)
        // ↑ Esto inserta en Room → LiveData notifica → UI actualiza
    }
}
```

**¿Cómo funciona?**
- `getTodayMessagesLive()` retorna un LiveData directamente desde Room
- Room notifica automáticamente cuando hay cambios (INSERT/UPDATE)
- No necesitas llamar manualmente `notifyDataSetChanged()`
- `syncMessagesNow()` fuerza descarga inmediata al abrir el chat

---

### 3. **ChatRepository.kt** - Sincronización con Servidor

```kotlin
/**
 * Obtiene mensajes nuevos del servidor y los guarda en Room
 */
suspend fun fetchNewMessages(conversationId: String, operatorCode: String): Result<Int> {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Obtener último mensaje ID local
            val lastMessage = chatMessageDao.getLastMessageByServerId(conversationId)
            val lastId = lastMessage?.serverId
            
            // 2. Llamar al API para obtener mensajes nuevos
            val response = chatApiService.getTodayMessages(
                operatorCode = operatorCode,
                lastId = lastId  // Solo mensajes después de este ID
            )
            
            if (response.success && response.data != null) {
                val newMessages = response.data.messages
                
                // 3. Insertar en Room
                newMessages.forEach { apiMessage ->
                    val localMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        content = apiMessage.content,
                        senderType = SenderType.valueOf(apiMessage.senderType),
                        senderId = apiMessage.senderId,
                        senderName = apiMessage.senderName ?: "Soporte",
                        syncStatus = SyncStatus.SENT,
                        readAt = apiMessage.readAt?.let { parseIso8601(it) },
                        createdAt = parseIso8601(apiMessage.createdAt),
                        serverId = apiMessage.id,
                        isPredefinedResponse = apiMessage.isPredefinedResponse
                    )
                    
                    // 🔴 CLAVE: Este INSERT dispara el LiveData
                    chatMessageDao.insertMessage(localMessage)
                }
                
                // 4. Actualizar unread_count
                if (newMessages.isNotEmpty()) {
                    val unreadCount = newMessages.count { 
                        it.senderType == "ANALISTA" && it.readAt == null 
                    }
                    conversationDao.updateUnreadCount(conversationId, unreadCount)
                }
                
                Result.Success(newMessages.size)
            } else {
                Result.Error(response.message ?: "Error al obtener mensajes")
            }
        } catch (e: Exception) {
            Result.NetworkError
        }
    }
}
```

**¿Cómo funciona?**
- `chatMessageDao.insertMessage()` inserta en Room
- Room dispara automáticamente el LiveData `getTodayMessagesLive()`
- ChatViewModel recibe el cambio y notifica a ChatFragment
- RecyclerView se actualiza con el nuevo mensaje

---

### 4. **ChatSyncWorker.kt** - Polling Cada 15 Segundos

```kotlin
class ChatSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting chat sync...")
        
        // 1. Obtener operador de la sesión
        val operatorCode = sessionManager.getOperatorCode() ?: return Result.success()
        
        // 2. Obtener conversación
        val conversation = chatRepository.getOrCreateConversation(operatorCode)
        
        // 3. Reintentar mensajes pendientes (FAILED)
        chatRepository.retryPendingMessages(conversation.id, operatorCode)
        
        // 4. Obtener mensajes nuevos del servidor
        val fetchResult = chatRepository.fetchNewMessages(conversation.id, operatorCode)
        // ↑ Esto inserta en Room → LiveData notifica → UI actualiza
        
        when (fetchResult) {
            is Result.Success -> {
                Log.d(TAG, "Sync completed: ${fetchResult.data} new messages")
                return Result.success()
            }
            else -> return Result.retry()
        }
    }
}
```

**Configuración en ControlOperadorApp.kt:**

```kotlin
val chatSyncRequest = PeriodicWorkRequestBuilder<ChatSyncWorker>(
    repeatInterval = 15,  // Mínimo permitido por Android
    repeatIntervalTimeUnit = TimeUnit.SECONDS
)
.setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // Solo con internet
        .build()
)
.build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    ChatSyncWorker.WORK_NAME,
    ExistingPeriodicWorkPolicy.KEEP,  // No duplicar si ya existe
    chatSyncRequest
)
```

**¿Cómo funciona?**
- WorkManager ejecuta cada **15 segundos** (mínimo Android)
- Solo si hay **conexión a internet**
- Descarga mensajes nuevos → Inserta en Room → LiveData notifica → UI actualiza
- Funciona **incluso cuando el usuario está en otra pantalla**

---

### 5. **ChatMessageDao.kt** - LiveData desde Room

```kotlin
@Dao
interface ChatMessageDao {
    
    /**
     * 🔴 CLAVE: Este método retorna LiveData que se actualiza automáticamente
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
        ORDER BY created_at ASC
    """)
    fun getTodayMessagesLive(conversationId: String): LiveData<List<ChatMessage>>
    
    /**
     * Conteo de mensajes no leídos (LiveData)
     */
    @Query("""
        SELECT COUNT(*) FROM chat_messages 
        WHERE conversation_id = :conversationId 
        AND sender_type = 'ANALISTA' 
        AND read_at IS NULL
    """)
    fun getUnreadCountLive(conversationId: String): LiveData<Int>
    
    /**
     * Insertar mensaje (dispara actualización de LiveData)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
}
```

**¿Cómo funciona?**
- Room observa cambios en la tabla `chat_messages`
- Cuando hay un `INSERT`, `UPDATE` o `DELETE`, Room notifica al LiveData
- No necesitas código extra para actualizar la UI
- Es **reactivo y eficiente**

---

## 📱 En Resumen: ¿Cómo se Cargan los Mensajes?

### Escenario 1: Operador Envía Mensaje
```
Usuario escribe → ViewModel → Repository → Room (PENDING)
                                        ↓
                                   LiveData notifica
                                        ↓
                                 UI muestra mensaje (⏳)
                                        ↓
                                   API POST /send
                                        ↓
                          Room actualiza (SENT) → LiveData notifica
                                        ↓
                                  UI cambia icono (✓)
```

### Escenario 2: Analista Envía Mensaje
```
Analista → API POST → SQL Server guarda mensaje
                            ↓
        (Después de máximo 15 segundos)
                            ↓
                    ChatSyncWorker ejecuta
                            ↓
                    GET /messages/today
                            ↓
                    Room inserta mensaje
                            ↓
                    LiveData notifica
                            ↓
                RecyclerView actualiza automáticamente
                            ↓
            Usuario ve mensaje nuevo (sin recargar)
```

### Escenario 3: Usuario Abre el Chat
```
ChatFragment.onResume()
        ↓
viewModel.syncMessagesNow()  ← NUEVO
        ↓
GET /messages/today (inmediato)
        ↓
Room inserta mensajes nuevos
        ↓
LiveData notifica
        ↓
RecyclerView actualizado al instante
```

---

## ✅ Ventajas de Esta Arquitectura

1. **Offline-First**: Mensajes se guardan primero localmente
2. **Reactivo**: UI se actualiza automáticamente sin código manual
3. **Eficiente**: Solo sincroniza mensajes nuevos (`last_id`)
4. **Background Sync**: WorkManager sincroniza incluso fuera del chat
5. **Inmediato**: `syncMessagesNow()` descarga al abrir el chat
6. **Resiliente**: Mensajes PENDING se reintentan automáticamente

---

## 🧪 Cómo Probar

### Test 1: Operador Envía Mensaje
```
1. Abrir ChatFragment
2. Escribir mensaje "Hola prueba"
3. Enviar
4. Verificar icono cambia: ⏳ → ✓
5. Mensaje aparece inmediatamente en RecyclerView
```

### Test 2: Analista Responde (Postman)
```bash
# Enviar mensaje como analista
curl -X POST http://localhost:8000/api/chat/send \
-H "Content-Type: application/json" \
-d '{
  "operator_code": "12345",
  "content": "Hola operador",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}'

# Esperar máximo 15 segundos
# Verificar mensaje aparece en app automáticamente
```

### Test 3: Sync Manual al Abrir Chat
```
1. Estar en HomeFragment
2. Enviar mensaje como analista (Postman)
3. Abrir ChatFragment inmediatamente
4. Mensaje aparece AL INSTANTE (no espera 15s)
5. Esto es gracias a syncMessagesNow() en onResume()
```

---

## 🔍 Logs para Debugging

```bash
# Ver sincronización automática
adb logcat | grep ChatSyncWorker

# Ver carga de mensajes
adb logcat | grep ChatFragment

# Ver operaciones de Room
adb logcat | grep ChatRepository

# Ver todo el flujo
adb logcat | grep -E "(ChatFragment|ChatSyncWorker|ChatRepository)"
```

---

## 📊 Timing de Sincronización

| Evento | Tiempo de Actualización | Mecanismo |
|--------|------------------------|-----------|
| Operador envía mensaje | **Inmediato** | LiveData desde Room |
| Analista envía (WorkManager) | **Máximo 15 segundos** | ChatSyncWorker |
| Analista envía (Usuario en chat) | **Inmediato** | syncMessagesNow() |
| Mensaje PENDING reintento | **15 segundos** | ChatSyncWorker |
| Badge "sin leer" actualiza | **Inmediato** | LiveData desde Room |

---

## 🎉 Conclusión

Los mensajes **SE CARGAN AUTOMÁTICAMENTE** gracias a:

1. ✅ **LiveData Reactivo** - Room notifica cambios automáticamente
2. ✅ **ChatSyncWorker** - Polling cada 15 segundos en background
3. ✅ **syncMessagesNow()** - Descarga inmediata al abrir el chat

**No necesitas código manual** para actualizar el RecyclerView. Todo es **automático y reactivo**. 🚀

---

**Última actualización**: 4 de Noviembre de 2025
