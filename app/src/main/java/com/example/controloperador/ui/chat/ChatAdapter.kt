package com.example.controloperador.ui.chat

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para mostrar mensajes de chat con estados de sincronización
 */
class ChatAdapter(
    private val currentOperatorCode: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderType == SenderType.OPERADOR && 
                   message.senderId == currentOperatorCode) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_text_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_text_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
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
