# 🚀 Guía Rápida - Testing Chat Operador ↔ Analistas

**Versión Corta para Testing Rápido**

---

## 🎯 Resumen en 5 Minutos

### ¿Qué es?
Chat bidireccional entre **Operadores** (app móvil) y **Analistas** (panel web).

### ¿Cómo funciona?
1. Operador envía mensaje → Guarda en Room → API → Backend SQL Server
2. Analista responde → Backend guarda → WorkManager sincroniza cada 15s → Operador recibe
3. Estados: ⏳ Enviando → ✓ Enviado → ✓✓ Leído

---

## ⚙️ Setup Rápido (3 pasos)

### 1. Backend (SQL Server + Laravel)
```sql
-- Crear respuestas predefinidas
INSERT INTO predefined_responses (id, mensaje, categoria, orden, activo) VALUES
(NEWID(), 'Todo en orden', 'Estado', 1, 1),
(NEWID(), 'Necesito asistencia', 'Urgente', 2, 1),
(NEWID(), 'Tráfico detenido', 'Tráfico', 3, 1);

-- Verificar operador existe
SELECT * FROM mt_operadores WHERE clave_operador = '12345';
```

### 2. App Android - Configurar IP
```kotlin
// Para emulador
const val BASE_URL = "http://10.0.2.2:8000/api/"

// Para dispositivo físico (usar tu IP)
const val BASE_URL = "http://192.168.1.100:8000/api/"
```

### 3. Instalar App
```bash
./gradlew installDebug
adb logcat | grep ChatFragment
```

---

## 🧪 Test Básico (2 minutos)

### Paso 1: Operador Envía
```
1. Login con operador 12345
2. Ir a pantalla "Chat"
3. Escribir: "Hola prueba"
4. Enviar → Verificar icono ⏳ → ✓
```

### Paso 2: Analista Responde (Postman)
```bash
curl -X POST http://localhost:8000/api/chat/send \
-H "Content-Type: application/json" \
-d '{
  "operator_code": "12345",
  "content": "Hola operador",
  "sender_type": "ANALISTA",
  "sender_id": "1"
}'
```

### Paso 3: Operador Recibe
```
1. Esperar 15 segundos (sync automático)
2. Verificar mensaje analista aparece
3. Verificar badge "1 sin leer"
4. Abrir chat → badge desaparece
```

---

## 🔍 Verificar Resultados

### Backend (SQL Server)
```sql
-- Ver última conversación
SELECT TOP 5 
    m.content, 
    m.sender_type, 
    m.created_at
FROM messages m
INNER JOIN conversations c ON c.id = m.conversation_id
WHERE c.operator_code = '12345'
ORDER BY m.created_at DESC;
```

### App (Room Database)
```bash
adb shell
sqlite3 /data/data/com.example.controloperador/databases/controloperador_database

SELECT content, sender_type, sync_status 
FROM chat_messages 
ORDER BY created_at DESC 
LIMIT 5;
```

---

## 📮 4 Endpoints API

### 1. Enviar Mensaje
```http
POST /api/chat/send
{
  "operator_code": "12345",
  "content": "Mensaje de prueba",
  "sender_type": "OPERADOR",
  "sender_id": "12345"
}
```

### 2. Obtener Mensajes del Día
```http
GET /api/chat/messages/today?operator_code=12345
```

### 3. Marcar como Leído
```http
POST /api/chat/mark-read
{
  "operator_code": "12345",
  "message_ids": ["uuid-1", "uuid-2"]
}
```

### 4. Respuestas Predefinidas
```http
GET /api/chat/predefined-responses
```

---

## ❌ Problemas Comunes

### Mensajes no sincronizan
```bash
# Verificar conexión
adb shell ping -c 3 8.8.8.8

# Ver logs
adb logcat | grep ChatSyncWorker

# Verificar BASE_URL correcta
# Emulador: 10.0.2.2
# Dispositivo: IP real (192.168.x.x)
```

### WorkManager no ejecuta
```bash
# Verificar app en foreground
# Verificar conexión a internet
# Ver estado
adb logcat | grep WorkManager
```

### Badge no actualiza
```sql
-- Verificar unread_count
SELECT unread_count FROM conversations WHERE operator_code = '12345';

-- Verificar mensajes no leídos
SELECT COUNT(*) FROM messages 
WHERE read_at IS NULL AND sender_type = 'ANALISTA';
```

---

## 📊 Checklist Rápido

- [ ] Operador envía → Backend recibe ✓
- [ ] Analista responde → Operador recibe en <15s ✓
- [ ] Badge "sin leer" funciona ✓
- [ ] Estados ⏳ → ✓ → ✓✓ funcionan ✓
- [ ] Respuestas predefinidas cargan ✓
- [ ] WorkManager ejecuta cada 15s ✓

---

## 📚 Documentación Completa

Ver **`TESTING_CHAT_GUIA_COMPLETA.md`** para:
- Arquitectura técnica detallada
- Flujos de testing completos
- Troubleshooting avanzado
- Queries SQL completos
- Testing con Postman
- Verificación de base de datos

---

## 🎯 Comandos Esenciales

```bash
# Compilar e instalar
./gradlew installDebug

# Ver logs
adb logcat | grep -E "(ChatFragment|ChatSyncWorker)"

# Acceder a Room
adb shell
sqlite3 /data/data/com.example.controloperador/databases/controloperador_database

# Limpiar base de datos (testing)
DELETE FROM chat_messages;
DELETE FROM conversations;
VACUUM;
```

---

## 🚀 Status

✅ **Android App**: COMPLETO - Listo para testing  
⏳ **Backend Laravel**: Requiere implementación de 4 endpoints  
📱 **Dispositivo**: Samsung SM-X115 (Android 15) - Instalado  

**Tiempo estimado de testing básico**: 5-10 minutos

---

**Última actualización**: 4 de Noviembre de 2025  
**Para más detalles**: Ver `TESTING_CHAT_GUIA_COMPLETA.md`
