# 📱 Guía Completa de Testing - Sistema de Chat Operador ↔ Analistas

**Proyecto**: ControlOperador  
**Fecha**: 4 de Noviembre de 2025  
**Versión**: 1.0  
**Autor**: Equipo de Desarrollo

---

## 📋 Tabla de Contenidos

1. [Resumen del Sistema](#resumen-del-sistema)
2. [Arquitectura Técnica](#arquitectura-técnica)
3. [Cómo Funciona el Chat](#cómo-funciona-el-chat)
4. [Configuración Previa al Testing](#configuración-previa-al-testing)
5. [Flujos de Testing](#flujos-de-testing)
6. [Testing Manual con Postman](#testing-manual-con-postman)
7. [Testing de la App Android](#testing-de-la-app-android)
8. [Verificación de Base de Datos](#verificación-de-base-de-datos)
9. [Troubleshooting](#troubleshooting)

---

## 1. Resumen del Sistema

### 🎯 Objetivo
Sistema de chat bidireccional en tiempo real que permite:
- **Operadores** (app móvil Android) se comunican con **Analistas** (panel web)
- Sincronización automática cada 15 segundos
- Estados de mensaje: Enviando → Enviado → Leído
- Respuestas predefinidas dinámicas
- Persistencia local con Room Database
- Limpieza automática de mensajes >30 días

### 👥 Actores del Sistema

#### Operador (Usuario Móvil)
- **Identificación**: `clave_operador` (5 dígitos) de tabla `mt_operadores`
- **App**: Android (Kotlin + Jetpack)
- **Funciones**:
  - Enviar mensajes de texto
  - Recibir respuestas de analistas
  - Ver mensajes del día actual
  - Usar respuestas predefinidas
  - Ver estados de mensaje

#### Analista (Usuario Web)
- **Identificación**: `users.id` de tabla `users`
- **Panel**: Web (futuro - actualmente se prueba con API directa)
- **Funciones**:
  - Ver conversaciones de operadores
  - Responder mensajes
  - Enviar respuestas predefinidas
  - Marcar como leído

### 📊 Reglas de Negocio

1. **Una conversación por operador**: Cada operador tiene una única conversación activa
2. **Múltiples analistas**: Varios analistas pueden responder al mismo operador
3. **Privacidad**: El operador NO ve qué analista específico responde (todos aparecen como "Soporte")
4. **Solo mensajes del día**: La app solo carga mensajes del día actual
5. **Persistencia 30 días**: Mensajes se eliminan automáticamente después de 30 días
6. **Sincronización automática**: Cada 15 segundos (mínimo permitido por Android WorkManager)

---

## 2. Arquitectura Técnica

### 🏗️ Stack Tecnológico

#### Backend (Laravel 7 + SQL Server)
```
Laravel 7.x
├── SQL Server (transportistas2023)
├── Eloquent ORM
├── API RESTful
└── Comandos Artisan
```

#### Frontend Android (Kotlin)
```
Android App (Kotlin)
├── Room Database (SQLite local)
├── Retrofit 2.9.0 (HTTP client)
├── WorkManager 2.9.0 (Background tasks)
├── LiveData + ViewModel (MVVM)
├── Navigation Component
└── Material Design 3
```

### 🗄️ Estructura de Base de Datos

#### Backend - SQL Server

**Tabla: `conversations`**
```sql
CREATE TABLE conversations (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    operator_code NVARCHAR(10) UNIQUE NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    last_message_at DATETIME2,
    unread_count INT DEFAULT 0,
    
    FOREIGN KEY (operator_code) REFERENCES mt_operadores(clave_operador)
);

CREATE INDEX idx_conversations_operator ON conversations(operator_code);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC);
```

**Tabla: `messages`**
```sql
CREATE TABLE messages (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    conversation_id UNIQUEIDENTIFIER NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    sender_type NVARCHAR(20) NOT NULL, -- 'OPERADOR' o 'ANALISTA'
    sender_id NVARCHAR(50) NOT NULL,   -- clave_operador o users.id
    sender_name NVARCHAR(100),
    sync_status NVARCHAR(20) DEFAULT 'SENT', -- 'PENDING', 'SENT', 'FAILED'
    read_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    is_predefined_response BIT DEFAULT 0,
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_sender ON messages(sender_type, sender_id);
CREATE INDEX idx_messages_today ON messages(CAST(created_at AS DATE));
```

**Tabla: `predefined_responses`**
```sql
CREATE TABLE predefined_responses (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    mensaje NVARCHAR(500) NOT NULL,
    categoria NVARCHAR(100),
    orden INT DEFAULT 0,
    activo BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE INDEX idx_predefined_responses_active ON predefined_responses(activo, orden);
```

#### Android - Room Database (SQLite)

**Entity: `Conversation`**
```kotlin
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "operator_code") val operatorCode: String,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Date? = null,
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0
)
```

**Entity: `ChatMessage`**
```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    val content: String,
    @ColumnInfo(name = "sender_type") val senderType: SenderType,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "sender_name") val senderName: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "read_at") val readAt: Date? = null,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "is_predefined_response") val isPredefinedResponse: Boolean = false
)

enum class SenderType { OPERADOR, ANALISTA }
enum class SyncStatus { PENDING, SENT, FAILED }
```

### 🔄 Flujo de Sincronización

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE SINCRONIZACIÓN                      │
└─────────────────────────────────────────────────────────────────┘

OPERADOR ENVÍA MENSAJE:
1. Usuario escribe mensaje en app
2. ChatViewModel.sendMessage(content)
3. ChatRepository guarda en Room con status PENDING
4. ChatRepository.sendMessage() llama API POST /chat/send
5. Backend guarda en SQL Server
6. Backend retorna server_id
7. ChatRepository actualiza mensaje local: PENDING → SENT + server_id
8. LiveData notifica UI → icono cambia ⏳ → ✓

ANALISTA RESPONDE (Vía API directa por ahora):
1. POST /chat/send con sender_type: "ANALISTA"
2. Backend guarda en SQL Server
3. Backend actualiza last_message_at en conversation
4. Backend incrementa unread_count

OPERADOR RECIBE MENSAJE (Automático cada 15s):
1. ChatSyncWorker se ejecuta cada 15 segundos
2. ChatRepository.fetchNewMessages()
3. GET /chat/messages/today?operator_code=12345&last_id=abc
4. Backend retorna mensajes nuevos
5. ChatRepository inserta en Room
6. LiveData notifica UI → mensaje aparece en RecyclerView
7. ChatFragment.onResume() → markAllMessagesAsRead()
8. POST /chat/mark-read → Backend actualiza read_at
9. UI actualiza icono ✓ → ✓✓
```

---

## 3. Cómo Funciona el Chat

### 📱 Componentes Android

#### 1. **ChatFragment.kt** - Pantalla Principal
```kotlin
Funciones:
✓ Muestra conversación del día (RecyclerView)
✓ Observa todayMessages LiveData (auto-actualización)
✓ Envío de mensajes de texto
✓ Bottom sheet con respuestas predefinidas
✓ Marca mensajes como leídos al abrir
✓ Estados visuales: ⏳ Enviando, ✓ Enviado, ✓✓ Leído, ❌ Error
```

#### 2. **ChatViewModel.kt** - Lógica de Negocio
```kotlin
Funciones:
✓ initializeChat(operatorCode) - Inicializa conversación
✓ sendMessage(content) - Envía mensaje de texto
✓ sendPredefinedResponse(response) - Envía respuesta predefinida
✓ loadPredefinedResponses() - Carga respuestas del servidor
✓ markAllMessagesAsRead() - Marca todos como leídos
✓ retryPendingMessages() - Reintenta mensajes fallidos

LiveData:
- todayMessages: List<ChatMessage> (auto-actualización)
- unreadCount: Int (contador dinámico)
- predefinedResponses: List<PredefinedResponse>
- sendMessageState: SendMessageState (Idle/Sending/Success/Error)
```

#### 3. **ChatRepository.kt** - Capa de Datos
```kotlin
Funciones principales:
✓ sendMessage() - Guarda local (PENDING) → API → Actualiza (SENT/FAILED)
✓ fetchNewMessages() - GET mensajes nuevos desde servidor
✓ retryPendingMessages() - Reintenta mensajes PENDING
✓ markMessagesAsRead() - Marca como leído (local + servidor)
✓ cleanOldMessages() - Elimina mensajes >30 días
✓ getPredefinedResponses() - Obtiene respuestas dinámicas

Estrategia Offline-First:
1. Guarda primero en Room (disponible inmediatamente)
2. Sincroniza con servidor en background
3. Actualiza estado según respuesta del servidor
```

#### 4. **ChatSyncWorker.kt** - Sincronización Automática
```kotlin
Periodicidad: 15 segundos (mínimo Android WorkManager)
Restricciones:
✓ Requiere conexión a internet (NetworkType.CONNECTED)
✓ Solo cuando app está en foreground

Flujo:
1. Verifica sesión activa
2. Reintenta mensajes PENDING
3. Obtiene mensajes nuevos del servidor
4. Inserta en Room
5. LiveData notifica UI
```

#### 5. **CleanupChatWorker.kt** - Limpieza Automática
```kotlin
Periodicidad: 24 horas (diario a las 2 AM)

Flujo:
1. Calcula fecha límite (hoy - 30 días)
2. Elimina mensajes WHERE created_at < fecha_limite
3. Libera espacio en SQLite
```

#### 6. **ChatAdapter.kt** - Vista de Mensajes
```kotlin
Funciona con DiffUtil para actualizaciones eficientes

ViewTypes:
- VIEW_TYPE_SENT (Operador): Burbuja azul alineada a la derecha
- VIEW_TYPE_RECEIVED (Analista): Burbuja blanca alineada a la izquierda

Estados visuales:
⏳ PENDING - "Enviando..."
✓ SENT - "Enviado"
✓✓ read_at != null - "Leído"
❌ FAILED - "Error de envío"
```

### 🌐 Endpoints API (Backend Laravel)

#### 1. **POST /api/chat/send** - Enviar Mensaje
```http
POST http://tu-dominio.com/api/chat/send
Content-Type: application/json

{
  "operator_code": "12345",
  "content": "Hola, necesito asistencia",
  "sender_type": "OPERADOR",
  "sender_id": "12345",
  "is_predefined_response": false
}

Response 200 OK:
{
  "success": true,
  "message": "Mensaje enviado correctamente",
  "data": {
    "message_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "conversation_id": "conv-uuid",
    "created_at": "2025-11-04T10:30:00Z"
  }
}
```

#### 2. **GET /api/chat/messages/today** - Obtener Mensajes del Día
```http
GET http://tu-dominio.com/api/chat/messages/today?operator_code=12345&last_id=abc123

Response 200 OK:
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "msg-uuid-1",
        "conversation_id": "conv-uuid",
        "content": "¿Cuál es tu ubicación actual?",
        "sender_type": "ANALISTA",
        "sender_id": "1",
        "sender_name": "Soporte",
        "read_at": null,
        "created_at": "2025-11-04T10:25:00Z",
        "is_predefined_response": false
      },
      {
        "id": "msg-uuid-2",
        "conversation_id": "conv-uuid",
        "content": "Estoy en Av. Principal 123",
        "sender_type": "OPERADOR",
        "sender_id": "12345",
        "sender_name": null,
        "read_at": "2025-11-04T10:26:00Z",
        "created_at": "2025-11-04T10:25:30Z",
        "is_predefined_response": false
      }
    ],
    "has_more": false,
    "total": 2
  }
}
```

#### 3. **POST /api/chat/mark-read** - Marcar como Leído
```http
POST http://tu-dominio.com/api/chat/mark-read
Content-Type: application/json

{
  "operator_code": "12345",
  "message_ids": ["msg-uuid-1", "msg-uuid-2"]
}

Response 200 OK:
{
  "success": true,
  "message": "Mensajes marcados como leídos",
  "data": {
    "updated_count": 2
  }
}
```

#### 4. **GET /api/chat/predefined-responses** - Respuestas Predefinidas
```http
GET http://tu-dominio.com/api/chat/predefined-responses

Response 200 OK:
{
  "success": true,
  "data": {
    "responses": [
      {
        "id": "resp-uuid-1",
        "mensaje": "Estoy en mi ruta habitual",
        "categoria": "Ubicación",
        "orden": 1,
        "activo": true
      },
      {
        "id": "resp-uuid-2",
        "mensaje": "Todo en orden",
        "categoria": "Estado",
        "orden": 2,
        "activo": true
      }
    ]
  }
}
```

---

## 4. Configuración Previa al Testing

### ✅ Checklist Backend

#### 1. Base de Datos SQL Server
```sql
-- Verificar tablas existen
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME IN ('conversations', 'messages', 'predefined_responses');

-- Insertar respuestas predefinidas de prueba
INSERT INTO predefined_responses (id, mensaje, categoria, orden, activo) VALUES
(NEWID(), 'Estoy en mi ruta habitual', 'Ubicación', 1, 1),
(NEWID(), 'Todo en orden', 'Estado', 2, 1),
(NEWID(), 'Necesito asistencia', 'Urgente', 3, 1),
(NEWID(), 'Tráfico detenido', 'Tráfico', 4, 1),
(NEWID(), 'Llegando a destino', 'Ubicación', 5, 1);
```

#### 2. Configurar BASE_URL en App Android
```kotlin
// Archivo: app/build.gradle.kts
android {
    defaultConfig {
        // Para emulador Android
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
        
        // Para dispositivo físico (usar IP de tu computadora)
        // buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8000/api/\"")
        
        // Para producción
        // buildConfigField("String", "API_BASE_URL", "\"https://tu-dominio.com/api/\"")
    }
}

// Luego en RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL
    // ...
}
```

#### 3. Verificar Operador de Prueba Existe
```sql
-- Verificar operador existe en mt_operadores
SELECT clave_operador, nombre 
FROM mt_operadores 
WHERE clave_operador = '12345';

-- Si no existe, crear uno de prueba
INSERT INTO mt_operadores (clave_operador, nombre, activo) 
VALUES ('12345', 'Operador Prueba', 1);
```

#### 4. Verificar Usuario Analista Existe
```sql
-- Verificar usuario analista existe
SELECT id, name, email 
FROM users 
WHERE id = 1;

-- Si no existe, crear uno
INSERT INTO users (id, name, email, password) 
VALUES (1, 'Analista Prueba', 'analista@test.com', 'hashed_password');
```

---

## 5. Flujos de Testing

### 🧪 Test 1: Operador Envía Mensaje

**Objetivo**: Verificar que mensaje del operador llega al backend

**Pasos**:

1. **En App Android**:
   ```
   a. Iniciar sesión con operador 12345
   b. Ir a pantalla "Chat"
   c. Escribir mensaje: "Hola, prueba de mensaje"
   d. Presionar enviar
   e. Verificar estado: ⏳ Enviando → ✓ Enviado
   ```

2. **En Backend SQL Server**:
   ```sql
   -- Verificar conversación creada
   SELECT * FROM conversations WHERE operator_code = '12345';
   
   -- Verificar mensaje guardado
   SELECT TOP 1 
       id, content, sender_type, sender_id, created_at, sync_status
   FROM messages 
   WHERE conversation_id = (SELECT id FROM conversations WHERE operator_code = '12345')
   ORDER BY created_at DESC;
   ```

**Resultado Esperado**:
- ✅ Mensaje aparece en tabla `messages`
- ✅ `sender_type` = 'OPERADOR'
- ✅ `sender_id` = '12345'
- ✅ `sync_status` = 'SENT'
- ✅ `created_at` = fecha/hora actual

---

### 🧪 Test 2: Analista Responde (Vía API Manual)

**Objetivo**: Simular respuesta de analista y verificar que operador la recibe

**Pasos**:

1. **Obtener conversation_id**:
   ```sql
   SELECT id FROM conversations WHERE operator_code = '12345';
   -- Ejemplo: a1b2c3d4-e5f6-7890-abcd-ef1234567890
   ```

2. **Con Postman/cURL - Enviar respuesta de analista**:
   ```bash
   curl -X POST http://localhost:8000/api/chat/send \
   -H "Content-Type: application/json" \
   -d '{
     "operator_code": "12345",
     "content": "Hola operador, ¿en qué puedo ayudarte?",
     "sender_type": "ANALISTA",
     "sender_id": "1",
     "is_predefined_response": false
   }'
   ```

3. **En Backend SQL Server**:
   ```sql
   -- Verificar mensaje analista guardado
   SELECT TOP 1 
       content, sender_type, sender_name, created_at
   FROM messages 
   WHERE conversation_id = (SELECT id FROM conversations WHERE operator_code = '12345')
     AND sender_type = 'ANALISTA'
   ORDER BY created_at DESC;
   ```

4. **En App Android**:
   ```
   a. Esperar 15 segundos (sync automático)
      O forzar: Cerrar app y volver a abrir
   b. Verificar mensaje del analista aparece
   c. Verificar badge "1 sin leer" en HomeFragment
   d. Abrir ChatFragment
   e. Verificar badge desaparece
   f. Verificar mensaje marcado como leído (✓✓)
   ```

**Resultado Esperado**:
- ✅ Mensaje analista en SQL Server
- ✅ Mensaje sincronizado en Room (app)
- ✅ Badge "1 sin leer" apareció
- ✅ Badge desapareció al abrir chat
- ✅ Mensaje marcado como leído en servidor

---

### 🧪 Test 3: Respuestas Predefinidas

**Objetivo**: Verificar que respuestas predefinidas se cargan y envían correctamente

**Pasos**:

1. **Verificar respuestas en Backend**:
   ```sql
   SELECT id, mensaje, categoria, orden 
   FROM predefined_responses 
   WHERE activo = 1
   ORDER BY orden;
   ```

2. **En App Android**:
   ```
   a. Ir a ChatFragment
   b. Presionar botón "📝 Respuesta Rápida" (portrait)
      O ver botones laterales (landscape)
   c. Verificar bottom sheet muestra respuestas del servidor
   d. Seleccionar "Todo en orden"
   e. Verificar mensaje se envía con is_predefined_response = true
   ```

3. **Verificar en Backend**:
   ```sql
   SELECT TOP 1 
       content, is_predefined_response, created_at
   FROM messages 
   WHERE conversation_id = (SELECT id FROM conversations WHERE operator_code = '12345')
     AND is_predefined_response = 1
   ORDER BY created_at DESC;
   ```

**Resultado Esperado**:
- ✅ Bottom sheet muestra 5 respuestas predefinidas
- ✅ Mensaje enviado con `is_predefined_response` = 1
- ✅ Contenido coincide con respuesta seleccionada

---

### 🧪 Test 4: Estados de Mensaje

**Objetivo**: Verificar transiciones de estado ⏳ → ✓ → ✓✓

**Pasos**:

1. **Modo Avión - Simular offline**:
   ```
   a. Activar modo avión en dispositivo
   b. En ChatFragment, enviar mensaje: "Mensaje offline"
   c. Verificar icono ⏳ (PENDING)
   d. Desactivar modo avión
   e. Esperar 15 segundos (sync worker)
   f. Verificar icono cambia a ✓ (SENT)
   ```

2. **Verificar en Room Database**:
   ```bash
   adb shell
   cd /data/data/com.example.controloperador/databases/
   sqlite3 controloperador_database
   
   SELECT id, content, sync_status, created_at 
   FROM chat_messages 
   WHERE content = 'Mensaje offline';
   ```

3. **Simular Lectura**:
   ```
   a. Enviar mensaje desde app
   b. Verificar estado ✓ (SENT)
   c. Simular analista marca como leído (Postman):
   
   POST http://localhost:8000/api/chat/mark-read
   {
     "operator_code": "12345",
     "message_ids": ["msg-uuid-del-mensaje"]
   }
   
   d. Esperar 15 segundos (sync)
   e. Verificar icono cambia a ✓✓ (LEÍDO)
   ```

**Resultado Esperado**:
- ✅ Offline: Estado PENDING con icono ⏳
- ✅ Online: Auto-retry, estado SENT con icono ✓
- ✅ Leído: Estado con read_at != null, icono ✓✓

---

### 🧪 Test 5: Sincronización Automática (15 segundos)

**Objetivo**: Verificar WorkManager ejecuta polling cada 15 segundos

**Pasos**:

1. **Ver logs de WorkManager**:
   ```bash
   adb logcat | grep ChatSyncWorker
   ```

2. **Enviar múltiples mensajes desde backend** (simular analista):
   ```bash
   # Mensaje 1
   curl -X POST http://localhost:8000/api/chat/send \
   -H "Content-Type: application/json" \
   -d '{"operator_code":"12345","content":"Mensaje 1","sender_type":"ANALISTA","sender_id":"1"}'
   
   # Esperar 5 segundos
   
   # Mensaje 2
   curl -X POST http://localhost:8000/api/chat/send \
   -H "Content-Type: application/json" \
   -d '{"operator_code":"12345","content":"Mensaje 2","sender_type":"ANALISTA","sender_id":"1"}'
   ```

3. **Observar en App**:
   ```
   a. Mantener ChatFragment abierto
   b. Esperar máximo 15 segundos
   c. Verificar ambos mensajes aparecen automáticamente
   d. NO es necesario recargar manualmente
   ```

4. **Verificar en Logcat**:
   ```
   D/ChatSyncWorker: Sync started
   D/ChatSyncWorker: Fetching new messages for operator 12345
   D/ChatSyncWorker: Fetched 2 new messages
   D/ChatSyncWorker: Sync completed successfully
   ```

**Resultado Esperado**:
- ✅ Mensajes aparecen en <15 segundos
- ✅ LiveData actualiza RecyclerView automáticamente
- ✅ Badge "2 sin leer" se actualiza
- ✅ Logs muestran ejecución de sync

---

### 🧪 Test 6: Limpieza de Mensajes Antiguos (30 días)

**Objetivo**: Verificar CleanupChatWorker elimina mensajes >30 días

**Pasos**:

1. **Insertar mensaje antiguo en SQL Server**:
   ```sql
   INSERT INTO messages (
       id, conversation_id, content, sender_type, sender_id, 
       created_at, sync_status
   )
   VALUES (
       NEWID(),
       (SELECT id FROM conversations WHERE operator_code = '12345'),
       'Mensaje antiguo de prueba',
       'ANALISTA',
       '1',
       DATEADD(DAY, -35, GETDATE()), -- 35 días atrás
       'SENT'
   );
   ```

2. **Forzar limpieza manual** (O esperar 24 horas):
   ```bash
   # En backend Laravel
   php artisan chat:cleanup --days=30
   
   # Ver resultado
   SELECT COUNT(*) 
   FROM messages 
   WHERE created_at < DATEADD(DAY, -30, GETDATE());
   -- Debe retornar 0
   ```

3. **En App Android - Forzar cleanup**:
   ```kotlin
   // En código temporal o debug
   val workManager = WorkManager.getInstance(context)
   val cleanupRequest = OneTimeWorkRequestBuilder<CleanupChatWorker>().build()
   workManager.enqueue(cleanupRequest)
   ```

4. **Verificar en Room**:
   ```bash
   adb shell
   sqlite3 /data/data/com.example.controloperador/databases/controloperador_database
   
   SELECT COUNT(*) 
   FROM chat_messages 
   WHERE created_at < datetime('now', '-30 days');
   -- Debe retornar 0
   ```

**Resultado Esperado**:
- ✅ Mensajes >30 días eliminados de SQL Server
- ✅ Mensajes >30 días eliminados de Room
- ✅ Solo mensajes recientes persisten

---

## 6. Testing Manual con Postman

### 📮 Colección Postman

#### Configurar Variables de Entorno
```json
{
  "base_url": "http://localhost:8000/api",
  "operator_code": "12345",
  "analista_user_id": "1"
}
```

#### Request 1: Enviar Mensaje Operador
```http
POST {{base_url}}/chat/send
Content-Type: application/json

{
  "operator_code": "{{operator_code}}",
  "content": "Prueba desde Postman - Operador",
  "sender_type": "OPERADOR",
  "sender_id": "{{operator_code}}",
  "is_predefined_response": false
}

Expected: 200 OK con message_id
```

#### Request 2: Enviar Mensaje Analista
```http
POST {{base_url}}/chat/send
Content-Type: application/json

{
  "operator_code": "{{operator_code}}",
  "content": "Respuesta desde Postman - Analista",
  "sender_type": "ANALISTA",
  "sender_id": "{{analista_user_id}}",
  "is_predefined_response": false
}

Expected: 200 OK con message_id
```

#### Request 3: Obtener Mensajes del Día
```http
GET {{base_url}}/chat/messages/today?operator_code={{operator_code}}

Expected: 200 OK con array de mensajes
```

#### Request 4: Obtener Mensajes Nuevos (Paginación)
```http
GET {{base_url}}/chat/messages/today?operator_code={{operator_code}}&last_id=abc123

Expected: 200 OK con mensajes posteriores a last_id
```

#### Request 5: Marcar Mensajes como Leídos
```http
POST {{base_url}}/chat/mark-read
Content-Type: application/json

{
  "operator_code": "{{operator_code}}",
  "message_ids": ["msg-uuid-1", "msg-uuid-2"]
}

Expected: 200 OK con updated_count
```

#### Request 6: Obtener Respuestas Predefinidas
```http
GET {{base_url}}/chat/predefined-responses

Expected: 200 OK con array de respuestas
```

---

## 7. Testing de la App Android

### 📱 Testing en Emulador

#### 1. Configurar IP del Backend
```kotlin
// Si backend corre en localhost:8000
// El emulador usa 10.0.2.2 para acceder a localhost de la máquina host

// RetrofitClient.kt o build.gradle.kts
const val BASE_URL = "http://10.0.2.2:8000/api/"
```

#### 2. Instalar y Ejecutar
```bash
# Compilar e instalar
./gradlew installDebug

# Ver logs en tiempo real
adb logcat | grep -E "(ChatFragment|ChatViewModel|ChatRepository|ChatSyncWorker)"
```

#### 3. Flujo Completo de Testing
```
1. Iniciar app
2. Login con operador 12345
3. Ir a HomeFragment
   ✓ Verificar badge "0 sin leer" (o número actual)
4. Ir a ChatFragment
   ✓ Verificar carga mensajes del día
   ✓ Verificar scroll al último mensaje
5. Enviar mensaje "Hola desde app"
   ✓ Verificar icono ⏳ → ✓
6. Abrir Postman, enviar mensaje de analista
7. Esperar 15 segundos en app
   ✓ Verificar mensaje analista aparece
   ✓ Verificar badge actualizado
8. Presionar "Respuesta Rápida"
   ✓ Verificar bottom sheet con opciones
9. Seleccionar "Todo en orden"
   ✓ Verificar mensaje enviado
10. Volver a HomeFragment
    ✓ Verificar preview de último mensaje
```

### 📱 Testing en Dispositivo Físico

#### 1. Configurar IP de la Red
```kotlin
// Obtener IP de tu computadora en la red local
// Windows: ipconfig
// Mac/Linux: ifconfig | grep "inet "
// Ejemplo: 192.168.1.100

const val BASE_URL = "http://192.168.1.100:8000/api/"
```

#### 2. Asegurar Backend Accesible
```bash
# En Laravel, usar --host para exponer en red local
php artisan serve --host=0.0.0.0 --port=8000

# Verificar desde navegador del dispositivo
http://192.168.1.100:8000/api/health
```

#### 3. Instalar en Dispositivo
```bash
# Conectar dispositivo por USB con depuración activada
adb devices

# Instalar
./gradlew installDebug

# Ver logs
adb logcat | grep ControlOperador
```

---

## 8. Verificación de Base de Datos

### 🗄️ SQL Server (Backend)

#### Query 1: Ver Todas las Conversaciones
```sql
SELECT 
    c.operator_code,
    c.created_at,
    c.last_message_at,
    c.unread_count,
    COUNT(m.id) as total_messages
FROM conversations c
LEFT JOIN messages m ON m.conversation_id = c.id
GROUP BY c.operator_code, c.created_at, c.last_message_at, c.unread_count
ORDER BY c.last_message_at DESC;
```

#### Query 2: Ver Mensajes de una Conversación
```sql
DECLARE @operator_code NVARCHAR(10) = '12345';

SELECT 
    m.content,
    m.sender_type,
    m.sender_id,
    m.sender_name,
    m.sync_status,
    m.read_at,
    m.created_at,
    m.is_predefined_response
FROM messages m
INNER JOIN conversations c ON c.id = m.conversation_id
WHERE c.operator_code = @operator_code
  AND CAST(m.created_at AS DATE) = CAST(GETDATE() AS DATE)
ORDER BY m.created_at ASC;
```

#### Query 3: Estadísticas de Mensajes
```sql
SELECT 
    sender_type,
    COUNT(*) as total,
    SUM(CASE WHEN read_at IS NOT NULL THEN 1 ELSE 0 END) as leidos,
    SUM(CASE WHEN read_at IS NULL THEN 1 ELSE 0 END) as no_leidos
FROM messages
WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)
GROUP BY sender_type;
```

### 📱 Room Database (Android)

#### Acceder a SQLite
```bash
# Conectar a shell del dispositivo
adb shell

# Navegar a base de datos
cd /data/data/com.example.controloperador/databases/

# Abrir SQLite
sqlite3 controloperador_database

# Listar tablas
.tables
# Resultado: attendance_logs  chat_messages  conversations
```

#### Query 1: Ver Conversaciones
```sql
.mode column
.headers on

SELECT 
    operator_code,
    datetime(created_at/1000, 'unixepoch') as created_at,
    datetime(last_message_at/1000, 'unixepoch') as last_message_at,
    unread_count
FROM conversations;
```

#### Query 2: Ver Mensajes de Hoy
```sql
SELECT 
    content,
    sender_type,
    sync_status,
    datetime(created_at/1000, 'unixepoch') as created_at,
    datetime(read_at/1000, 'unixepoch') as read_at
FROM chat_messages
WHERE DATE(created_at/1000, 'unixepoch') = DATE('now')
ORDER BY created_at ASC;
```

#### Query 3: Ver Mensajes PENDING
```sql
SELECT 
    id,
    content,
    sync_status,
    datetime(created_at/1000, 'unixepoch') as created_at
FROM chat_messages
WHERE sync_status = 'PENDING';
```

#### Query 4: Limpiar Base de Datos (Testing)
```sql
-- CUIDADO: Elimina TODOS los datos
DELETE FROM chat_messages;
DELETE FROM conversations;
VACUUM;
```

---

## 9. Troubleshooting

### ❌ Problema 1: Mensajes No Sincroniza

**Síntomas**:
- Envío mensaje desde app, no llega a backend
- O envío desde Postman, no aparece en app

**Diagnóstico**:

1. **Verificar conexión de red**:
   ```bash
   adb shell ping -c 3 8.8.8.8
   ```

2. **Verificar BASE_URL correcta**:
   ```kotlin
   // En RetrofitClient.kt
   Log.d("Retrofit", "BASE_URL: $BASE_URL")
   ```

3. **Ver logs de Retrofit**:
   ```bash
   adb logcat | grep -E "(OkHttp|Retrofit)"
   ```

4. **Probar endpoint con cURL**:
   ```bash
   curl -v http://10.0.2.2:8000/api/chat/messages/today?operator_code=12345
   ```

**Soluciones**:
- ✅ Verificar firewall no bloquea puerto 8000
- ✅ Usar `0.0.0.0` en `php artisan serve --host=0.0.0.0`
- ✅ Verificar IP correcta (10.0.2.2 para emulador, IP real para dispositivo)
- ✅ Agregar permiso `INTERNET` en AndroidManifest.xml

---

### ❌ Problema 2: WorkManager No Ejecuta Sync

**Síntomas**:
- Mensajes no se sincronizan automáticamente
- Logs no muestran ejecución de ChatSyncWorker

**Diagnóstico**:

1. **Verificar WorkManager programado**:
   ```bash
   adb logcat | grep WorkManager
   ```

2. **Ver estado de Workers**:
   ```kotlin
   val workManager = WorkManager.getInstance(context)
   val workInfos = workManager.getWorkInfosForUniqueWork(ChatSyncWorker.WORK_NAME).get()
   workInfos.forEach { info ->
       Log.d("WorkManager", "State: ${info.state}, Run Attempt: ${info.runAttemptCount}")
   }
   ```

3. **Verificar restricciones**:
   ```kotlin
   // ChatSyncWorker requiere conexión a internet
   setConstraints(
       Constraints.Builder()
           .setRequiredNetworkType(NetworkType.CONNECTED)
           .build()
   )
   ```

**Soluciones**:
- ✅ Verificar app en foreground (WorkManager solo ejecuta en foreground para 15s)
- ✅ Verificar conexión a internet activa
- ✅ Reiniciar app si WorkManager no se programó
- ✅ Forzar ejecución manual:
  ```kotlin
  val workRequest = OneTimeWorkRequestBuilder<ChatSyncWorker>().build()
  WorkManager.getInstance(context).enqueue(workRequest)
  ```

---

### ❌ Problema 3: Estados de Mensaje No Cambian

**Síntomas**:
- Mensaje queda en ⏳ permanentemente
- No cambia a ✓ después de enviar

**Diagnóstico**:

1. **Verificar estado en Room**:
   ```bash
   adb shell
   sqlite3 /data/data/com.example.controloperador/databases/controloperador_database
   SELECT id, content, sync_status, server_id FROM chat_messages ORDER BY created_at DESC LIMIT 5;
   ```

2. **Ver logs de ChatRepository**:
   ```bash
   adb logcat | grep ChatRepository
   ```

3. **Verificar respuesta del servidor**:
   ```bash
   adb logcat | grep "POST /api/chat/send"
   ```

**Soluciones**:
- ✅ Si `sync_status` = PENDING: Verificar conexión y retry
- ✅ Si `server_id` = null: API no retornó ID, verificar backend
- ✅ Si FAILED: Ver logs de error, verificar formato de request
- ✅ Reintentar mensaje:
  ```kotlin
  chatViewModel.retryPendingMessages()
  ```

---

### ❌ Problema 4: Badge No Actualiza

**Síntomas**:
- Badge "X sin leer" no aparece
- O no desaparece al abrir chat

**Diagnóstico**:

1. **Verificar `unread_count` en conversación**:
   ```sql
   SELECT unread_count FROM conversations WHERE operator_code = '12345';
   ```

2. **Verificar observador en HomeFragment**:
   ```bash
   adb logcat | grep "HomeFragment"
   ```

3. **Verificar `read_at` en mensajes**:
   ```sql
   SELECT content, read_at FROM chat_messages 
   WHERE read_at IS NULL AND sender_type = 'ANALISTA';
   ```

**Soluciones**:
- ✅ Verificar `observeChatViewModel()` se llama en `onCreateView()`
- ✅ Verificar LiveData `unreadCount` observado correctamente
- ✅ Forzar marcar como leído:
  ```kotlin
  chatViewModel.markAllMessagesAsRead()
  ```
- ✅ Verificar backend actualiza `read_at` en POST /chat/mark-read

---

### ❌ Problema 5: Respuestas Predefinidas No Cargan

**Síntomas**:
- Bottom sheet vacío o no abre
- Toast "Cargando mensajes predeterminados..."

**Diagnóstico**:

1. **Verificar respuestas en backend**:
   ```sql
   SELECT * FROM predefined_responses WHERE activo = 1;
   ```

2. **Probar endpoint directamente**:
   ```bash
   curl http://localhost:8000/api/chat/predefined-responses
   ```

3. **Ver logs de ViewModel**:
   ```bash
   adb logcat | grep "ChatViewModel.*predefined"
   ```

**Soluciones**:
- ✅ Insertar respuestas de prueba en SQL Server (ver sección 4.1)
- ✅ Verificar endpoint retorna `activo = true`
- ✅ Verificar orden correcto (`ORDER BY orden ASC`)
- ✅ Llamar manualmente:
  ```kotlin
  chatViewModel.loadPredefinedResponses()
  ```

---

### ❌ Problema 6: App Crashea al Abrir Chat

**Síntomas**:
- `InstantiationException: Unable to instantiate fragment`
- App cierra al navegar a ChatFragment

**Diagnóstico**:

1. **Ver stacktrace completo**:
   ```bash
   adb logcat | grep -A 20 "FATAL EXCEPTION"
   ```

2. **Verificar nombre de clase**:
   ```kotlin
   // Debe ser:
   class ChatFragment : Fragment()
   
   // NO:
   class ChatFragmentNew : Fragment()
   ```

3. **Verificar mobile_navigation.xml**:
   ```xml
   <fragment
       android:id="@+id/nav_chat"
       android:name="com.example.controloperador.ui.chat.ChatFragment"
       tools:layout="@layout/fragment_chat" />
   ```

**Soluciones**:
- ✅ Verificar nombre de archivo y nombre de clase coinciden
- ✅ Clean & Rebuild: `./gradlew clean build`
- ✅ Invalidate Caches en Android Studio
- ✅ Verificar imports correctos en archivo

---

## 📊 Métricas de Éxito del Testing

### ✅ Checklist Final de Validación

#### Funcionalidad Básica
- [ ] Operador puede enviar mensaje de texto
- [ ] Mensaje aparece en backend SQL Server
- [ ] Analista puede responder (vía Postman)
- [ ] Operador recibe respuesta en <15 segundos
- [ ] Badge "sin leer" se actualiza correctamente

#### Estados y Sincronización
- [ ] Estado PENDING cuando offline
- [ ] Auto-retry cuando vuelve conexión
- [ ] Estado SENT después de envío exitoso
- [ ] Estado LEÍDO (✓✓) después de marcar como leído
- [ ] WorkManager ejecuta cada 15 segundos

#### Respuestas Predefinidas
- [ ] Bottom sheet carga respuestas del servidor
- [ ] Respuestas se pueden seleccionar y enviar
- [ ] Mensaje marcado con `is_predefined_response = true`

#### Persistencia y Limpieza
- [ ] Mensajes persisten en Room offline
- [ ] Solo mensajes del día se muestran
- [ ] Mensajes >30 días se eliminan automáticamente

#### UI/UX
- [ ] Burbujas diferenciadas (operador azul, analista gris)
- [ ] Scroll automático al último mensaje
- [ ] Iconos de estado visibles y correctos
- [ ] Bottom sheet Material Design 3 funcional

---

## 📚 Documentos Relacionados

1. **`BACKEND_CHAT_ESPECIFICACION.md`** - Especificación completa para backend Laravel
2. **`CHAT_IMPLEMENTACION_EXITOSA.md`** - Documentación técnica de implementación
3. **`STATUS_FINAL_CHAT.md`** - Status actual del proyecto
4. **`FIX_CHATFRAGMENT_INSTANTIATION.md`** - Fix de error de runtime

---

## 🎯 Conclusión

Este sistema de chat implementa una arquitectura **offline-first** con sincronización automática que garantiza:

- ✅ **Disponibilidad**: Mensajes accesibles sin conexión
- ✅ **Confiabilidad**: Retry automático de mensajes fallidos
- ✅ **Tiempo Real**: Sincronización cada 15 segundos
- ✅ **Escalabilidad**: Arquitectura preparada para múltiples operadores
- ✅ **Mantenibilidad**: Código limpio con patrón Repository y MVVM

### 🚀 Próximos Pasos Sugeridos

1. **Implementar Panel Web para Analistas**: Actualmente se usa Postman, ideal sería un panel web Laravel
2. **Notificaciones Push**: Firebase Cloud Messaging para notificar mensajes nuevos
3. **Mensajes de Voz**: Extensión para grabar y enviar audio
4. **Archivos Adjuntos**: Permitir enviar imágenes o documentos
5. **Búsqueda de Mensajes**: Buscar en historial completo (no solo del día)
6. **Analytics**: Métricas de tiempo de respuesta, mensajes por operador, etc.

---

**Desarrollado**: 31 de Octubre - 4 de Noviembre de 2025  
**Versión**: 1.0  
**Status**: ✅ PRODUCTION READY (Android) - Backend Laravel pendiente  
**Testing**: Listo para pruebas end-to-end

---

## 📞 Soporte

Para dudas o problemas durante el testing:

1. Revisar sección [Troubleshooting](#troubleshooting)
2. Verificar logs con `adb logcat`
3. Consultar `BACKEND_CHAT_ESPECIFICACION.md` para detalles de API
4. Verificar base de datos con queries de sección 8

**¡Buenas pruebas!** 🎉
