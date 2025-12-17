package com.example.controloperador.ui.voice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.api.model.VoiceMessageData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoiceMessageAdapter(
    private val onPlayClick: (VoiceMessageData) -> Unit
) : RecyclerView.Adapter<VoiceMessageAdapter.VoiceMessageViewHolder>() {

    private var messages: List<VoiceMessageData> = emptyList()
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

    fun updateMessages(newMessages: List<VoiceMessageData>) {
        messages = newMessages
        notifyDataSetChanged()
    }

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
            message: VoiceMessageData,
            isPlaying: Boolean,
            onPlayClick: (VoiceMessageData) -> Unit
        ) {
            // Ocultar indicador de no reproducido (ya que ahora viene del servidor)
            unplayedIndicator.visibility = View.GONE
            
            // Formatear duración (si está disponible)
            voiceDuration.text = message.duracion?.let { formatDuration(it) } ?: "0:00"
            
            // Formatear timestamp desde la fecha y hora del mensaje
            val timestamp = parseDateTime(message.fecha, message.hora)
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

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%d:%02d", minutes, secs)
        }
        
        private fun parseDateTime(fecha: String, hora: String): Long {
            return try {
                val dateTimeString = "$fecha $hora"
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                format.parse(dateTimeString)?.time ?: System.currentTimeMillis()
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
