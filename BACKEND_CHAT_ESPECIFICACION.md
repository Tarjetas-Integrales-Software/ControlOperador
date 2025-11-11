# Especificación Técnica: Sistema de Chat para Backend Laravel 7

## 📋 Información General

- **Backend**: Laravel 7.x
- **Base de Datos**: SQL Server (transportistas2023)
- **Tipo de API**: RESTful
- **Formato**: JSON
- **Autenticación**: Basada en `operator_code` (código operador de 5 dígitos)
- **Charset**: UTF-8
- **Timezone**: UTC (Coordinated Universal Time)

---

## 🎯 Contexto del Proyecto

### Participantes del Chat
- **OPERADORES**: Conductores de camiones que usan la app Android
  - Se identifican con código de 5 dígitos (ej: `12345`)
  - Tienen UNA conversación única con el sistema de soporte
  - Solo ven mensajes del día actual en la app
  - NO saben qué analista específico les responde

- **ANALISTAS**: Personal de soporte que usa panel web
  - Se identifican con `users.id` + `users.email` de la tabla `dbo.users`
  - Pueden responder a cualquier operador
  - Cualquier analista puede continuar una conversación iniciada por otro
  - El operador los ve a todos como "Soporte" (sin identificación individual)

### Características Principales
- ✅ Cada operador tiene una conversación única con "Soporte"
- ✅ Mensajes persisten 30 días en la base de datos
- ✅ App Android solo carga mensajes del día actual
- ✅ Estados de mensaje: **Enviando** → **Enviado** → **Leído**
- ✅ Sincronización cada 15 segundos desde la app
- ✅ Respuestas predefinidas dinámicas desde el backend
- ❌ NO usa WebSocket ni Firebase (polling HTTP)

---

## 🗄️ Estructura de Base de Datos

### 1. Tabla: `conversations`
Almacena conversaciones únicas por operador.

```sql
CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    operator_code VARCHAR(5) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    last_message_at DATETIME NOT NULL DEFAULT GETDATE(),
    unread_count INT NOT NULL DEFAULT 0,
    
    -- Índices
    CONSTRAINT UQ_conversations_operator UNIQUE (operator_code)
);

CREATE INDEX IX_conversations_operator_code ON conversations(operator_code);
CREATE INDEX IX_conversations_last_message ON conversations(last_message_at);
```

### 2. Tabla: `messages`
Almacena todos los mensajes del chat.

```sql
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    conversation_id VARCHAR(36) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    sender_type VARCHAR(20) NOT NULL, -- 'OPERADOR' o 'ANALISTA'
    sender_id VARCHAR(50) NOT NULL, -- operator_code (5 dígitos) o users.id (analista)
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    read_at DATETIME NULL, -- NULL si no ha sido leído
    is_predefined_response BIT NOT NULL DEFAULT 0,
    predefined_response_id VARCHAR(36) NULL,
    
    -- Foreign Keys
    CONSTRAINT FK_messages_conversation 
        FOREIGN KEY (conversation_id) 
        REFERENCES conversations(id) 
        ON DELETE CASCADE,
    
    -- Validación
    CONSTRAINT CK_sender_type CHECK (sender_type IN ('OPERADOR', 'ANALISTA'))
);

CREATE INDEX IX_messages_conversation ON messages(conversation_id);
CREATE INDEX IX_messages_created_at ON messages(created_at);
CREATE INDEX IX_messages_sender_type ON messages(sender_type);
CREATE INDEX IX_messages_read_at ON messages(read_at);
```

### 3. Tabla: `predefined_responses`
Respuestas predeterminadas configurables desde admin.

```sql
CREATE TABLE predefined_responses (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    mensaje NVARCHAR(500) NOT NULL,
    categoria VARCHAR(100) NULL, -- Ej: 'tráfico', 'mecánico', 'general'
    orden INT NOT NULL DEFAULT 0, -- Orden de aparición en la app
    activo BIT NOT NULL DEFAULT 1, -- Habilitar/deshabilitar
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL
);

CREATE INDEX IX_predefined_responses_activo ON predefined_responses(activo);
CREATE INDEX IX_predefined_responses_orden ON predefined_responses(orden);
```

