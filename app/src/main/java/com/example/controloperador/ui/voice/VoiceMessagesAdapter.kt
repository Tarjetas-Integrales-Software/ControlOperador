package com.example.controloperador.ui.voice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.api.model.VoiceMessageDetail
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adapter para mostrar los mensajes de voz dentro de una conversación.
 * Utiliza el modelo VoiceMessageDetail del nuevo API.
 */
class VoiceMessagesAdapter(
    private val onPlayClick: (VoiceMessageDetail) -> Unit
) : RecyclerView.Adapter<VoiceMessagesAdapter.VoiceMessageViewHolder>() {

    private var messages: List<VoiceMessageDetail> = emptyList()
    private var playingMessageId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_message_full, parent, false)
        return VoiceMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoiceMessageViewHolder, position: Int) {
        val message = messages[position]
        val isPlaying = message.id.toString() == playingMessageId
        holder.bind(message, isPlaying, onPlayClick)
    }

    override fun getItemCount(): Int = messages.size

    /**
     * Actualiza la lista de mensajes y notifica cambios
     */
    fun updateMessages(newMessages: List<VoiceMessageDetail>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    /**
     * Establece qué mensaje está siendo reproducido actualmente
     */
    fun setPlayingMessage(messageId: String?) {
        val oldPlayingId = playingMessageId
        playingMessageId = messageId
        
        // Actualizar ambos items (el que estaba reproduciéndose y el nuevo)
        messages.forEachIndexed { index, message ->
            if (message.id.toString() == oldPlayingId || message.id.toString() == messageId) {
                notifyItemChanged(index)
            }
        }
    }

    class VoiceMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val voiceIcon: ImageView = itemView.findViewById(R.id.voiceIcon)
        private val unplayedIndicator: View = itemView.findViewById(R.id.unplayedIndicator)
        private val voiceDuration: TextView = itemView.findViewById(R.id.voiceDuration)
        private val voiceTimestamp: TextView = itemView.findViewById(R.id.voiceTimestamp)
        private val playButton: FloatingActionButton = itemView.findViewById(R.id.playButton)

        fun bind(
            message: VoiceMessageDetail,
            isPlaying: Boolean,
            onPlayClick: (VoiceMessageDetail) -> Unit
        ) {
            // Mostrar indicador de no leído si aplica
            unplayedIndicator.visibility = if (message.isRead) View.GONE else View.VISIBLE
            
            // Formatear duración usando la propiedad helper
            voiceDuration.text = message.formattedDuration
            
            // Formatear timestamp desde createdAt
            val timestamp = parseDateTime(message.createdAt)
            voiceTimestamp.text = getRelativeTime(timestamp)
            
            // Cambiar icono según estado
            playButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            
            // Click en botón de reproducción
            playButton.setOnClickListener {
                onPlayClick(message)
            }
        }

        private fun parseDateTime(dateTimeString: String): Long {
            return try {
                // El formato del backend es: "2025-01-30T14:30:00.000000Z"
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                // Remover microsegundos y Z si existen
                val cleanDateTime = dateTimeString.substringBefore(".").substringBefore("Z")
                format.parse(cleanDateTime)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            
            return when {
                minutes < 1 -> itemView.context.getString(R.string.time_just_now)
                minutes < 60 -> itemView.context.getString(R.string.time_minutes_ago, minutes)
                hours < 24 -> itemView.context.getString(R.string.time_hours_ago, hours)
                else -> itemView.context.getString(R.string.time_days_ago, days)
            }
        }
    }
}
