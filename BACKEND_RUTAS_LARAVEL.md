# 🚀 Implementación Rápida de Rutas Laravel - Chat API

**Problema Detectado**: Los endpoints retornan **404 Not Found**  
**Causa**: Las rutas del chat NO están implementadas en el backend Laravel  
**Solución**: Implementar rutas, controlador y modelos

---

## 📋 Checklist de Implementación

- [ ] Crear migraciones de tablas (conversations, messages, predefined_responses)
- [ ] Crear modelos Eloquent (Conversation, Message, PredefinedResponse)
- [ ] Crear ChatController con 4 métodos
- [ ] Registrar rutas en routes/api.php
- [ ] Ejecutar migraciones
- [ ] Insertar respuestas predefinidas de prueba
- [ ] Probar endpoints con Postman

---

## 1️⃣ Crear Migraciones

### Migración 1: `conversations`

```bash
php artisan make:migration create_conversations_table
```

```php
<?php
// database/migrations/YYYY_MM_DD_HHMMSS_create_conversations_table.php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateConversationsTable extends Migration
{
    public function up()
    {
        Schema::create('conversations', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('operator_code', 10)->unique();
            $table->timestamp('created_at')->useCurrent();
            $table->timestamp('last_message_at')->nullable();
            $table->integer('unread_count')->default(0);
            
            // Foreign key a mt_operadores
            $table->foreign('operator_code')
                  ->references('clave_operador')
                  ->on('mt_operadores')
                  ->onDelete('cascade');
            
            $table->index('operator_code');
            $table->index('last_message_at');
        });
    }

    public function down()
    {
        Schema::dropIfExists('conversations');
    }
}
```

---

### Migración 2: `messages`

```bash
php artisan make:migration create_messages_table
```

```php
<?php
// database/migrations/YYYY_MM_DD_HHMMSS_create_messages_table.php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMessagesTable extends Migration
{
    public function up()
    {
        Schema::create('messages', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->uuid('conversation_id');
            $table->text('content');
            $table->enum('sender_type', ['OPERADOR', 'ANALISTA']);
            $table->string('sender_id', 50);
            $table->string('sender_name', 100)->nullable();
            $table->enum('sync_status', ['PENDING', 'SENT', 'FAILED'])->default('SENT');
            $table->timestamp('read_at')->nullable();
            $table->timestamp('created_at')->useCurrent();
            $table->boolean('is_predefined_response')->default(false);
            $table->uuid('predefined_response_id')->nullable();
            
            // Foreign key a conversations con CASCADE delete
            $table->foreign('conversation_id')
                  ->references('id')
                  ->on('conversations')
                  ->onDelete('cascade');
            
            $table->index('conversation_id');
            $table->index('created_at');
            $table->index(['sender_type', 'sender_id']);
            // Índice para consultar mensajes del día
            $table->index(\DB::raw('CAST(created_at AS DATE)'));
        });
    }

    public function down()
    {
        Schema::dropIfExists('messages');
    }
}
```

---

### Migración 3: `predefined_responses`

```bash
php artisan make:migration create_predefined_responses_table
```

```php
<?php
// database/migrations/YYYY_MM_DD_HHMMSS_create_predefined_responses_table.php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreatePredefinedResponsesTable extends Migration
{
    public function up()
    {
        Schema::create('predefined_responses', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('mensaje', 500);
            $table->string('categoria', 100)->nullable();
            $table->integer('orden')->default(0);
            $table->boolean('activo')->default(true);
            $table->timestamp('created_at')->useCurrent();
            
            $table->index(['activo', 'orden']);
        });
    }

    public function down()
    {
        Schema::dropIfExists('predefined_responses');
    }
}
```

---

## 2️⃣ Crear Modelos Eloquent

### Modelo: `Conversation`

```bash
php artisan make:model Conversation
```