### 4. Datos Iniciales (Seed)
```sql
-- Insertar respuestas predefinidas de ejemplo
INSERT INTO predefined_responses (id, mensaje, categoria, orden, activo) VALUES
(NEWID(), 'Tráfico detenido, retraso estimado 15 minutos', 'tráfico', 1, 1),
(NEWID(), 'Ruta completada sin incidentes', 'general', 2, 1),
(NEWID(), 'Solicito asistencia mecánica', 'mecánico', 3, 1),
(NEWID(), 'Pausa para descanso programado', 'general', 4, 1),
(NEWID(), 'Desvío por manifestación en ruta', 'tráfico', 5, 1),
(NEWID(), 'Llegada anticipada al destino', 'general', 6, 1);
```

---

## 🔌 Endpoints de la API

### Base URL
```
Desarrollo:  http://172.16.20.10:8000/api/v1
Producción:  https://backtransportistas.tarjetasintegrales.mx:806/api/v1
```

---

### 1. **POST** `/chat/send`
Envía un mensaje del operador a los analistas.

#### Request Body
```json
{
  "operator_code": "12345",
  "content": "Tráfico detenido en Av. Reforma",
  "is_predefined_response": false,
  "predefined_response_id": null,
  "local_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### Request Fields
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `operator_code` | string(5) | Sí | Código del operador |
| `content` | string | Sí | Contenido del mensaje (máx 2000 caracteres) |
| `is_predefined_response` | boolean | No | Si es respuesta predefinida (default: false) |
| `predefined_response_id` | string | No | ID de respuesta predefinida (si aplica) |
| `local_id` | string(UUID) | Sí | ID local del mensaje para tracking |

#### Validaciones (Laravel Request)
```php
[
    'operator_code' => 'required|string|size:5',
    'content' => 'required|string|max:2000',
    'is_predefined_response' => 'boolean',
    'predefined_response_id' => 'nullable|string|exists:predefined_responses,id',
    'local_id' => 'required|uuid'
]
```

#### Response Success (200)
```json
{
  "success": true,
  "message": "Mensaje enviado correctamente",
  "data": {
    "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
    "conversation_id": "a1a2a3a4-b5b6-c7c8-d9d0-e1e2e3e4e5e6",
    "content": "Tráfico detenido en Av. Reforma",
    "sender_type": "OPERADOR",
    "sender_id": "12345",
    "created_at": "2025-10-31T14:30:45Z",
    "read_at": null
  }
}
```

#### Response Error (422 Validation)
```json
{
  "success": false,
  "message": "Error de validación",
  "errors": {
    "operator_code": ["El código de operador es requerido"],
    "content": ["El contenido no puede estar vacío"]
  }
}
```

---

### 2. **GET** `/chat/messages/today`
Obtiene los mensajes del día actual para un operador.

#### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `operator_code` | string(5) | Sí | Código del operador |
| `last_id` | string(UUID) | No | ID del último mensaje recibido (paginación) |

#### Request Example
```
GET /api/v1/chat/messages/today?operator_code=12345&last_id=abc123
```

#### Response Success (200)
```json
{
  "success": true,
  "message": "Mensajes cargados correctamente",
  "data": {
    "conversation_id": "a1a2a3a4-b5b6-c7c8-d9d0-e1e2e3e4e5e6",
    "messages": [
      {
        "id": "msg-001",
        "conversation_id": "a1a2a3a4-b5b6-c7c8-d9d0-e1e2e3e4e5e6",
        "content": "Buenos días operador, favor reportar status",
        "sender_type": "ANALISTA",
        "sender_id": "42",
        "created_at": "2025-10-31T08:00:00Z",
        "read_at": null
      },
      {
        "id": "msg-002",
        "conversation_id": "a1a2a3a4-b5b6-c7c8-d9d0-e1e2e3e4e5e6",
        "content": "Tráfico detenido",
        "sender_type": "OPERADOR",
        "sender_id": "12345",
        "created_at": "2025-10-31T08:05:00Z",
        "read_at": "2025-10-31T08:06:00Z"
      }
    ],
    "total": 2,
    "unread_count": 1
  }
}
```

#### Lógica del Endpoint
1. Buscar o crear conversación para el `operator_code`
2. Filtrar mensajes con `DATE(created_at) = CURDATE()`
3. Si `last_id` está presente, solo devolver mensajes con `id > last_id`
4. Ordenar por `created_at ASC` (más antiguo primero)
5. Calcular `unread_count`: mensajes de ANALISTA con `read_at IS NULL`

---

### 3. **POST** `/chat/mark-read`
Marca mensajes como leídos por el destinatario.

#### Request Body
```json
{
  "message_ids": [
    "msg-001",
    "msg-002",
    "msg-003"
  ]
}
```

#### Validaciones
```php
[
    'message_ids' => 'required|array|min:1',
    'message_ids.*' => 'required|string|exists:messages,id'
]
```

#### Response Success (200)
```json
{
  "success": true,
  "message": "Mensajes marcados como leídos",
  "data": {
    "marked_count": 3,
    "read_at": "2025-10-31T14:35:00Z"
  }
}
```

#### Lógica del Endpoint
1. Validar que los `message_ids` existan
2. Actualizar `read_at = NOW()` para cada mensaje
3. **Importante**: Solo actualizar si `read_at IS NULL` (no sobrescribir)
4. Devolver el conteo de mensajes actualizados

---

### 4. **GET** `/chat/predefined-responses`
Obtiene las respuestas predefinidas activas.

#### Response Success (200)
```json
{
  "success": true,
  "message": "Respuestas predefinidas cargadas",
  "data": {
    "responses": [
      {
        "id": "resp-001",
        "mensaje": "Tráfico detenido, retraso estimado 15 minutos",
        "categoria": "tráfico",
        "orden": 1,
        "activo": true
      },
      {
        "id": "resp-002",
        "mensaje": "Ruta completada sin incidentes",
        "categoria": "general",
        "orden": 2,
        "activo": true
      }
    ],
    "total": 2
  }
}
```

#### Lógica del Endpoint
1. Seleccionar solo respuestas con `activo = 1`
2. Ordenar por `orden ASC`
3. Devolver lista completa (no paginar, son pocas)

---

## 📝 Modelos Eloquent

### Conversation.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class Conversation extends Model
{
    protected $table = 'conversations';
    public $incrementing = false;
    protected $keyType = 'string';
    
    protected $fillable = [
        'id',
        'operator_code',
        'created_at',
        'last_message_at',
        'unread_count'
    ];
    
    protected $casts = [
        'created_at' => 'datetime',
        'last_message_at' => 'datetime',
        'unread_count' => 'integer'
    ];
    
    protected static function boot()
    {
        parent::boot();
        
        static::creating(function ($model) {
            if (empty($model->id)) {
                $model->id = (string) Str::uuid();
            }
        });
    }
    
    /**
     * Relación: Una conversación tiene muchos mensajes
     */
    public function messages()
    {
        return $this->hasMany(Message::class, 'conversation_id');
    }
    
    /**
     * Mensajes del día actual
     */
    public function todayMessages()
    {
        return $this->messages()
            ->whereDate('created_at', today())
            ->orderBy('created_at', 'asc');
    }
    
    /**
     * Mensajes no leídos (enviados por analistas)
     */
    public function unreadMessages()
    {
        return $this->messages()
            ->where('sender_type', 'ANALISTA')
            ->whereNull('read_at');
    }
}
```

