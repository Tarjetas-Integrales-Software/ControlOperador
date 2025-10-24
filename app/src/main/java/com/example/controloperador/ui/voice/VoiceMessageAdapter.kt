package com.example.controloperador.ui.voice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.model.VoiceMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit

class VoiceMessageAdapter(
    private val onPlayClick: (VoiceMessage) -> Unit
) : RecyclerView.Adapter<VoiceMessageAdapter.VoiceMessageViewHolder>() {

    private var messages: List<VoiceMessage> = emptyList()
    private var playingMessageId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_message_full, parent, false)
        return VoiceMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoiceMessageViewHolder, position: Int) {
        val message = messages[position]
        val isPlaying = message.id == playingMessageId
        holder.bind(message, isPlaying, onPlayClick)
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<VoiceMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun setPlayingMessage(messageId: String?) {
        val oldPlayingId = playingMessageId
        playingMessageId = messageId
        
        // Actualizar ambos items (el que estaba reproduciéndose y el nuevo)
        messages.forEachIndexed { index, message ->
            if (message.id == oldPlayingId || message.id == messageId) {
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
            message: VoiceMessage,
            isPlaying: Boolean,
            onPlayClick: (VoiceMessage) -> Unit
        ) {
            // Mostrar/ocultar indicador de no reproducido
            unplayedIndicator.visibility = if (message.isPlayed) View.GONE else View.VISIBLE
            
            // Formatear duración
            voiceDuration.text = formatDuration(message.duration)
            
            // Formatear timestamp
            voiceTimestamp.text = getRelativeTime(message.timestamp.time)
            
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