```php
<?php
// app/Conversation.php

namespace App;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class Conversation extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';
    public $timestamps = false;
    
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
     * Relación con mensajes
     */
    public function messages()
    {
        return $this->hasMany(Message::class, 'conversation_id');
    }
    
    /**
     * Relación con operador
     */
    public function operator()
    {
        return $this->belongsTo('App\MtOperador', 'operator_code', 'clave_operador');
    }
}
```

---

### Modelo: `Message`

```bash
php artisan make:model Message
```

```php
<?php
// app/Message.php

namespace App;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class Message extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';
    public $timestamps = false;
    
    protected $fillable = [
        'id',
        'conversation_id',
        'content',
        'sender_type',
        'sender_id',
        'sender_name',
        'sync_status',
        'read_at',
        'created_at',
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
     * Relación con conversación
     */
    public function conversation()
    {
        return $this->belongsTo(Conversation::class, 'conversation_id');
    }
}
```

---

### Modelo: `PredefinedResponse`

```bash
php artisan make:model PredefinedResponse
```

```php
<?php
// app/PredefinedResponse.php

namespace App;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class PredefinedResponse extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';
    public $timestamps = false;
    
    protected $fillable = [
        'id',
        'mensaje',
        'categoria',
        'orden',
        'activo',
        'created_at'
    ];
    
    protected $casts = [
        'activo' => 'boolean',
        'orden' => 'integer',
        'created_at' => 'datetime'
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
     * Scope para respuestas activas
     */
    public function scopeActive($query)
    {
        return $query->where('activo', true)->orderBy('orden');
    }
}
```

---

## 3️⃣ Crear ChatController

```bash
php artisan make:controller ChatController
```