### Message.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class Message extends Model
{
    protected $table = 'messages';
    public $incrementing = false;
    protected $keyType = 'string';
    public $timestamps = false; // Usamos created_at manual
    
    protected $fillable = [
        'id',
        'conversation_id',
        'content',
        'sender_type',
        'sender_id',
        'created_at',
        'read_at',
        'is_predefined_response',
        'predefined_response_id'
    ];
    
    protected $casts = [
        'created_at' => 'datetime',
        'read_at' => 'datetime',
        'is_predefined_response' => 'boolean'
    ];
    
    protected static function boot()
    {
        parent::boot();
        
        static::creating(function ($model) {
            if (empty($model->id)) {
                $model->id = (string) Str::uuid();
            }
            if (empty($model->created_at)) {
                $model->created_at = now();
            }
        });
    }
    
    /**
     * Relación: Un mensaje pertenece a una conversación
     */
    public function conversation()
    {
        return $this->belongsTo(Conversation::class, 'conversation_id');
    }
    
    /**
     * Relación: Respuesta predefinida (si aplica)
     */
    public function predefinedResponse()
    {
        return $this->belongsTo(PredefinedResponse::class, 'predefined_response_id');
    }
    
    /**
     * Scope: Mensajes de hoy
     */
    public function scopeToday($query)
    {
        return $query->whereDate('created_at', today());
    }
    
    /**
     * Scope: Mensajes no leídos
     */
    public function scopeUnread($query)
    {
        return $query->whereNull('read_at');
    }
}
```

### PredefinedResponse.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class PredefinedResponse extends Model
{
    protected $table = 'predefined_responses';
    public $incrementing = false;
    protected $keyType = 'string';
    
    protected $fillable = [
        'id',
        'mensaje',
        'categoria',
        'orden',
        'activo',
        'created_at',
        'updated_at'
    ];
    
    protected $casts = [
        'activo' => 'boolean',
        'orden' => 'integer',
        'created_at' => 'datetime',
        'updated_at' => 'datetime'
    ];
    
    protected static function boot()
    {
        parent::boot();
        
        static::creating(function ($model) {
            if (empty($model->id)) {
                $model->id = (string) Str::uuid();
            }
        });
    }
    
    /**
     * Scope: Solo respuestas activas
     */
    public function scopeActive($query)
    {
        return $query->where('activo', true);
    }
    
    /**
     * Scope: Ordenadas por orden
     */
    public function scopeOrdered($query)
    {
        return $query->orderBy('orden', 'asc');
    }
}
```

