# 🎵 Archivos de Audio para Mensajes de Voz

Esta carpeta contiene los archivos de audio (.ogg o .mp3) utilizados en la app para reproducir mensajes de voz.

## 📁 Ubicación
```
app/src/main/res/raw/
```

## 🔧 Cómo agregar archivos de audio

### 1. Formatos soportados
- **.ogg** (recomendado para Android)
- **.mp3**
- **.m4a**
- **.wav**

### 2. Agregar archivo
1. Copia tu archivo de audio (ej: `voice_message_1.ogg`)
2. Pega el archivo directamente en `app/src/main/res/raw/`
3. El nombre debe ser en minúsculas y sin espacios (usa guiones bajos)
   - ✅ Correcto: `voice_message_1.ogg`
   - ❌ Incorrecto: `Voice Message 1.ogg`

### 3. Usar el audio en el código

#### Opción A: Desde res/raw/ (archivo incluido en la app)
```kotlin
// En MessageRepository.kt
VoiceMessage(
    id = "v1",
    audioUrl = null,
    audioFilePath = "android.resource://com.example.controloperador/raw/voice_message_1",
    duration = 45, // Obtener con AudioPlayerHelper.getAudioDuration()
    timestamp = Date(),
    senderName = "Central de Control",
    isPlayed = false
)
```

#### Opción B: Desde URL externa (servidor)
```kotlin
VoiceMessage(
    id = "v1",
    audioUrl = "https://tu-servidor.com/audios/mensaje1.ogg",
    audioFilePath = null,
    duration = 45,
    timestamp = Date(),
    senderName = "Central de Control",
    isPlayed = false
)
```

#### Opción C: Desde archivo local (descargado)
```kotlin
VoiceMessage(
    id = "v1",
    audioUrl = null,
    audioFilePath = "/storage/emulated/0/Download/mensaje.ogg",
    duration = 45,
    timestamp = Date(),
    senderName = "Central de Control",
    isPlayed = false
)
```

## 🎯 Obtener duración del audio

Para obtener automáticamente la duración de un archivo:

```kotlin
// Desde res/raw/
val audioPlayer = AudioPlayerHelper(context)
val duration = audioPlayer.getAudioDuration(R.raw.voice_message_1)
// duration en segundos

// Desde path
val duration = audioPlayer.getAudioDurationFromPath("android.resource://...")
```

## 📊 Ejemplo completo

```kotlin
// MessageRepository.kt
private val voiceMessages = mutableListOf(
    VoiceMessage(
        id = "v1",
        audioUrl = null,
        audioFilePath = "android.resource://com.example.controloperador/raw/mensaje_central",
        duration = 45,
        timestamp = Date(System.currentTimeMillis() - 1800000),
        senderName = "Central de Control",
        isPlayed = false,
        transcription = "Favor de confirmar recepción de pasajeros."
    ),
    VoiceMessage(
        id = "v2",
        audioUrl = "https://api.empresa.com/audios/mensaje_supervisor_001.ogg",
        audioFilePath = null,
        duration = 32,
        timestamp = Date(System.currentTimeMillis() - 5400000),
        senderName = "Supervisor",
        isPlayed = true,
        transcription = "Buen trabajo en la ruta de hoy."
    )
)
```

## 🎼 Archivos de ejemplo actuales

| Archivo | Descripción | Duración | Usado en |
|---------|-------------|----------|----------|
| *(vacío)* | Agrega tus archivos aquí | - | - |

## 🚀 Pasos siguientes

1. **Agregar audios de prueba**: Copia archivos .ogg o .mp3 a esta carpeta
2. **Actualizar MessageRepository**: Configura `audioFilePath` con el path correcto
3. **Probar reproducción**: Ejecuta la app y ve a la sección de Mensajes de Voz
4. **Producción**: Configurar para descargar desde API Laravel

## 🔗 Integración con Backend Laravel

En producción, los mensajes de voz vendrán desde el backend:

```kotlin
// Desde API
VoiceMessage(
    id = "v123",
    audioUrl = "https://tu-dominio.com/storage/voice_messages/msg_123.ogg",
    audioFilePath = null, // Se descargará y cacheará localmente
    duration = response.duration,
    timestamp = Date(response.created_at),
    senderName = response.sender_name,
    isPlayed = false,
    transcription = response.transcription
)
```

## 📝 Notas

- Los archivos en `res/raw/` se incluyen en el APK
- Para archivos grandes, usar URL externa y cachear localmente
- La duración se obtiene automáticamente al reproducir
- MediaPlayer soporta múltiples formatos de audio