```php
<?php
// app/Http/Controllers/ChatController.php

namespace App\Http\Controllers;

use App\Conversation;
use App\Message;
use App\PredefinedResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class ChatController extends Controller
{
    /**
     * Enviar mensaje (operador o analista)
     * POST /api/v1/chat/send
     */
    public function send(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|max:10',
            'content' => 'required|string|max:5000',
            'sender_type' => 'required|in:OPERADOR,ANALISTA',
            'sender_id' => 'required|string|max:50',
            'is_predefined_response' => 'boolean',
            'predefined_response_id' => 'nullable|string'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos inválidos',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            // Obtener o crear conversación
            $conversation = Conversation::firstOrCreate(
                ['operator_code' => $request->operator_code],
                [
                    'id' => (string) Str::uuid(),
                    'created_at' => now(),
                    'last_message_at' => now()
                ]
            );
            
            // Crear mensaje
            $message = Message::create([
                'id' => (string) Str::uuid(),
                'conversation_id' => $conversation->id,
                'content' => $request->content,
                'sender_type' => $request->sender_type,
                'sender_id' => $request->sender_id,
                'sender_name' => $request->sender_name ?? ($request->sender_type === 'ANALISTA' ? 'Soporte' : null),
                'sync_status' => 'SENT',
                'created_at' => now(),
                'is_predefined_response' => $request->is_predefined_response ?? false,
                'predefined_response_id' => $request->predefined_response_id
            ]);
            
            // Actualizar conversación
            $conversation->update([
                'last_message_at' => now()
            ]);
            
            // Si es mensaje de analista, incrementar unread_count
            if ($request->sender_type === 'ANALISTA') {
                $conversation->increment('unread_count');
            }
            
            return response()->json([
                'success' => true,
                'message' => 'Mensaje enviado correctamente',
                'data' => [
                    'message_id' => $message->id,
                    'conversation_id' => $conversation->id,
                    'created_at' => $message->created_at->toIso8601String()
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al enviar mensaje: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * Obtener mensajes del día actual
     * GET /api/v1/chat/messages/today?operator_code=12345&last_id=uuid
     */
    public function getTodayMessages(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|max:10',
            'last_id' => 'nullable|string'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Parámetros inválidos',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            // Obtener conversación
            $conversation = Conversation::where('operator_code', $request->operator_code)->first();
            
            if (!$conversation) {
                return response()->json([
                    'success' => true,
                    'data' => [
                        'messages' => [],
                        'has_more' => false,
                        'total' => 0
                    ]
                ], 200);
            }
            
            // Query de mensajes del día
            $query = Message::where('conversation_id', $conversation->id)
                ->whereDate('created_at', today())
                ->orderBy('created_at', 'ASC');
            
            // Si se proporciona last_id, solo mensajes después de ese ID
            if ($request->last_id) {
                $lastMessage = Message::find($request->last_id);
                if ($lastMessage) {
                    $query->where('created_at', '>', $lastMessage->created_at);
                }
            }
            
            $messages = $query->get()->map(function ($message) {
                return [
                    'id' => $message->id,
                    'conversation_id' => $message->conversation_id,
                    'content' => $message->content,
                    'sender_type' => $message->sender_type,
                    'sender_id' => $message->sender_id,
                    'sender_name' => $message->sender_name,
                    'read_at' => $message->read_at ? $message->read_at->toIso8601String() : null,
                    'created_at' => $message->created_at->toIso8601String(),
                    'is_predefined_response' => $message->is_predefined_response
                ];
            });
            
            return response()->json([
                'success' => true,
                'data' => [
                    'messages' => $messages,
                    'has_more' => false,
                    'total' => $messages->count()
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al obtener mensajes: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * Marcar mensajes como leídos
     * POST /api/v1/chat/mark-read
     */
    public function markAsRead(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|max:10',
            'message_ids' => 'required|array',
            'message_ids.*' => 'string'
        ]);
        
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos inválidos',
                'errors' => $validator->errors()
            ], 422);
        }
        
        try {
            $conversation = Conversation::where('operator_code', $request->operator_code)->first();
            
            if (!$conversation) {
                return response()->json([
                    'success' => false,
                    'message' => 'Conversación no encontrada'
                ], 404);
            }
            
            // Actualizar mensajes
            $updated = Message::whereIn('id', $request->message_ids)
                ->where('conversation_id', $conversation->id)
                ->whereNull('read_at')
                ->update(['read_at' => now()]);
            
            // Resetear unread_count
            $conversation->update(['unread_count' => 0]);
            
            return response()->json([
                'success' => true,
                'message' => 'Mensajes marcados como leídos',
                'data' => [
                    'updated_count' => $updated
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
     * Obtener respuestas predefinidas
     * GET /api/v1/chat/predefined-responses
     */
    public function getPredefinedResponses()
    {
        try {
            $responses = PredefinedResponse::active()->get()->map(function ($response) {
                return [
                    'id' => $response->id,
                    'mensaje' => $response->mensaje,
                    'categoria' => $response->categoria,
                    'orden' => $response->orden,
                    'activo' => $response->activo
                ];
            });
            
            return response()->json([
                'success' => true,
                'data' => [
                    'responses' => $responses
                ]
            ], 200);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al obtener respuestas: ' . $e->getMessage()
            ], 500);
        }
    }
}
```

---

## 4️⃣ Registrar Rutas

```php
<?php
// routes/api.php

// Chat routes (Laravel 7 sintaxis)
Route::prefix('v1/chat')->group(function () {
    Route::post('send', 'ChatController@send');
    Route::get('messages/today', 'ChatController@getTodayMessages');
    Route::post('mark-read', 'ChatController@markAsRead');
    Route::get('predefined-responses', 'ChatController@getPredefinedResponses');
});
```

---

## 5️⃣ Ejecutar Migraciones

```bash
# Ejecutar todas las migraciones
php artisan migrate

# Verificar tablas creadas
php artisan db:table conversations
php artisan db:table messages
php artisan db:table predefined_responses
```

---

## 6️⃣ Insertar Respuestas Predefinidas de Prueba