---

## 🎮 Controlador: ChatController.php

```php
<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Conversation;
use App\Models\Message;
use App\Models\PredefinedResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class ChatController extends Controller
{
    /**
     * Envía un mensaje del operador
     * POST /api/v1/chat/send
     */
    public function sendMessage(Request $request)
    {
        // Validación
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5',
            'content' => 'required|string|max:2000',
            'is_predefined_response' => 'boolean',
            'predefined_response_id' => 'nullable|string|exists:predefined_responses,id',
            'local_id' => 'required|uuid'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Error de validación',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            DB::beginTransaction();
            
            // 1. Obtener o crear conversación
            $conversation = Conversation::firstOrCreate(
                ['operator_code' => $request->operator_code],
                [
                    'created_at' => now(),
                    'last_message_at' => now(),
                    'unread_count' => 0
                ]
            );
            
            // 2. Crear mensaje
            $message = Message::create([
                'conversation_id' => $conversation->id,
                'content' => $request->content,
                'sender_type' => 'OPERADOR',
                'sender_id' => $request->operator_code,
                'created_at' => now(),
                'is_predefined_response' => $request->is_predefined_response ?? false,
                'predefined_response_id' => $request->predefined_response_id
            ]);
            
            // 3. Actualizar timestamp de conversación
            $conversation->update([
                'last_message_at' => now()
            ]);
            
            DB::commit();
            
            return response()->json([
                'success' => true,
                'message' => 'Mensaje enviado correctamente',
                'data' => [
                    'id' => $message->id,
                    'conversation_id' => $message->conversation_id,
                    'content' => $message->content,
                    'sender_type' => $message->sender_type,
                    'sender_id' => $message->sender_id,
                    'created_at' => $message->created_at->toIso8601String(),
                    'read_at' => $message->read_at ? $message->read_at->toIso8601String() : null
                ]
            ], 200);
            
        } catch (\Exception $e) {
            DB::rollBack();
            
            return response()->json([
                'success' => false,
                'message' => 'Error al enviar mensaje: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * Obtiene mensajes del día actual
     * GET /api/v1/chat/messages/today
     */
    public function getTodayMessages(Request $request)
    {
        // Validación
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5',
            'last_id' => 'nullable|string'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Error de validación',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            // 1. Obtener o crear conversación
            $conversation = Conversation::firstOrCreate(
                ['operator_code' => $request->operator_code],
                [
                    'created_at' => now(),
                    'last_message_at' => now(),
                    'unread_count' => 0
                ]
            );
            
            // 2. Query de mensajes del día actual
            $query = Message::where('conversation_id', $conversation->id)
                ->whereDate('created_at', today())
                ->orderBy('created_at', 'asc');
            
            // 3. Paginación incremental (si last_id está presente)
            if ($request->has('last_id') && !empty($request->last_id)) {
                $lastMessage = Message::find($request->last_id);
                if ($lastMessage) {
                    $query->where('created_at', '>', $lastMessage->created_at);
                }
            }
            
            $messages = $query->get();
            
            // 4. Calcular mensajes no leídos (enviados por ANALISTA)
            $unreadCount = Message::where('conversation_id', $conversation->id)
                ->where('sender_type', 'ANALISTA')
                ->whereNull('read_at')
                ->whereDate('created_at', today())
                ->count();
            
            // 5. Formatear respuesta
            $formattedMessages = $messages->map(function ($message) {
                return [
                    'id' => $message->id,
                    'conversation_id' => $message->conversation_id,
                    'content' => $message->content,
                    'sender_type' => $message->sender_type,
                    'sender_id' => $message->sender_id,
                    'created_at' => $message->created_at->toIso8601String(),
                    'read_at' => $message->read_at ? $message->read_at->toIso8601String() : null
                ];
            });
            
            return response()->json([
                'success' => true,
                'message' => 'Mensajes cargados correctamente',
                'data' => [
                    'conversation_id' => $conversation->id,
                    'messages' => $formattedMessages,
                    'total' => $formattedMessages->count(),
                    'unread_count' => $unreadCount
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al cargar mensajes: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * Marca mensajes como leídos
     * POST /api/v1/chat/mark-read
     */
    public function markAsRead(Request $request)
    {
        // Validación
        $validator = Validator::make($request->all(), [
            'message_ids' => 'required|array|min:1',
            'message_ids.*' => 'required|string|exists:messages,id'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Error de validación',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            $readAt = now();
            
            // Actualizar solo mensajes que NO han sido leídos
            $markedCount = Message::whereIn('id', $request->message_ids)
                ->whereNull('read_at')
                ->update(['read_at' => $readAt]);
            
            return response()->json([
                'success' => true,
                'message' => 'Mensajes marcados como leídos',
                'data' => [
                    'marked_count' => $markedCount,
                    'read_at' => $readAt->toIso8601String()
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al marcar mensajes: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * Obtiene respuestas predefinidas activas
     * GET /api/v1/chat/predefined-responses
     */
    public function getPredefinedResponses()
    {
        try {
            $responses = PredefinedResponse::active()
                ->ordered()
                ->get(['id', 'mensaje', 'categoria', 'orden', 'activo']);
            
            return response()->json([
                'success' => true,
                'message' => 'Respuestas predefinidas cargadas',
                'data' => [
                    'responses' => $responses,
                    'total' => $responses->count()
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al cargar respuestas: ' . $e->getMessage()
            ], 500);
        }
    }
}
```

