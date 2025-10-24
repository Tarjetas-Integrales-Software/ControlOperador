# 🎵 Guía Rápida: Reproducción de Audio en ControlOperador

## ✅ ¿Qué se implementó?

1. ✅ **AudioPlayerHelper**: Clase completa con MediaPlayer para reproducir audios
2. ✅ **Integración en VoiceMessagesFragment**: Control de play/pause/stop
3. ✅ **Carpeta res/raw/**: Ubicación para archivos de audio (.ogg, .mp3)
4. ✅ **Obtención automática de duración**: El AudioPlayer detecta los minutos y segundos
5. ✅ **Manejo del lifecycle**: Cleanup automático para evitar memory leaks

## 📁 ¿Dónde agregar archivos de audio?

### Ubicación:
```
app/src/main/res/raw/
```

### Pasos para agregar un audio:
1. **Copia** tu archivo `.ogg` o `.mp3`
2. **Pégalo** en `app/src/main/res/raw/`
3. **Nombre** debe ser en minúsculas sin espacios:
   - ✅ `mensaje_central.ogg`
   - ✅ `voice_1.mp3`
   - ❌ `Mensaje Central.ogg`

## 🔧 Cómo usar los audios

### Opción 1: Archivo en res/raw/ (recomendado para desarrollo)

```kotlin
// MessageRepository.kt
VoiceMessage(
    id = "v1",
    audioUrl = null,
    audioFilePath = "android.resource://com.example.controloperador/raw/mensaje_central",
    duration = 45, // Se obtiene automáticamente al reproducir
    timestamp = Date(),
    senderName = "Central de Control",
    isPlayed = false
)
```

### Opción 2: URL externa (para producción con backend)

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

## 🎯 Obtener duración de un audio

```kotlin
val audioPlayer = AudioPlayerHelper(context)

// Desde res/raw/
val duration = audioPlayer.getAudioDuration(R.raw.mensaje_central)
println("Duración: $duration segundos") // Ej: 45

// Desde path/URL
val duration = audioPlayer.getAudioDurationFromPath("android.resource://...")
```

## 🎮 Funcionalidades implementadas

### En VoiceMessagesFragment:

- ✅ **Play**: Reproduce el audio cuando se presiona el botón
- ✅ **Pause**: Pausa el audio si se presiona nuevamente
- ✅ **Resume**: Continúa desde donde se pausó
- ✅ **Stop**: Detiene completamente al cambiar de mensaje
- ✅ **Icono dinámico**: Cambia entre ▶️ y ⏸️ según el estado
- ✅ **Marcar como reproducido**: Automáticamente al iniciar
- ✅ **Toast de duración**: Muestra segundos totales al reproducir
- ✅ **Manejo de errores**: Toast si el archivo no existe o falla

## 📱 Cómo probar

### 1. Agregar audio de prueba

Descarga un audio de prueba o usa uno propio y:

```bash
# Copia el archivo a la carpeta raw
cp ~/Downloads/audio_prueba.ogg app/src/main/res/raw/mensaje_prueba.ogg
```

### 2. Actualizar MessageRepository

```kotlin
// app/src/main/java/.../data/MessageRepository.kt

VoiceMessage(
    id = "v1",
    audioUrl = null,
    audioFilePath = "android.resource://com.example.controloperador/raw/mensaje_prueba",
    duration = 30, // Se calculará automáticamente
    timestamp = Date(System.currentTimeMillis() - 1800000),
    senderName = "Central de Control",
    isPlayed = false,
    transcription = "Mensaje de prueba"
)
```

### 3. Ejecutar la app

1. Compila el proyecto
2. Ve a la sección **"Notas de Voz"** desde el drawer
3. Presiona el botón **▶️** en cualquier mensaje
4. Verás el audio reproduciéndose con el icono cambiando a **⏸️**

## 🚀 Ejemplo completo paso a paso

### 1. Agregar archivo de audio

Coloca `central_control.ogg` en `app/src/main/res/raw/`

### 2. Modificar MessageRepository.kt

```kotlin
private val voiceMessages = mutableListOf(
    VoiceMessage(
        id = "v1",
        audioUrl = null,
        audioFilePath = "android.resource://com.example.controloperador/raw/central_control",
        duration = 45,
        timestamp = Date(System.currentTimeMillis() - 1800000),
        senderName = "Central de Control",
        isPlayed = false,
        transcription = "Favor de confirmar recepción de pasajeros en parada 15."
    )
)
```

### 3. Probar en el emulador

- Abre la app
- Login con código `54321`
- Ve a "Notas de Voz"
- Presiona ▶️ en el mensaje
- ¡El audio se reproduce!

## 🐛 Solución de problemas

### ❌ "No hay archivo de audio disponible"
**Causa**: El `audioFilePath` y `audioUrl` están en `null`

**Solución**: Asegúrate de configurar al menos uno:
```kotlin
audioFilePath = "android.resource://com.example.controloperador/raw/tu_audio"
```

### ❌ "Error al reproducir: ..."
**Causa**: El archivo no existe o el nombre es incorrecto

**Solución**:
1. Verifica que el archivo esté en `res/raw/`
2. Verifica el nombre (sin extensión en el resource ID)
3. Sincroniza Gradle (Build > Clean Project)

### ❌ El botón no responde
**Causa**: Posible error de compilación

**Solución**:
1. Build > Clean Project
2. Build > Rebuild Project
3. Verifica errores en Logcat

## 📊 Formato del audio recomendado

Para mejor rendimiento:

- **Formato**: OGG Vorbis
- **Bitrate**: 64-128 kbps
- **Sample Rate**: 44.1 kHz
- **Canales**: Mono (mejor para voz)

### Convertir audio a OGG:

Usando FFmpeg (línea de comandos):
```bash
ffmpeg -i audio_original.mp3 -c:a libvorbis -q:a 4 audio_salida.ogg
```

O usar herramientas online:
- https://convertio.co/es/mp3-ogg/
- https://online-audio-converter.com/es/

## 🔗 Integración futura con Laravel

Cuando el backend esté listo:

```kotlin
// Desde API Response
data class VoiceMessageResponse(
    val id: String,
    val audio_url: String,
    val duration: Int,
    val sender_name: String,
    val transcription: String?,
    val created_at: String
)

// Mapear a VoiceMessage
VoiceMessage(
    id = response.id,
    audioUrl = response.audio_url, // URL completa del servidor
    audioFilePath = null,
    duration = response.duration,
    timestamp = parseDate(response.created_at),
    senderName = response.sender_name,
    isPlayed = false,
    transcription = response.transcription
)
```

## 📝 Archivos modificados

1. ✅ `AudioPlayerHelper.kt` (NUEVO)
2. ✅ `VoiceMessagesFragment.kt` (ACTUALIZADO)
3. ✅ `VoiceMessageAdapter.kt` (FIX de typo)
4. ✅ `MessageRepository.kt` (COMENTARIOS)
5. ✅ `res/raw/` (CARPETA CREADA)
6. ✅ `res/raw/README.md` (DOCUMENTACIÓN)

## ✨ ¡Listo para usar!

Ahora puedes agregar tus archivos de audio en `res/raw/` y probar la reproducción completa. El sistema detecta automáticamente la duración y maneja play/pause/stop correctamente.
