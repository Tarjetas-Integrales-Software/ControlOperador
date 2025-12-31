package com.example.controloperador.ui.voice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.api.model.VoiceConversation
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para mostrar la lista de conversaciones de mensajes de voz.
 * Cada conversación muestra el nombre del analista, la última fecha de mensaje,
 * la duración del último audio y un contador de mensajes no leídos.
 */
class VoiceConversationsAdapter(
    private val onConversationClick: (VoiceConversation) -> Unit
) : RecyclerView.Adapter<VoiceConversationsAdapter.ConversationViewHolder>() {

    private var conversations: List<VoiceConversation> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation, onConversationClick)
    }

    override fun getItemCount(): Int = conversations.size

    /**
     * Actualiza la lista de conversaciones y notifica cambios
     */
    fun updateConversations(newConversations: List<VoiceConversation>) {
        conversations = newConversations
        notifyDataSetChanged()
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val conversationCard: CardView = itemView.findViewById(R.id.conversationCard)
        private val analystName: TextView = itemView.findViewById(R.id.analystName)
        private val lastMessageDate: TextView = itemView.findViewById(R.id.lastMessageDate)
        private val lastDuration: TextView = itemView.findViewById(R.id.lastDuration)
        private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
        private val totalMessages: TextView = itemView.findViewById(R.id.totalMessages)

        fun bind(
            conversation: VoiceConversation,
            onConversationClick: (VoiceConversation) -> Unit
        ) {
            // Nombre del analista
            analystName.text = conversation.analystName ?: "Sin nombre"

            // Fecha del último mensaje (relativa)
            lastMessageDate.text = formatRelativeDate(conversation.lastMessageDate)

            // Duración del último audio
            lastDuration.text = formatDuration(conversation.lastAudioDuration)

            // Total de mensajes
            totalMessages.text = "${conversation.totalMessages} mensaje${if (conversation.totalMessages != 1) "s" else ""}"

            // Badge de mensajes no leídos
            if (conversation.unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (conversation.unreadCount > 99) {
                    "99+"
                } else {
                    conversation.unreadCount.toString()
                }
                
                // Resaltar conversación con mensajes no leídos
                conversationCard.cardElevation = 8f
            } else {
                unreadBadge.visibility = View.GONE
                conversationCard.cardElevation = 4f
            }

            // Click en la tarjeta para abrir conversación
            conversationCard.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        /**
         * Formatea la fecha a formato relativo (Hoy, Ayer, o fecha)
         */
        private fun formatRelativeDate(dateString: String): String {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val messageDate = format.parse(dateString)
                val now = Date()

                val diff = now.time - (messageDate?.time ?: now.time)
                val days = diff / (1000 * 60 * 60 * 24)

                when {
                    days == 0L -> {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        "Hoy ${messageDate?.let { timeFormat.format(it) } ?: ""}"
                    }
                    days == 1L -> "Ayer"
                    days < 7 -> {
                        val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
                        messageDate?.let { dayFormat.format(it) } ?: dateString
                    }
                    else -> {
                        val shortFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        messageDate?.let { shortFormat.format(it) } ?: dateString
                    }
                }
            } catch (e: Exception) {
                dateString
            }
        }

        /**
         * Formatea la duración en segundos a formato mm:ss
         */
        private fun formatDuration(seconds: Int?): String {
            if (seconds == null || seconds == 0) return "0:00"
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%d:%02d", minutes, secs)
        }
    }
}