---

## 🛣️ Rutas (api.php)

```php
// routes/api.php

Route::prefix('v1')->group(function () {
    
    // Chat endpoints
    Route::prefix('chat')->group(function () {
        Route::post('/send', 'Api\V1\ChatController@sendMessage');
        Route::get('/messages/today', 'Api\V1\ChatController@getTodayMessages');
        Route::post('/mark-read', 'Api\V1\ChatController@markAsRead');
        Route::get('/predefined-responses', 'Api\V1\ChatController@getPredefinedResponses');
    });
    
});
```

---

## 🧹 Tarea Programada: Limpieza de Mensajes Antiguos

### Comando Artisan: `CleanupOldMessages.php`

```php
<?php

namespace App\Console\Commands;

use App\Models\Message;
use Carbon\Carbon;
use Illuminate\Console\Command;

class CleanupOldMessages extends Command
{
    protected $signature = 'chat:cleanup-old-messages';
    protected $description = 'Elimina mensajes de chat más antiguos que 30 días';
    
    public function handle()
    {
        $this->info('Iniciando limpieza de mensajes antiguos...');
        
        $beforeDate = Carbon::now()->subDays(30);
        
        $deletedCount = Message::where('created_at', '<', $beforeDate)->delete();
        
        $this->info("✅ Se eliminaron {$deletedCount} mensajes antiguos");
        
        return 0;
    }
}
```