```sql
-- Ejecutar en SQL Server Management Studio
INSERT INTO predefined_responses (id, mensaje, categoria, orden, activo, created_at) VALUES
(NEWID(), 'Estoy en mi ruta habitual', 'Ubicación', 1, 1, GETDATE()),
(NEWID(), 'Todo en orden', 'Estado', 2, 1, GETDATE()),
(NEWID(), 'Necesito asistencia', 'Urgente', 3, 1, GETDATE()),
(NEWID(), 'Tráfico detenido', 'Tráfico', 4, 1, GETDATE()),
(NEWID(), 'Llegando a destino', 'Ubicación', 5, 1, GETDATE());
```

**O desde Laravel:**

```bash
php artisan tinker
```

```php
use App\PredefinedResponse;

PredefinedResponse::create(['mensaje' => 'Estoy en mi ruta habitual', 'categoria' => 'Ubicación', 'orden' => 1, 'activo' => true]);
PredefinedResponse::create(['mensaje' => 'Todo en orden', 'categoria' => 'Estado', 'orden' => 2, 'activo' => true]);
PredefinedResponse::create(['mensaje' => 'Necesito asistencia', 'categoria' => 'Urgente', 'orden' => 3, 'activo' => true]);
PredefinedResponse::create(['mensaje' => 'Tráfico detenido', 'categoria' => 'Tráfico', 'orden' => 4, 'activo' => true]);
PredefinedResponse::create(['mensaje' => 'Llegando a destino', 'categoria' => 'Ubicación', 'orden' => 5, 'activo' => true]);
```

---

## 7️⃣ Verificar que Funciona

```bash
# Ver rutas registradas
php artisan route:list | grep chat

# Iniciar servidor
php artisan serve --host=172.16.20.10 --port=8000

# Probar endpoint desde otra terminal
curl http://172.16.20.10:8000/api/v1/chat/predefined-responses

# Respuesta esperada:
{
  "success": true,
  "data": {
    "responses": [
      {
        "id": "uuid-1",
        "mensaje": "Estoy en mi ruta habitual",
        "categoria": "Ubicación",
        "orden": 1,
        "activo": true
      },
      ...
    ]
  }
}
```

---

## 8️⃣ Probar desde la App Android

```kotlin
// Actualizar BASE_URL en RetrofitClient.kt
const val BASE_URL = "http://172.16.20.10:8000/api/v1/"

// Ejecutar app y verificar logs
adb logcat | grep ChatRepository
```

---

## 🎯 Resumen de Implementación

### Archivos a Crear:

1. ✅ **3 Migraciones**: `conversations`, `messages`, `predefined_responses`
2. ✅ **3 Modelos**: `Conversation.php`, `Message.php`, `PredefinedResponse.php`
3. ✅ **1 Controlador**: `ChatController.php` con 4 métodos
4. ✅ **Rutas**: Agregar a `routes/api.php`

### Comandos a Ejecutar:

```bash
# 1. Crear migraciones
php artisan make:migration create_conversations_table
php artisan make:migration create_messages_table
php artisan make:migration create_predefined_responses_table

# 2. Crear modelos
php artisan make:model Conversation
php artisan make:model Message
php artisan make:model PredefinedResponse

# 3. Crear controlador
php artisan make:controller ChatController

# 4. Ejecutar migraciones
php artisan migrate

# 5. Iniciar servidor
php artisan serve --host=172.16.20.10 --port=8000

# 6. Verificar rutas
php artisan route:list | grep chat
```

---

## ✅ URLs Finales que Funcionarán

```
✅ http://172.16.20.10:8000/api/v1/chat/send
✅ http://172.16.20.10:8000/api/v1/chat/messages/today?operator_code=12345
✅ http://172.16.20.10:8000/api/v1/chat/mark-read
✅ http://172.16.20.10:8000/api/v1/chat/predefined-responses
```

---

**Última actualización**: 4 de Noviembre de 2025
