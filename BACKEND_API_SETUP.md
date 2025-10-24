# Backend API Setup - Laravel 7
## Sistema de Autenticación para Control de Operadores

Este documento contiene las instrucciones completas para implementar el backend de autenticación en Laravel 7.

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Estructura de la Base de Datos](#estructura-de-la-base-de-datos)
3. [Migraciones](#migraciones)
4. [Modelos](#modelos)
5. [Controladores](#controladores)
6. [Rutas](#rutas)
7. [Middleware](#middleware)
8. [Respuestas JSON](#respuestas-json)
9. [Seeders (Datos de Prueba)](#seeders-datos-de-prueba)
10. [Configuración CORS](#configuración-cors)
11. [Testing con Postman](#testing-con-postman)

---

## 🔧 Requisitos Previos

- PHP >= 7.2.5
- Laravel 7.x
- MySQL/MariaDB
- Composer instalado

---

## 🗄️ Estructura de la Base de Datos

**Nombre del Esquema**: `secomsa`

### Configuración de Base de Datos

Actualizar el archivo `.env` de Laravel:

```env
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=secomsa
DB_USERNAME=root
DB_PASSWORD=
```

Crear el esquema en MySQL:

```sql
CREATE DATABASE secomsa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE secomsa;
```

---

### Tablas del Sistema

#### 1. Tabla: `ct_transportistas`
**Descripción**: Catálogo de empresas transportistas

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| nombre | VARCHAR(255) | Nombre de la transportista |
| descripcion | TEXT NULL | Descripción detallada |
| razon_social | VARCHAR(255) | Razón social de la empresa |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

---

#### 2. Tabla: `ct_corredores`
**Descripción**: Catálogo de corredores asociados a transportistas

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| id_transportistas | BIGINT UNSIGNED (FK) | Relación con transportista |
| nombres | VARCHAR(255) | Nombres del corredor |
| descripcion | TEXT NULL | Descripción adicional |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Relaciones:**
- `id_transportistas` → `ct_transportistas.id` (ON DELETE CASCADE)

---

#### 3. Tabla: `ct_unidades`
**Descripción**: Catálogo de unidades (camiones) por corredor

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| id_corredor | BIGINT UNSIGNED (FK) | Relación con corredor |
| nombre | VARCHAR(100) | Identificador de la unidad |
| descripcion | TEXT NULL | Descripción de la unidad |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Relaciones:**
- `id_corredor` → `ct_corredores.id` (ON DELETE CASCADE)

---

#### 4. Tabla: `ct_user_control_operador`
**Descripción**: Usuarios operadores del sistema (tabla principal de autenticación)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| id_corredor | BIGINT UNSIGNED (FK) | Relación con corredor |
| user | VARCHAR(5) UNIQUE | Clave numérica del operador (5 dígitos) |
| nombre | VARCHAR(100) | Nombre del operador |
| apellido_paterno | VARCHAR(100) | Apellido paterno |
| apellido_materno | VARCHAR(100) NULL | Apellido materno |
| status | ENUM('active', 'inactive') | Estado del operador |
| last_login | TIMESTAMP NULL | Último inicio de sesión |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Relaciones:**
- `id_corredor` → `ct_corredores.id` (ON DELETE CASCADE)

**Índices:**
- `user` (UNIQUE)
- `status`
- `id_corredor`

---

#### 5. Tabla: `ct_mensajes_texto_predeterminados`
**Descripción**: Catálogo de mensajes de texto predefinidos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| nombre | VARCHAR(100) | Nombre del mensaje (ej: "Falla Mecánica") |
| mensaje | TEXT | Contenido del mensaje |
| descripcion | TEXT NULL | Descripción adicional |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Ejemplos de mensajes:**
- "Falla Mecánica" → "Unidad con falla mecánica, requiero asistencia"
- "Neumático Ponchado" → "Llanta ponchada, en proceso de cambio"
- "Siniestro" → "Reporto siniestro, requiero apoyo urgente"

---

#### 6. Tabla: `ct_mensajes_voz_predeterminados`
**Descripción**: Catálogo de mensajes de voz predefinidos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| nombre | VARCHAR(100) | Nombre del mensaje de voz |
| mensaje | TEXT | Descripción del mensaje de voz |
| descripcion | TEXT NULL | Descripción adicional |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

---

#### 7. Tabla: `cs_settings`
**Descripción**: Configuraciones del sistema (key-value store)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| clave | VARCHAR(100) UNIQUE | Clave de configuración |
| valor | TEXT | Valor de la configuración |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Ejemplos de configuraciones:**
- `session_timeout` → "28800" (8 horas en segundos)
- `api_version` → "1.0"
- `maintenance_mode` → "false"

---

### Tablas de Relación (Many-to-Many)

#### 8. Tabla: `sy_ct_mensajes_texto_predeterminados`
**Descripción**: Relación entre mensajes de texto predeterminados y corredores

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| id_mtp | BIGINT UNSIGNED (FK) | ID del mensaje de texto predeterminado |
| id_corredor | BIGINT UNSIGNED (FK) | ID del corredor |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Relaciones:**
- `id_mtp` → `ct_mensajes_texto_predeterminados.id` (ON DELETE CASCADE)
- `id_corredor` → `ct_corredores.id` (ON DELETE CASCADE)

**Índices:**
- UNIQUE(`id_mtp`, `id_corredor`) - Evita duplicados

---

#### 9. Tabla: `sy_ct_mensajes_voz_predeterminados`
**Descripción**: Relación entre mensajes de voz predeterminados y corredores

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT UNSIGNED (PK) | ID autoincremental |
| id_mvp | BIGINT UNSIGNED (FK) | ID del mensaje de voz predeterminado |
| id_corredor | BIGINT UNSIGNED (FK) | ID del corredor |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Fecha de actualización |

**Relaciones:**
- `id_mvp` → `ct_mensajes_voz_predeterminados.id` (ON DELETE CASCADE)
- `id_corredor` → `ct_corredores.id` (ON DELETE CASCADE)

**Índices:**
- UNIQUE(`id_mvp`, `id_corredor`) - Evita duplicados

---

### Diagrama de Relaciones

```
ct_transportistas (1) ──── (N) ct_corredores
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
               (1) ct_unidades   (N) ct_user_control_operador
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                                   │
        sy_ct_mensajes_texto_predeterminados   sy_ct_mensajes_voz_predeterminados
                    │                                   │
        ct_mensajes_texto_predeterminados   ct_mensajes_voz_predeterminados
```

---

## 📦 Migraciones

### Orden de Ejecución de Migraciones

Las migraciones deben ejecutarse en este orden debido a las dependencias de claves foráneas:

1. `ct_transportistas`
2. `ct_corredores`
3. `ct_unidades`
4. `ct_user_control_operador`
5. `ct_mensajes_texto_predeterminados`
6. `ct_mensajes_voz_predeterminados`
7. `cs_settings`
8. `sy_ct_mensajes_texto_predeterminados`
9. `sy_ct_mensajes_voz_predeterminados`

---

### 1. Migración: ct_transportistas

```bash
php artisan make:migration create_ct_transportistas_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtTransportistasTable extends Migration
{
    public function up()
    {
        Schema::create('ct_transportistas', function (Blueprint $table) {
            $table->id();
            $table->string('nombre')->comment('Nombre de la transportista');
            $table->text('descripcion')->nullable()->comment('Descripción detallada');
            $table->string('razon_social')->comment('Razón social de la empresa');
            $table->timestamps();
            
            $table->index('nombre');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_transportistas');
    }
}
```

---

### 2. Migración: ct_corredores

```bash
php artisan make:migration create_ct_corredores_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtCorredoresTable extends Migration
{
    public function up()
    {
        Schema::create('ct_corredores', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('id_transportistas')->comment('Relación con transportista');
            $table->string('nombres')->comment('Nombres del corredor');
            $table->text('descripcion')->nullable()->comment('Descripción adicional');
            $table->timestamps();
            
            $table->foreign('id_transportistas')
                  ->references('id')
                  ->on('ct_transportistas')
                  ->onDelete('cascade');
            
            $table->index('id_transportistas');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_corredores');
    }
}
```

---

### 3. Migración: ct_unidades

```bash
php artisan make:migration create_ct_unidades_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtUnidadesTable extends Migration
{
    public function up()
    {
        Schema::create('ct_unidades', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('id_corredor')->comment('Relación con corredor');
            $table->string('nombre', 100)->comment('Identificador de la unidad');
            $table->text('descripcion')->nullable()->comment('Descripción de la unidad');
            $table->timestamps();
            
            $table->foreign('id_corredor')
                  ->references('id')
                  ->on('ct_corredores')
                  ->onDelete('cascade');
            
            $table->index('id_corredor');
            $table->index('nombre');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_unidades');
    }
}
```

---

### 4. Migración: ct_user_control_operador

```bash
php artisan make:migration create_ct_user_control_operador_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtUserControlOperadorTable extends Migration
{
    public function up()
    {
        Schema::create('ct_user_control_operador', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('id_corredor')->comment('Relación con corredor');
            $table->string('user', 5)->unique()->comment('Clave numérica del operador (5 dígitos)');
            $table->string('nombre', 100)->comment('Nombre del operador');
            $table->string('apellido_paterno', 100)->comment('Apellido paterno');
            $table->string('apellido_materno', 100)->nullable()->comment('Apellido materno');
            $table->enum('status', ['active', 'inactive'])->default('active')->comment('Estado del operador');
            $table->timestamp('last_login')->nullable()->comment('Último inicio de sesión');
            $table->timestamps();
            
            $table->foreign('id_corredor')
                  ->references('id')
                  ->on('ct_corredores')
                  ->onDelete('cascade');
            
            $table->index('user');
            $table->index('status');
            $table->index('id_corredor');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_user_control_operador');
    }
}
```

---

### 5. Migración: ct_mensajes_texto_predeterminados

```bash
php artisan make:migration create_ct_mensajes_texto_predeterminados_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtMensajesTextoPredeterminadosTable extends Migration
{
    public function up()
    {
        Schema::create('ct_mensajes_texto_predeterminados', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 100)->comment('Nombre del mensaje');
            $table->text('mensaje')->comment('Contenido del mensaje');
            $table->text('descripcion')->nullable()->comment('Descripción adicional');
            $table->timestamps();
            
            $table->index('nombre');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_mensajes_texto_predeterminados');
    }
}
```

---

### 6. Migración: ct_mensajes_voz_predeterminados

```bash
php artisan make:migration create_ct_mensajes_voz_predeterminados_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCtMensajesVozPredeterminadosTable extends Migration
{
    public function up()
    {
        Schema::create('ct_mensajes_voz_predeterminados', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 100)->comment('Nombre del mensaje de voz');
            $table->text('mensaje')->comment('Descripción del mensaje de voz');
            $table->text('descripcion')->nullable()->comment('Descripción adicional');
            $table->timestamps();
            
            $table->index('nombre');
        });
    }

    public function down()
    {
        Schema::dropIfExists('ct_mensajes_voz_predeterminados');
    }
}
```

---

### 7. Migración: cs_settings

```bash
php artisan make:migration create_cs_settings_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCsSettingsTable extends Migration
{
    public function up()
    {
        Schema::create('cs_settings', function (Blueprint $table) {
            $table->id();
            $table->string('clave', 100)->unique()->comment('Clave de configuración');
            $table->text('valor')->comment('Valor de la configuración');
            $table->timestamps();
            
            $table->index('clave');
        });
    }

    public function down()
    {
        Schema::dropIfExists('cs_settings');
    }
}
```

---

### 8. Migración: sy_ct_mensajes_texto_predeterminados

```bash
php artisan make:migration create_sy_ct_mensajes_texto_predeterminados_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateSyCtMensajesTextoPredeterminadosTable extends Migration
{
    public function up()
    {
        Schema::create('sy_ct_mensajes_texto_predeterminados', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('id_mtp')->comment('ID del mensaje de texto predeterminado');
            $table->unsignedBigInteger('id_corredor')->comment('ID del corredor');
            $table->timestamps();
            
            $table->foreign('id_mtp')
                  ->references('id')
                  ->on('ct_mensajes_texto_predeterminados')
                  ->onDelete('cascade');
            
            $table->foreign('id_corredor')
                  ->references('id')
                  ->on('ct_corredores')
                  ->onDelete('cascade');
            
            // Evitar duplicados
            $table->unique(['id_mtp', 'id_corredor']);
            
            $table->index('id_mtp');
            $table->index('id_corredor');
        });
    }

    public function down()
    {
        Schema::dropIfExists('sy_ct_mensajes_texto_predeterminados');
    }
}
```

---

### 9. Migración: sy_ct_mensajes_voz_predeterminados

```bash
php artisan make:migration create_sy_ct_mensajes_voz_predeterminados_table
```

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateSyCtMensajesVozPredeterminadosTable extends Migration
{
    public function up()
    {
        Schema::create('sy_ct_mensajes_voz_predeterminados', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('id_mvp')->comment('ID del mensaje de voz predeterminado');
            $table->unsignedBigInteger('id_corredor')->comment('ID del corredor');
            $table->timestamps();
            
            $table->foreign('id_mvp')
                  ->references('id')
                  ->on('ct_mensajes_voz_predeterminados')
                  ->onDelete('cascade');
            
            $table->foreign('id_corredor')
                  ->references('id')
                  ->on('ct_corredores')
                  ->onDelete('cascade');
            
            // Evitar duplicados
            $table->unique(['id_mvp', 'id_corredor']);
            
            $table->index('id_mvp');
            $table->index('id_corredor');
        });
    }

    public function down()
    {
        Schema::dropIfExists('sy_ct_mensajes_voz_predeterminados');
    }
}
```

---

### Ejecutar todas las migraciones

```bash
php artisan migrate
```

Para revertir todas las migraciones:

```bash
php artisan migrate:rollback
```

Para reiniciar migraciones (elimina y recrea):

```bash
php artisan migrate:fresh
```

```bash
php artisan migrate
```

---

## 🎯 Modelos

### Crear los modelos

```bash
php artisan make:model Transportista
php artisan make:model Corredor
php artisan make:model Unidad
php artisan make:model UserControlOperador
php artisan make:model MensajeTextoPredeterminado
php artisan make:model MensajeVozPredeterminado
php artisan make:model Setting
```

---

### 1. Modelo: Transportista

**Archivo: `app/Transportista.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Transportista extends Model
{
    protected $table = 'ct_transportistas';
    
    protected $fillable = [
        'nombre',
        'descripcion',
        'razon_social',
    ];
    
    /**
     * Relación: Una transportista tiene muchos corredores
     */
    public function corredores()
    {
        return $this->hasMany(Corredor::class, 'id_transportistas');
    }
}
```

---

### 2. Modelo: Corredor

**Archivo: `app/Corredor.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Corredor extends Model
{
    protected $table = 'ct_corredores';
    
    protected $fillable = [
        'id_transportistas',
        'nombres',
        'descripcion',
    ];
    
    /**
     * Relación: Un corredor pertenece a una transportista
     */
    public function transportista()
    {
        return $this->belongsTo(Transportista::class, 'id_transportistas');
    }
    
    /**
     * Relación: Un corredor tiene muchas unidades
     */
    public function unidades()
    {
        return $this->hasMany(Unidad::class, 'id_corredor');
    }
    
    /**
     * Relación: Un corredor tiene muchos operadores
     */
    public function operadores()
    {
        return $this->hasMany(UserControlOperador::class, 'id_corredor');
    }
    
    /**
     * Relación Many-to-Many: Mensajes de texto predeterminados
     */
    public function mensajesTextoPredeterminados()
    {
        return $this->belongsToMany(
            MensajeTextoPredeterminado::class,
            'sy_ct_mensajes_texto_predeterminados',
            'id_corredor',
            'id_mtp'
        );
    }
    
    /**
     * Relación Many-to-Many: Mensajes de voz predeterminados
     */
    public function mensajesVozPredeterminados()
    {
        return $this->belongsToMany(
            MensajeVozPredeterminado::class,
            'sy_ct_mensajes_voz_predeterminados',
            'id_corredor',
            'id_mvp'
        );
    }
}
```

---

### 3. Modelo: Unidad

**Archivo: `app/Unidad.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Unidad extends Model
{
    protected $table = 'ct_unidades';
    
    protected $fillable = [
        'id_corredor',
        'nombre',
        'descripcion',
    ];
    
    /**
     * Relación: Una unidad pertenece a un corredor
     */
    public function corredor()
    {
        return $this->belongsTo(Corredor::class, 'id_corredor');
    }
}
```

---

### 4. Modelo: UserControlOperador (Principal para autenticación)

**Archivo: `app/UserControlOperador.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;
use Carbon\Carbon;

class UserControlOperador extends Model
{
    protected $table = 'ct_user_control_operador';
    
    protected $fillable = [
        'id_corredor',
        'user',
        'nombre',
        'apellido_paterno',
        'apellido_materno',
        'status',
        'last_login',
    ];
    
    protected $casts = [
        'last_login' => 'datetime',
    ];
    
    /**
     * Relación: Un operador pertenece a un corredor
     */
    public function corredor()
    {
        return $this->belongsTo(Corredor::class, 'id_corredor');
    }
    
    /**
     * Scope: Obtener solo operadores activos
     */
    public function scopeActive($query)
    {
        return $query->where('status', 'active');
    }
    
    /**
     * Verifica si el operador está activo
     */
    public function isActive()
    {
        return $this->status === 'active';
    }
    
    /**
     * Actualiza el último login
     */
    public function updateLastLogin()
    {
        $this->last_login = Carbon::now();
        $this->save();
    }
    
    /**
     * Obtiene el nombre completo del operador
     */
    public function getNombreCompletoAttribute()
    {
        $apellidos = trim($this->apellido_paterno . ' ' . ($this->apellido_materno ?? ''));
        return trim($this->nombre . ' ' . $apellidos);
    }
}
```

---

### 5. Modelo: MensajeTextoPredeterminado

**Archivo: `app/MensajeTextoPredeterminado.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class MensajeTextoPredeterminado extends Model
{
    protected $table = 'ct_mensajes_texto_predeterminados';
    
    protected $fillable = [
        'nombre',
        'mensaje',
        'descripcion',
    ];
    
    /**
     * Relación Many-to-Many: Corredores que tienen este mensaje
     */
    public function corredores()
    {
        return $this->belongsToMany(
            Corredor::class,
            'sy_ct_mensajes_texto_predeterminados',
            'id_mtp',
            'id_corredor'
        );
    }
}
```

---

### 6. Modelo: MensajeVozPredeterminado

**Archivo: `app/MensajeVozPredeterminado.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class MensajeVozPredeterminado extends Model
{
    protected $table = 'ct_mensajes_voz_predeterminados';
    
    protected $fillable = [
        'nombre',
        'mensaje',
        'descripcion',
    ];
    
    /**
     * Relación Many-to-Many: Corredores que tienen este mensaje
     */
    public function corredores()
    {
        return $this->belongsToMany(
            Corredor::class,
            'sy_ct_mensajes_voz_predeterminados',
            'id_mvp',
            'id_corredor'
        );
    }
}
```

---

### 7. Modelo: Setting

**Archivo: `app/Setting.php`**

```php
<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Setting extends Model
{
    protected $table = 'cs_settings';
    
    protected $fillable = [
        'clave',
        'valor',
    ];
    
    /**
     * Obtener valor de una configuración por clave
     */
    public static function get($clave, $default = null)
    {
        $setting = self::where('clave', $clave)->first();
        return $setting ? $setting->valor : $default;
    }
    
    /**
     * Establecer valor de una configuración
     */
    public static function set($clave, $valor)
    {
        return self::updateOrCreate(
            ['clave' => $clave],
            ['valor' => $valor]
        );
    }
}
```

---

## 🎮 Controladores

### Crear el controlador

```bash
php artisan make:controller Api/AuthController
```

### Archivo: `app/Http/Controllers/Api/AuthController.php`

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\UserControlOperador;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Log;

class AuthController extends Controller
{
    /**
     * Autenticar operador por clave numérica
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function login(Request $request)
    {
        // Validar la entrada
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5|regex:/^[0-9]{5}$/',
        ], [
            'operator_code.required' => 'La clave de operador es requerida.',
            'operator_code.size' => 'La clave debe tener exactamente 5 dígitos.',
            'operator_code.regex' => 'La clave debe contener solo números.',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos de entrada inválidos.',
                'errors' => $validator->errors()
            ], 422);
        }

        try {
            // Buscar operador por código en ct_user_control_operador
            $operator = UserControlOperador::where('user', $request->operator_code)
                                           ->active()
                                           ->with('corredor.transportista') // Eager loading
                                           ->first();

            if (!$operator) {
                Log::warning('Intento de login fallido', [
                    'operator_code' => $request->operator_code,
                    'ip' => $request->ip()
                ]);

                return response()->json([
                    'success' => false,
                    'message' => 'Clave de operador incorrecta o inactiva.',
                ], 401);
            }

            // Actualizar último login
            $operator->updateLastLogin();

            Log::info('Login exitoso', [
                'operator_id' => $operator->id,
                'operator_code' => $operator->user,
                'corredor' => $operator->corredor->nombres ?? 'N/A',
                'ip' => $request->ip()
            ]);

            return response()->json([
                'success' => true,
                'message' => 'Autenticación exitosa.',
                'data' => [
                    'operator' => [
                        'id' => $operator->id,
                        'operator_code' => $operator->user,
                        'name' => $operator->nombre_completo,
                        'nombre' => $operator->nombre,
                        'apellido_paterno' => $operator->apellido_paterno,
                        'apellido_materno' => $operator->apellido_materno,
                        'corredor' => [
                            'id' => $operator->corredor->id ?? null,
                            'nombre' => $operator->corredor->nombres ?? null,
                            'transportista' => $operator->corredor->transportista->nombre ?? null,
                        ],
                        'last_login' => $operator->last_login,
                    ],
                    'session' => [
                        'expires_in' => 28800, // 8 horas en segundos
                    ]
                ]
            ], 200);

        } catch (\Exception $e) {
            Log::error('Error en login', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Error interno del servidor.',
            ], 500);
        }
    }

    /**
     * Verificar si una clave de operador existe y está activa
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function verify(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5|regex:/^[0-9]{5}$/',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos de entrada inválidos.',
                'errors' => $validator->errors()
            ], 422);
        }

        $exists = UserControlOperador::where('user', $request->operator_code)
                                     ->active()
                                     ->exists();

        return response()->json([
            'success' => true,
            'data' => [
                'exists' => $exists,
                'is_active' => $exists,
            ]
        ], 200);
    }

    /**
     * Cerrar sesión (logout)
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function logout(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos de entrada inválidos.',
            ], 422);
        }

        Log::info('Logout', [
            'operator_code' => $request->operator_code,
            'ip' => $request->ip()
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Sesión cerrada exitosamente.',
        ], 200);
    }
    
    /**
     * Obtener mensajes predeterminados del operador
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function getPredefinedMessages(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'operator_code' => 'required|string|size:5',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Datos de entrada inválidos.',
            ], 422);
        }

        try {
            $operator = UserControlOperador::where('user', $request->operator_code)
                                           ->active()
                                           ->with([
                                               'corredor.mensajesTextoPredeterminados',
                                               'corredor.mensajesVozPredeterminados'
                                           ])
                                           ->first();

            if (!$operator) {
                return response()->json([
                    'success' => false,
                    'message' => 'Operador no encontrado.',
                ], 404);
            }

            return response()->json([
                'success' => true,
                'data' => [
                    'text_messages' => $operator->corredor->mensajesTextoPredeterminados ?? [],
                    'voice_messages' => $operator->corredor->mensajesVozPredeterminados ?? [],
                ]
            ], 200);

        } catch (\Exception $e) {
            Log::error('Error al obtener mensajes predeterminados', [
                'error' => $e->getMessage()
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Error interno del servidor.',
            ], 500);
        }
    }
}
```

---

## 🛣️ Rutas

### Archivo: `routes/api.php`

```php
<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes - Control de Operadores
|--------------------------------------------------------------------------
*/

Route::prefix('v1')->group(function () {
    
    // Rutas de autenticación (sin autenticación requerida)
    Route::prefix('auth')->group(function () {
        Route::post('/login', 'Api\AuthController@login');
        Route::post('/verify', 'Api\AuthController@verify');
        Route::post('/logout', 'Api\AuthController@logout');
    });
    
    // Rutas de mensajes predeterminados
    Route::prefix('messages')->group(function () {
        Route::post('/predefined', 'Api\AuthController@getPredefinedMessages');
    });

    // Ejemplo de ruta protegida (implementar middleware si es necesario)
    // Route::middleware('auth:api')->group(function () {
    //     Route::get('/operators', 'Api\OperatorController@index');
    // });
});

// Ruta de prueba para verificar que la API está funcionando
Route::get('/health', function () {
    return response()->json([
        'status' => 'ok',
        'timestamp' => now()->toIso8601String(),
        'service' => 'Control de Operadores API',
        'database' => 'secomsa',
    ]);
});
```

**URLs resultantes:**
- Login: `POST http://tu-dominio.com/api/v1/auth/login`
- Verificar: `POST http://tu-dominio.com/api/v1/auth/verify`
- Logout: `POST http://tu-dominio.com/api/v1/auth/logout`
- Mensajes Predefinidos: `POST http://tu-dominio.com/api/v1/messages/predefined`
- Health Check: `GET http://tu-dominio.com/api/health`

---

## 🛡️ Middleware

### Crear middleware para rate limiting (opcional)

```bash
php artisan make:middleware ThrottleLogin
```

### Archivo: `app/Http/Middleware/ThrottleLogin.php`

```php
<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Support\Facades\RateLimiter;

class ThrottleLogin
{
    /**
     * Handle an incoming request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \Closure  $next
     * @return mixed
     */
    public function handle($request, Closure $next)
    {
        $key = 'login:' . $request->ip();

        if (RateLimiter::tooManyAttempts($key, 5)) {
            return response()->json([
                'success' => false,
                'message' => 'Demasiados intentos de login. Intente nuevamente en ' . 
                            RateLimiter::availableIn($key) . ' segundos.',
            ], 429);
        }

        RateLimiter::hit($key, 60); // 5 intentos por minuto

        return $next($request);
    }
}
```

### Registrar middleware en `app/Http/Kernel.php`

```php
protected $routeMiddleware = [
    // ... otros middlewares
    'throttle.login' => \App\Http\Middleware\ThrottleLogin::class,
];
```

### Aplicar en rutas (opcional)

```php
Route::post('/login', 'Api\AuthController@login')->middleware('throttle.login');
```

---

## 📤 Respuestas JSON

### Estructura de Respuestas

#### ✅ Login Exitoso (200)

```json
{
    "success": true,
    "message": "Autenticación exitosa.",
    "data": {
        "operator": {
            "id": 1,
            "operator_code": "12345",
            "name": "Juan Pérez García",
            "last_login": "2025-10-23T14:30:45.000000Z"
        },
        "session": {
            "expires_in": 28800
        }
    }
}
```

#### ❌ Login Fallido - Clave Incorrecta (401)

```json
{
    "success": false,
    "message": "Clave de operador incorrecta o inactiva."
}
```

#### ❌ Validación Fallida (422)

```json
{
    "success": false,
    "message": "Datos de entrada inválidos.",
    "errors": {
        "operator_code": [
            "La clave debe tener exactamente 5 dígitos."
        ]
    }
}
```

#### ❌ Error del Servidor (500)

```json
{
    "success": false,
    "message": "Error interno del servidor."
}
```

#### ✅ Verificación Exitosa (200)

```json
{
    "success": true,
    "data": {
        "exists": true,
        "is_active": true
    }
}
```

---

## 🌱 Seeders (Datos de Prueba)

### Crear los seeders

```bash
php artisan make:seeder DatabaseSeeder
php artisan make:seeder TransportistaSeeder
php artisan make:seeder CorredorSeeder
php artisan make:seeder UnidadSeeder
php artisan make:seeder UserControlOperadorSeeder
php artisan make:seeder MensajeTextoPredeterminadoSeeder
php artisan make:seeder MensajeVozPredeterminadoSeeder
php artisan make:seeder SettingSeeder
php artisan make:seeder SyMensajesSeeder
```

---

### 1. Seeder: TransportistaSeeder

**Archivo: `database/seeds/TransportistaSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\Transportista;

class TransportistaSeeder extends Seeder
{
    public function run()
    {
        $transportistas = [
            [
                'nombre' => 'Transportes del Norte SA',
                'descripcion' => 'Empresa de transporte de carga pesada',
                'razon_social' => 'Transportes del Norte Sociedad Anónima',
            ],
            [
                'nombre' => 'Logística Express',
                'descripcion' => 'Servicios de logística y transporte',
                'razon_social' => 'Logística Express SA de CV',
            ],
        ];

        foreach ($transportistas as $transportista) {
            Transportista::create($transportista);
        }

        $this->command->info('✅ Transportistas creadas exitosamente.');
    }
}
```

---

### 2. Seeder: CorredorSeeder

**Archivo: `database/seeds/CorredorSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\Corredor;

class CorredorSeeder extends Seeder
{
    public function run()
    {
        $corredores = [
            [
                'id_transportistas' => 1,
                'nombres' => 'Ruta México-Guadalajara',
                'descripcion' => 'Corredor principal de la zona occidente',
            ],
            [
                'id_transportistas' => 1,
                'nombres' => 'Ruta Monterrey-Querétaro',
                'descripcion' => 'Corredor de la zona norte',
            ],
            [
                'id_transportistas' => 2,
                'nombres' => 'Ruta Puebla-Veracruz',
                'descripcion' => 'Corredor de la zona golfo',
            ],
        ];

        foreach ($corredores as $corredor) {
            Corredor::create($corredor);
        }

        $this->command->info('✅ Corredores creados exitosamente.');
    }
}
```

---

### 3. Seeder: UnidadSeeder

**Archivo: `database/seeds/UnidadSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\Unidad;

class UnidadSeeder extends Seeder
{
    public function run()
    {
        $unidades = [
            ['id_corredor' => 1, 'nombre' => 'T-001', 'descripcion' => 'Kenworth T680'],
            ['id_corredor' => 1, 'nombre' => 'T-002', 'descripcion' => 'Freightliner Cascadia'],
            ['id_corredor' => 1, 'nombre' => 'T-003', 'descripcion' => 'Volvo VNL'],
            ['id_corredor' => 2, 'nombre' => 'T-101', 'descripcion' => 'Peterbilt 579'],
            ['id_corredor' => 2, 'nombre' => 'T-102', 'descripcion' => 'International LT'],
            ['id_corredor' => 3, 'nombre' => 'T-201', 'descripcion' => 'Mack Anthem'],
        ];

        foreach ($unidades as $unidad) {
            Unidad::create($unidad);
        }

        $this->command->info('✅ Unidades creadas exitosamente.');
    }
}
```

---

### 4. Seeder: UserControlOperadorSeeder

**Archivo: `database/seeds/UserControlOperadorSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\UserControlOperador;
use Carbon\Carbon;

class UserControlOperadorSeeder extends Seeder
{
    public function run()
    {
        $operadores = [
            [
                'id_corredor' => 1,
                'user' => '12345',
                'nombre' => 'Juan',
                'apellido_paterno' => 'Pérez',
                'apellido_materno' => 'García',
                'status' => 'active',
                'last_login' => null,
            ],
            [
                'id_corredor' => 1,
                'user' => '54321',
                'nombre' => 'María',
                'apellido_paterno' => 'López',
                'apellido_materno' => 'Ramírez',
                'status' => 'active',
                'last_login' => null,
            ],
            [
                'id_corredor' => 2,
                'user' => '11111',
                'nombre' => 'Carlos',
                'apellido_paterno' => 'Martínez',
                'apellido_materno' => 'Sánchez',
                'status' => 'active',
                'last_login' => null,
            ],
            [
                'id_corredor' => 2,
                'user' => '99999',
                'nombre' => 'Ana',
                'apellido_paterno' => 'González',
                'apellido_materno' => 'Fernández',
                'status' => 'active',
                'last_login' => null,
            ],
            [
                'id_corredor' => 3,
                'user' => '00000',
                'nombre' => 'Pedro',
                'apellido_paterno' => 'Rodríguez',
                'apellido_materno' => 'Torres',
                'status' => 'inactive',
                'last_login' => Carbon::now()->subDays(30),
            ],
        ];

        foreach ($operadores as $operador) {
            UserControlOperador::create($operador);
        }

        $this->command->info('✅ Operadores creados exitosamente.');
    }
}
```

---

### 5. Seeder: MensajeTextoPredeterminadoSeeder

**Archivo: `database/seeds/MensajeTextoPredeterminadoSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\MensajeTextoPredeterminado;

class MensajeTextoPredeterminadoSeeder extends Seeder
{
    public function run()
    {
        $mensajes = [
            [
                'nombre' => 'Falla Mecánica',
                'mensaje' => 'Unidad con falla mecánica, requiero asistencia inmediata',
                'descripcion' => 'Mensaje para reportar fallas mecánicas en la unidad',
            ],
            [
                'nombre' => 'Neumático Ponchado',
                'mensaje' => 'Llanta ponchada, en proceso de cambio',
                'descripcion' => 'Notificación de neumático ponchado',
            ],
            [
                'nombre' => 'Siniestro',
                'mensaje' => 'Reporto siniestro, requiero apoyo urgente',
                'descripcion' => 'Alerta de accidente o siniestro',
            ],
            [
                'nombre' => 'Tráfico Pesado',
                'mensaje' => 'Tráfico pesado en ruta, posible retraso',
                'descripcion' => 'Notificación de congestión vehicular',
            ],
            [
                'nombre' => 'Desviación',
                'mensaje' => 'Tomando ruta alterna por cierre de carretera',
                'descripcion' => 'Aviso de cambio de ruta',
            ],
            [
                'nombre' => 'Falla Prepago',
                'mensaje' => 'Sistema de prepago sin saldo, requiero recarga',
                'descripcion' => 'Notificación de saldo insuficiente en prepago',
            ],
        ];

        foreach ($mensajes as $mensaje) {
            MensajeTextoPredeterminado::create($mensaje);
        }

        $this->command->info('✅ Mensajes de texto predeterminados creados.');
    }
}
```

---

### 6. Seeder: MensajeVozPredeterminadoSeeder

**Archivo: `database/seeds/MensajeVozPredeterminadoSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\MensajeVozPredeterminado;

class MensajeVozPredeterminadoSeeder extends Seeder
{
    public function run()
    {
        $mensajes = [
            [
                'nombre' => 'Llegada a Destino',
                'mensaje' => 'Mensaje de voz confirmando llegada al destino',
                'descripcion' => 'Audio notificando arribo exitoso',
            ],
            [
                'nombre' => 'Inicio de Ruta',
                'mensaje' => 'Mensaje de voz confirmando inicio de recorrido',
                'descripcion' => 'Audio notificando salida',
            ],
            [
                'nombre' => 'Parada Intermedia',
                'mensaje' => 'Mensaje de voz reportando parada de descanso',
                'descripcion' => 'Audio de notificación de parada programada',
            ],
        ];

        foreach ($mensajes as $mensaje) {
            MensajeVozPredeterminado::create($mensaje);
        }

        $this->command->info('✅ Mensajes de voz predeterminados creados.');
    }
}
```

---

### 7. Seeder: SettingSeeder

**Archivo: `database/seeds/SettingSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\Setting;

class SettingSeeder extends Seeder
{
    public function run()
    {
        $settings = [
            ['clave' => 'session_timeout', 'valor' => '28800'], // 8 horas
            ['clave' => 'api_version', 'valor' => '1.0'],
            ['clave' => 'maintenance_mode', 'valor' => 'false'],
            ['clave' => 'max_login_attempts', 'valor' => '5'],
            ['clave' => 'app_name', 'valor' => 'Control de Operadores'],
        ];

        foreach ($settings as $setting) {
            Setting::create($setting);
        }

        $this->command->info('✅ Configuraciones del sistema creadas.');
    }
}
```

---

### 8. Seeder: SyMensajesSeeder (Relaciones)

**Archivo: `database/seeds/SyMensajesSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;
use App\Corredor;
use App\MensajeTextoPredeterminado;
use App\MensajeVozPredeterminado;

class SyMensajesSeeder extends Seeder
{
    public function run()
    {
        // Asignar todos los mensajes de texto a cada corredor
        $corredores = Corredor::all();
        $mensajesTexto = MensajeTextoPredeterminado::all();
        $mensajesVoz = MensajeVozPredeterminado::all();

        foreach ($corredores as $corredor) {
            // Asignar mensajes de texto
            $corredor->mensajesTextoPredeterminados()->attach($mensajesTexto->pluck('id'));
            
            // Asignar mensajes de voz
            $corredor->mensajesVozPredeterminados()->attach($mensajesVoz->pluck('id'));
        }

        $this->command->info('✅ Relaciones de mensajes creadas exitosamente.');
    }
}
```

---

### 9. DatabaseSeeder Principal

**Archivo: `database/seeds/DatabaseSeeder.php`**

```php
<?php

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     *
     * @return void
     */
    public function run()
    {
        // Orden de ejecución respetando dependencias
        $this->call([
            TransportistaSeeder::class,
            CorredorSeeder::class,
            UnidadSeeder::class,
            UserControlOperadorSeeder::class,
            MensajeTextoPredeterminadoSeeder::class,
            MensajeVozPredeterminadoSeeder::class,
            SettingSeeder::class,
            SyMensajesSeeder::class,
        ]);

        $this->command->info('🎉 ¡Base de datos poblada exitosamente!');
    }
}
```

---

### Ejecutar seeders

```bash
# Ejecutar todos los seeders
php artisan db:seed

# Ejecutar un seeder específico
php artisan db:seed --class=UserControlOperadorSeeder

# Refrescar base de datos y ejecutar seeders
php artisan migrate:fresh --seed
```

---

## 🌐 Configuración CORS

### Instalar paquete CORS (si no está instalado)

```bash
composer require fruitcake/laravel-cors
```

### Archivo: `config/cors.php`

```php
<?php

return [
    'paths' => ['api/*'],
    'allowed_methods' => ['*'],
    'allowed_origins' => ['*'], // En producción, especificar dominios permitidos
    'allowed_origins_patterns' => [],
    'allowed_headers' => ['*'],
    'exposed_headers' => [],
    'max_age' => 0,
    'supports_credentials' => false,
];
```

### Registrar middleware en `app/Http/Kernel.php`

```php
protected $middleware = [
    // ... otros middlewares
    \Fruitcake\Cors\HandleCors::class,
];
```

---

## 🧪 Testing con Postman

### 1. Login

**Request:**
```http
POST http://localhost:8000/api/v1/auth/login
Content-Type: application/json

{
    "operator_code": "12345"
}
```

**Expected Response (200):**
```json
{
    "success": true,
    "message": "Autenticación exitosa.",
    "data": {
        "operator": {
            "id": 1,
            "operator_code": "12345",
            "name": "Juan Pérez García",
            "last_login": "2025-10-23T14:30:45.000000Z"
        },
        "session": {
            "expires_in": 28800
        }
    }
}
```

### 2. Verificar Operador

**Request:**
```http
POST http://localhost:8000/api/v1/auth/verify
Content-Type: application/json

{
    "operator_code": "12345"
}
```

### 3. Logout

**Request:**
```http
POST http://localhost:8000/api/v1/auth/logout
Content-Type: application/json

{
    "operator_code": "12345"
}
```

### 4. Health Check

**Request:**
```http
GET http://localhost:8000/api/health
```

---

## 📝 Pasos de Implementación (Orden)

1. ✅ Crear migración: `php artisan make:migration create_operators_table`
2. ✅ Ejecutar migración: `php artisan migrate`
3. ✅ Crear modelo: `php artisan make:model Operator`
4. ✅ Crear controlador: `php artisan make:controller Api/AuthController`
5. ✅ Agregar rutas en `routes/api.php`
6. ✅ Crear seeder: `php artisan make:seeder OperatorSeeder`
7. ✅ Ejecutar seeder: `php artisan db:seed --class=OperatorSeeder`
8. ✅ Configurar CORS si es necesario
9. ✅ Probar endpoints con Postman
10. ✅ Verificar logs en `storage/logs/laravel.log`

---

## 🔒 Consideraciones de Seguridad

1. **Rate Limiting**: Implementar middleware para limitar intentos de login
2. **HTTPS**: Usar siempre HTTPS en producción
3. **Validación**: Validar siempre el formato de la clave (5 dígitos numéricos)
4. **Logs**: Registrar intentos fallidos para detección de ataques
5. **CORS**: Configurar dominios permitidos específicos en producción
6. **Variables de Entorno**: No hardcodear URLs o credenciales

---

## 📊 Variables de Entorno (.env)

```env
# Aplicación
APP_NAME="Control de Operadores - SECOMSA"
APP_ENV=local
APP_DEBUG=true
APP_URL=http://localhost:8000

# Base de datos
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=secomsa
DB_USERNAME=root
DB_PASSWORD=

# Sesión
SESSION_LIFETIME=480

# Cache
CACHE_DRIVER=file
QUEUE_CONNECTION=sync

# Logs
LOG_CHANNEL=stack
```

---

## 🚀 Comandos Útiles

```bash
# Limpiar cache
php artisan config:clear
php artisan cache:clear
php artisan route:clear

# Ver rutas registradas
php artisan route:list

# Iniciar servidor de desarrollo
php artisan serve

# Logs en tiempo real
tail -f storage/logs/laravel.log
```

---

## 📞 Soporte

Para dudas o problemas:
1. Verificar logs en `storage/logs/laravel.log`
2. Revisar configuración de CORS
3. Verificar que la base de datos esté corriendo
4. Comprobar que los seeders se ejecutaron correctamente

---

**Versión**: 2.0  
**Fecha**: Octubre 2025  
**Framework**: Laravel 7.x  
**Base de Datos**: MySQL - Esquema `secomsa`  
**Compatible con**: ControlOperador Android App

---

## 📋 Resumen de Tablas del Sistema

### Tablas Principales (9 tablas)

| # | Tabla | Propósito | Registros Iniciales |
|---|-------|-----------|---------------------|
| 1 | `ct_transportistas` | Empresas transportistas | 2 |
| 2 | `ct_corredores` | Rutas/Corredores de transporte | 3 |
| 3 | `ct_unidades` | Camiones/Unidades | 6 |
| 4 | `ct_user_control_operador` | Operadores (Login) | 5 |
| 5 | `ct_mensajes_texto_predeterminados` | Mensajes de texto rápidos | 6 |
| 6 | `ct_mensajes_voz_predeterminados` | Mensajes de voz rápidos | 3 |
| 7 | `cs_settings` | Configuraciones del sistema | 5 |
| 8 | `sy_ct_mensajes_texto_predeterminados` | Relación mensajes texto-corredor | N |
| 9 | `sy_ct_mensajes_voz_predeterminados` | Relación mensajes voz-corredor | N |

### Flujo de Autenticación

```
Usuario ingresa clave (5 dígitos)
         ↓
Consulta en ct_user_control_operador
         ↓
    ¿Existe y está activo?
         ↓
    SÍ → Login exitoso + datos de corredor y transportista
         ↓
    NO → Error 401: Clave incorrecta
```

### Claves de Prueba Disponibles

| Clave | Nombre Completo | Corredor | Estado |
|-------|-----------------|----------|--------|
| 12345 | Juan Pérez García | Ruta México-Guadalajara | Activo |
| 54321 | María López Ramírez | Ruta México-Guadalajara | Activo |
| 11111 | Carlos Martínez Sánchez | Ruta Monterrey-Querétaro | Activo |
| 99999 | Ana González Fernández | Ruta Monterrey-Querétaro | Activo |
| 00000 | Pedro Rodríguez Torres | Ruta Puebla-Veracruz | Inactivo |

---

## 🎯 Checklist de Implementación

- [ ] Crear base de datos `secomsa`
- [ ] Configurar archivo `.env` con credenciales correctas
- [ ] Ejecutar `composer install`
- [ ] Ejecutar `php artisan migrate` (9 migraciones)
- [ ] Ejecutar `php artisan db:seed` (poblar con datos de prueba)
- [ ] Configurar CORS para permitir peticiones desde Android
- [ ] Probar endpoint de health check
- [ ] Probar login con claves de prueba en Postman
- [ ] Verificar logs en `storage/logs/laravel.log`
- [ ] Configurar servidor web (Apache/Nginx)
- [ ] Actualizar `BASE_URL` en Android `RetrofitClient.kt`

---