### Programar en `Kernel.php`

```php
// app/Console/Kernel.php

protected function schedule(Schedule $schedule)
{
    // Limpiar mensajes más antiguos que 30 días (diariamente a las 2 AM)
    $schedule->command('chat:cleanup-old-messages')
        ->dailyAt('02:00')
        ->withoutOverlapping();
}
```

---

## 🧪 Testing con Postman

### 1. Enviar Mensaje
```
POST {{base_url}}/v1/chat/send
Content-Type: application/json

{
  "operator_code": "12345",
  "content": "Prueba de mensaje desde Postman",
  "is_predefined_response": false,
  "local_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Obtener Mensajes del Día
```
GET {{base_url}}/v1/chat/messages/today?operator_code=12345
```

### 3. Marcar como Leído
```
POST {{base_url}}/v1/chat/mark-read
Content-Type: application/json

{
  "message_ids": [
    "msg-001",
    "msg-002"
  ]
}
```

### 4. Obtener Respuestas Predefinidas
```
GET {{base_url}}/v1/chat/predefined-responses
```

### Variables de Entorno Postman
```
base_url_dev: http://172.16.20.10:8000/api
base_url_prod: https://backtransportistas.tarjetasintegrales.mx:806/api
```

---

## ⚠️ Consideraciones Importantes

### Seguridad
- ✅ Validar todos los inputs con Laravel Request Validation
- ✅ Usar prepared statements (Eloquent lo hace automáticamente)
- ✅ Sanitizar contenido de mensajes antes de guardar
- ✅ Implementar rate limiting para evitar spam
- ⚠️ Considerar agregar autenticación JWT en futuras versiones

### Performance
- ✅ Índices en `operator_code`, `created_at`, `conversation_id`
- ✅ Limitar resultado de mensajes con paginación incremental (`last_id`)
- ✅ Usar `whereDate()` optimizado para filtrar mensajes del día
- ✅ Cache opcional para respuestas predefinidas (cambian poco)

### Escalabilidad
- ✅ Diseño preparado para múltiples operadores concurrentes
- ✅ Limpieza automática de mensajes antiguos (30 días)
- ⚠️ Si crece mucho, considerar particionado de tablas por fecha

---

## 📊 Queries SQL de Ejemplo

### Mensajes del día actual de un operador
```sql
SELECT m.*
FROM messages m
INNER JOIN conversations c ON m.conversation_id = c.id
WHERE c.operator_code = '12345'
  AND CAST(m.created_at AS DATE) = CAST(GETDATE() AS DATE)
ORDER BY m.created_at ASC;
```

### Conteo de mensajes no leídos
```sql
SELECT COUNT(*) as unread_count
FROM messages m
INNER JOIN conversations c ON m.conversation_id = c.id
WHERE c.operator_code = '12345'
  AND m.sender_type = 'ANALISTA'
  AND m.read_at IS NULL
  AND CAST(m.created_at AS DATE) = CAST(GETDATE() AS DATE);
```

### Eliminar mensajes antiguos
```sql
DELETE FROM messages
WHERE created_at < DATEADD(day, -30, GETDATE());
```

---

## 🎉 Checklist de Implementación

- [ ] Crear tablas `conversations`, `messages`, `predefined_responses`
- [ ] Insertar datos seed de respuestas predefinidas
- [ ] Crear modelos Eloquent (Conversation, Message, PredefinedResponse)
- [ ] Crear ChatController con los 4 endpoints
- [ ] Registrar rutas en `api.php`
- [ ] Crear comando `chat:cleanup-old-messages`
- [ ] Programar comando en `Kernel.php`
- [ ] Probar endpoints con Postman
- [ ] Verificar índices y optimizaciones
- [ ] Documentar para el equipo de frontend

---

## 📞 Contacto y Soporte

Si tienes dudas sobre la implementación, contacta al equipo de desarrollo Android.

**Versión del documento**: 1.0  
**Fecha**: 31 de octubre de 2025  
**Autor**: GitHub Copilot  
**Proyecto**: ControlOperador - Sistema de Chat
