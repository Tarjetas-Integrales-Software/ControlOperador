package com.example.controloperador.ui.chat

import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.database.chat.ChatMessage
import com.example.controloperador.data.database.chat.SenderType
import com.example.controloperador.data.database.chat.SyncStatus
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para mostrar mensajes de chat (texto y voz) con estados de sincronización
 */
class ChatAdapter(
    private val currentOperatorCode: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {
    
    companion object {
        private const val TAG = "ChatAdapter"
        
        // View types para texto
        private const val VIEW_TYPE_TEXT_SENT = 1
        private const val VIEW_TYPE_TEXT_RECEIVED = 2
        
        // View type para voz (solo recibidos, operador no envía audio)
        private const val VIEW_TYPE_VOICE_RECEIVED = 4
        
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    
    // MediaPlayer compartido para reproducir audios
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingMessageId: String? = null

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSent = message.senderType == SenderType.OPERADOR && 
                     message.senderId == currentOperatorCode
        
        return when {
            // Los mensajes de voz solo pueden ser recibidos (analista)
            message.isVoiceMessage() -> VIEW_TYPE_VOICE_RECEIVED
            isSent -> VIEW_TYPE_TEXT_SENT
            else -> VIEW_TYPE_TEXT_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TEXT_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_TEXT_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_VOICE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_voice_message_received, parent, false)
                VoiceReceivedViewHolder(view, this)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is VoiceReceivedViewHolder -> holder.bind(message)
        }
    }
    
    /**
     * Reproduce o pausa un audio
     */
    fun playPauseAudio(message: ChatMessage, button: MaterialButton) {
        if (message.audioUrl.isNullOrEmpty()) {
            Log.e(TAG, "No audio URL for message: ${message.id}")
            return
        }
        
        Log.d(TAG, "playPauseAudio called - messageId: ${message.id}, audioUrl: ${message.audioUrl}")
        
        if (currentPlayingMessageId == message.id && mediaPlayer?.isPlaying == true) {
            // Pausar audio actual
            mediaPlayer?.pause()
            button.setIconResource(R.drawable.ic_play)
            Log.d(TAG, "Paused audio: ${message.id}")
        } else if (currentPlayingMessageId == message.id && mediaPlayer != null) {
            // Reanudar audio pausado
            try {
                mediaPlayer?.start()
                button.setIconResource(R.drawable.ic_pause)
                Log.d(TAG, "Resumed audio: ${message.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming audio", e)
                stopCurrentAudio()
                button.setIconResource(R.drawable.ic_play)
            }
        } else {
            // Reproducir nuevo audio
            stopCurrentAudio()
            
            Log.d(TAG, "Starting new audio playback from URL: ${message.audioUrl}")
            
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(message.audioUrl)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        currentPlayingMessageId = message.id
                        button.setIconResource(R.drawable.ic_pause)
                        Log.d(TAG, "Playing audio: ${message.id}")
                    }
                    setOnCompletionListener {
                        button.setIconResource(R.drawable.ic_play)
                        currentPlayingMessageId = null
                        Log.d(TAG, "Audio completed: ${message.id}")
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra, url=${message.audioUrl}")
                        button.setIconResource(R.drawable.ic_play)
                        currentPlayingMessageId = null
                        // Liberar el MediaPlayer con error
                        release()
                        mediaPlayer = null
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up MediaPlayer for URL: ${message.audioUrl}", e)
                button.setIconResource(R.drawable.ic_play)
            }
        }
    }
    
    /**
     * Detiene la reproducción actual
     */
    private fun stopCurrentAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentPlayingMessageId = null
    }
    
    /**
     * Limpia recursos cuando el adapter es destruido
     */
    fun onDestroy() {
        stopCurrentAudio()
    }

    /**
     * ViewHolder para mensajes enviados (OPERADOR)
     */
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.messageContent)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)

        fun bind(message: ChatMessage) {
            messageContent.text = message.content
            
            // Formatear hora
            val timeText = TIME_FORMAT.format(message.createdAt)
            
            // Agregar icono de estado según sync_status y read_at
            val statusText = when {
                message.syncStatus == SyncStatus.PENDING -> "⏳" // Enviando
                message.syncStatus == SyncStatus.FAILED -> "❌" // Error
                message.readAt != null -> "✓✓" // Leído
                message.syncStatus == SyncStatus.SENT -> "✓" // Enviado
                else -> ""
            }
            
            // Mostrar hora + estado en messageTime
            messageTime.text = "$timeText $statusText"
        }
    }

    /**
     * ViewHolder para mensajes recibidos (ANALISTA)
     */
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.messageContent)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)

        fun bind(message: ChatMessage) {
            messageContent.text = message.content
            messageTime.text = TIME_FORMAT.format(message.createdAt)
            
            // Los mensajes de analista no necesitan mostrar nombre 
            // (todos aparecen como "Soporte" desde el servidor)
        }
    }
    
    /**
     * ViewHolder para mensajes de voz recibidos (solo analista envía audio)
     */
    class VoiceReceivedViewHolder(itemView: View, private val adapter: ChatAdapter) : RecyclerView.ViewHolder(itemView) {
        private val playPauseButton: MaterialButton = itemView.findViewById(R.id.playPauseButton)
        private val audioDuration: TextView = itemView.findViewById(R.id.audioDuration)
        private val audioLabel: TextView = itemView.findViewById(R.id.audioLabel)
        private val timestamp: TextView = itemView.findViewById(R.id.messageTime)
        
        fun bind(message: ChatMessage) {
            // Configurar duración
            val duration = message.getFormattedDuration()
            audioDuration.text = duration
            
            // Configurar label
            audioLabel.text = "🎙️ Mensaje de voz"
            
            // Configurar timestamp (mensajes recibidos no tienen estado)
            timestamp.text = TIME_FORMAT.format(message.createdAt)
            
            // Configurar botón play/pause
            val isCurrentlyPlaying = adapter.currentPlayingMessageId == message.id && adapter.mediaPlayer?.isPlaying == true
            playPauseButton.setIconResource(
                if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            
            playPauseButton.setOnClickListener {
                adapter.playPauseAudio(message, playPauseButton)
            }
        }
    }
}

/**
 * DiffUtil Callback para comparar mensajes eficientemente
 */
class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        // Comparar por ID único del mensaje
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        // Comparar todos los campos relevantes
        return oldItem.content == newItem.content &&
                oldItem.syncStatus == newItem.syncStatus &&
                oldItem.readAt == newItem.readAt &&
                oldItem.createdAt == newItem.createdAt
    }
}
