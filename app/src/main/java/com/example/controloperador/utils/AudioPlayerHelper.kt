package com.example.controloperador.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.RawRes

/**
 * Helper class para manejar reproducción de audio con MediaPlayer
 * Soporta archivos de res/raw/ y URIs externas
 */
class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioId: String? = null
    
    // Callbacks
    private var onCompletionListener: ((String) -> Unit)? = null
    private var onErrorListener: ((String, String) -> Unit)? = null
    private var onPreparedListener: ((String, Int) -> Unit)? = null

    /**
     * Reproduce un archivo de audio desde res/raw/
     * @param audioResId ID del recurso (R.raw.audio_file)
     * @param messageId ID del mensaje para tracking
     * @return true si inició correctamente
     */
    fun playAudio(@RawRes audioResId: Int, messageId: String): Boolean {
        try {
            stop() // Detener reproducción anterior
            
            mediaPlayer = MediaPlayer.create(context, audioResId)
            currentAudioId = messageId
            
            mediaPlayer?.apply {
                setOnCompletionListener {
                    onCompletionListener?.invoke(messageId)
                    release()
                    mediaPlayer = null
                    currentAudioId = null
                }
                
                setOnErrorListener { _, what, extra ->
                    onErrorListener?.invoke(messageId, "Error: $what, Extra: $extra")
                    true
                }
                
                // Notificar duración cuando esté listo
                val duration = this.duration / 1000 // milisegundos a segundos
                onPreparedListener?.invoke(messageId, duration)
                
                start()
            }
            
            return true
        } catch (e: Exception) {
            onErrorListener?.invoke(messageId, e.message ?: "Error desconocido")
            return false
        }
    }
    
    /**
     * Reproduce un archivo de audio desde URI o file path
     * @param audioPath Path local o URI del audio
     * @param messageId ID del mensaje para tracking
     * @return true si inició correctamente
     */
    fun playAudioFromPath(audioPath: String, messageId: String): Boolean {
        try {
            stop() // Detener reproducción anterior
            
            mediaPlayer = MediaPlayer().apply {
                // Para URLs HTTP, usar setDataSource(String) directamente
                // Para files locales, usar setDataSource(Context, Uri)
                if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
                    setDataSource(audioPath)
                } else {
                    setDataSource(context, Uri.parse(audioPath))
                }
                
                setOnPreparedListener {
                    val duration = this.duration / 1000
                    onPreparedListener?.invoke(messageId, duration)
                    start()
                }
                
                setOnCompletionListener {
                    onCompletionListener?.invoke(messageId)
                    release()
                    mediaPlayer = null
                    currentAudioId = null
                }
                
                setOnErrorListener { _, what, extra ->
                    onErrorListener?.invoke(messageId, "Error: $what, Extra: $extra")
                    true
                }
                
                prepareAsync()
            }
            
            currentAudioId = messageId
            return true
        } catch (e: Exception) {
            onErrorListener?.invoke(messageId, e.message ?: "Error desconocido")
            return false
        }
    }

    /**
     * Pausa la reproducción actual
     */
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    /**
     * Reanuda la reproducción pausada
     */
    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    /**
     * Detiene y libera el reproductor
     */
    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentAudioId = null
    }

    /**
     * Verifica si hay audio reproduciéndose
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    /**
     * Obtiene el ID del audio actual
     */
    fun getCurrentAudioId(): String? {
        return currentAudioId
    }

    /**
     * Obtiene la posición actual de reproducción (segundos)
     */
    fun getCurrentPosition(): Int {
        return (mediaPlayer?.currentPosition ?: 0) / 1000
    }

    /**
     * Obtiene la duración total del audio (segundos)
     */
    fun getDuration(): Int {
        return (mediaPlayer?.duration ?: 0) / 1000
    }

    /**
     * Establece el listener de finalización
     */
    fun setOnCompletionListener(listener: (String) -> Unit) {
        onCompletionListener = listener
    }

    /**
     * Establece el listener de errores
     */
    fun setOnErrorListener(listener: (String, String) -> Unit) {
        onErrorListener = listener
    }

    /**
     * Establece el listener cuando el audio está listo
     */
    fun setOnPreparedListener(listener: (String, Int) -> Unit) {
        onPreparedListener = listener
    }

    /**
     * Obtiene la duración de un archivo de audio sin reproducirlo
     * @param audioResId ID del recurso de audio
     * @return Duración en segundos
     */
    fun getAudioDuration(@RawRes audioResId: Int): Int {
        var duration = 0
        var tempPlayer: MediaPlayer? = null
        try {
            tempPlayer = MediaPlayer.create(context, audioResId)
            duration = tempPlayer?.duration?.div(1000) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempPlayer?.release()
        }
        return duration
    }
    
    /**
     * Obtiene la duración de un archivo de audio desde path sin reproducirlo
     * @param audioPath Path del archivo de audio
     * @return Duración en segundos
     */
    fun getAudioDurationFromPath(audioPath: String): Int {
        var duration = 0
        var tempPlayer: MediaPlayer? = null
        try {
            tempPlayer = MediaPlayer().apply {
                // Para URLs HTTP, usar setDataSource(String) directamente
                if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
                    setDataSource(audioPath)
                } else {
                    setDataSource(context, Uri.parse(audioPath))
                }
                prepare()
            }
            duration = tempPlayer?.duration?.div(1000) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempPlayer?.release()
        }
        return duration
    }

    /**
     * Limpieza al destruir
     * IMPORTANTE: Llamar desde onDestroyView del Fragment
     */
    fun release() {
        stop()
        onCompletionListener = null
        onErrorListener = null
        onPreparedListener = null
    }
}
