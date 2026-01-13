package com.example.controloperador.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.controloperador.ControlOperadorApp
import com.example.controloperador.data.api.Result
import com.example.controloperador.data.api.model.chat.PredefinedResponse
import com.example.controloperador.data.database.chat.ChatMessage
import com.example.controloperador.data.database.chat.ChatRepository
import com.example.controloperador.data.database.chat.Conversation
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de chat/mensajes
 * Gestiona mensajes en tiempo real con sincronización bidireccional
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    private val chatRepository: ChatRepository = 
        (application as ControlOperadorApp).appContainer.chatRepository
    
    // Código del operador actual
    private val _operatorCode = MutableLiveData<String>()
    
    // Conversación actual
    private val _conversation = MutableLiveData<Conversation?>()
    val conversation: LiveData<Conversation?> = _conversation
    
    // Mensajes del día actual (se actualiza automáticamente desde Room)
    val todayMessages: LiveData<List<ChatMessage>> = _operatorCode.switchMap { operatorCode ->
        _conversation.switchMap { conversation ->
            if (conversation != null) {
                chatRepository.getTodayMessagesLive(conversation.id)
            } else {
                MutableLiveData(emptyList())
            }
        }
    }
    
    // Conteo de mensajes no leídos
    val unreadCount: LiveData<Int> = _conversation.switchMap { conversation ->
        if (conversation != null) {
            chatRepository.getUnreadCountLive(conversation.id)
        } else {
            MutableLiveData(0)
        }
    }
    
    // Estado de envío de mensaje
    private val _sendMessageState = MutableLiveData<SendMessageState>(SendMessageState.Idle)
    val sendMessageState: LiveData<SendMessageState> = _sendMessageState
    
    // Respuestas predefinidas dinámicas desde el servidor
    private val _predefinedResponses = MutableLiveData<List<PredefinedResponse>>()
    val predefinedResponses: LiveData<List<PredefinedResponse>> = _predefinedResponses
    
    // Estado de carga de respuestas predefinidas
    private val _responsesState = MutableLiveData<ResponsesState>(ResponsesState.Idle)
    val responsesState: LiveData<ResponsesState> = _responsesState
    
    /**
     * Inicializa el chat para un operador
     */
    fun initializeChat(operatorCode: String) {
        if (_operatorCode.value == operatorCode) {
            // Ya inicializado para este operador
            return
        }
        
        _operatorCode.value = operatorCode
        
        viewModelScope.launch {
            try {
                // Obtener o crear conversación
                val conversation = chatRepository.getOrCreateConversation(operatorCode)
                _conversation.value = conversation
                
                Log.d(TAG, "Chat initialized for operator: $operatorCode")
                
                // Cargar respuestas predefinidas
                loadPredefinedResponses()
                
                // Marcar mensajes como leídos al abrir el chat
                markAllMessagesAsRead()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing chat", e)
            }
        }
    }
    
    /**
     * Envía un mensaje de texto
     */
    fun sendMessage(content: String) {
        Log.d(TAG, "📤 sendMessage called with content: '$content'")
        
        val operatorCode = _operatorCode.value
        Log.d(TAG, "   Operator code: $operatorCode")
        
        if (operatorCode == null) {
            Log.e(TAG, "❌ Cannot send message: operatorCode is null")
            return
        }
        
        if (content.isBlank()) {
            Log.e(TAG, "❌ Cannot send message: content is blank")
            return
        }
        
        Log.d(TAG, "✅ Sending message to repository...")
        _sendMessageState.value = SendMessageState.Sending
        
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                operatorCode = operatorCode,
                content = content.trim()
            )
            
            Log.d(TAG, "📥 Repository result: $result")
            
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "✅ Message sent successfully")
                    _sendMessageState.value = SendMessageState.Success
                    
                    // Resetear a Idle después de un momento
                    kotlinx.coroutines.delay(500)
                    _sendMessageState.value = SendMessageState.Idle
                }
                
                is Result.Error -> {
                    Log.e(TAG, "❌ Error sending message: ${result.message}")
                    _sendMessageState.value = SendMessageState.Error(result.message)
                    
                    // Resetear después de mostrar error
                    kotlinx.coroutines.delay(3000)
                    _sendMessageState.value = SendMessageState.Idle
                }
                
                else -> {
                    // NetworkError o Timeout - el mensaje queda como PENDING
                    Log.w(TAG, "⚠️ Message saved as pending, will retry later")
                    _sendMessageState.value = SendMessageState.Success
                    
                    kotlinx.coroutines.delay(500)
                    _sendMessageState.value = SendMessageState.Idle
                }
            }
        }
    }
    
    /**
     * Envía una respuesta predefinida
     */
    fun sendPredefinedResponse(response: PredefinedResponse) {
        val operatorCode = _operatorCode.value ?: return
        
        _sendMessageState.value = SendMessageState.Sending
        
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                operatorCode = operatorCode,
                content = response.mensaje,
                isPredefinedResponse = true,
                predefinedResponseId = response.id
            )
            
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Predefined response sent successfully")
                    _sendMessageState.value = SendMessageState.Success
                    
                    kotlinx.coroutines.delay(500)
                    _sendMessageState.value = SendMessageState.Idle
                }
                
                is Result.Error -> {
                    Log.e(TAG, "Error sending predefined response: ${result.message}")
                    _sendMessageState.value = SendMessageState.Error(result.message)
                    
                    kotlinx.coroutines.delay(3000)
                    _sendMessageState.value = SendMessageState.Idle
                }
                
                else -> {
                    _sendMessageState.value = SendMessageState.Success
                    kotlinx.coroutines.delay(500)
                    _sendMessageState.value = SendMessageState.Idle
                }
            }
        }
    }
    
    /**
     * Carga las respuestas predefinadas desde el servidor
     */
    fun loadPredefinedResponses() {
        viewModelScope.launch {
            _responsesState.value = ResponsesState.Loading
            
            val result = chatRepository.getPredefinedResponses()
            
            when (result) {
                is Result.Success -> {
                    _predefinedResponses.value = result.data
                    _responsesState.value = ResponsesState.Success
                    Log.d(TAG, "Loaded ${result.data.size} predefined responses")
                }
                
                is Result.Error -> {
                    Log.e(TAG, "Error loading predefined responses: ${result.message}")
                    _responsesState.value = ResponsesState.Error(result.message)
                    // Mantener respuestas anteriores si las hay
                }
                
                else -> {
                    Log.w(TAG, "Network error loading predefined responses")
                    _responsesState.value = ResponsesState.Error("Sin conexión")
                }
            }
        }
    }
    
    /**
     * Marca todos los mensajes del día como leídos
     */
    fun markAllMessagesAsRead() {
        val conversationId = _conversation.value?.id ?: return
        
        viewModelScope.launch {
            chatRepository.markAllTodayAsRead(conversationId)
            Log.d(TAG, "Marked all messages as read")
        }
    }
    
    /**
     * Sincroniza mensajes inmediatamente (al abrir el chat)
     */
    fun syncMessagesNow() {
        val conversationId = _conversation.value?.id ?: return
        val operatorCode = _operatorCode.value ?: return
        
        viewModelScope.launch {
            // Reintentar mensajes pendientes
            val retriedCount = chatRepository.retryPendingMessages(conversationId, operatorCode)
            if (retriedCount > 0) {
                Log.d(TAG, "Retried $retriedCount pending messages")
            }
            
            // Obtener mensajes nuevos del servidor
            val result = chatRepository.fetchNewMessages(conversationId, operatorCode)
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Sync completed: ${result.data} new messages fetched")
                }
                is Result.Error -> {
                    Log.e(TAG, "Sync error: ${result.message}")
                }
                else -> {
                    Log.w(TAG, "Network issue during sync")
                }
            }
        }
    }
    
    /**
     * Reintenta enviar mensajes pendientes manualmente
     */
    fun retryPendingMessages() {
        val conversationId = _conversation.value?.id ?: return
        val operatorCode = _operatorCode.value ?: return
        
        viewModelScope.launch {
            val count = chatRepository.retryPendingMessages(conversationId, operatorCode)
            Log.d(TAG, "Retried $count pending messages")
        }
    }
}

/**
 * Estados del envío de mensaje
 */
sealed class SendMessageState {
    object Idle : SendMessageState()
    object Sending : SendMessageState()
    object Success : SendMessageState()
    data class Error(val message: String) : SendMessageState()
}

/**
 * Estados de carga de respuestas predefinidas
 */
sealed class ResponsesState {
    object Idle : ResponsesState()
    object Loading : ResponsesState()
    object Success : ResponsesState()
    data class Error(val message: String) : ResponsesState()
}
