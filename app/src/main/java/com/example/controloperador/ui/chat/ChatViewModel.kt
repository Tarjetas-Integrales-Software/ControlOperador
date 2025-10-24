package com.example.controloperador.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controloperador.data.api.MessagesRepository
import com.example.controloperador.data.api.Result
import com.example.controloperador.data.api.model.TextMessage
import com.example.controloperador.data.api.model.VoiceMessage
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de chat/mensajes
 */
class ChatViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    private val messagesRepository = MessagesRepository()
    
    // Estado de carga de mensajes predeterminados
    private val _messagesState = MutableLiveData<MessagesState>(MessagesState.Idle)
    val messagesState: LiveData<MessagesState> = _messagesState
    
    // Lista de mensajes de texto predeterminados
    private val _textMessages = MutableLiveData<List<TextMessage>>()
    val textMessages: LiveData<List<TextMessage>> = _textMessages
    
    // Lista de mensajes de voz predeterminados
    private val _voiceMessages = MutableLiveData<List<VoiceMessage>>()
    val voiceMessages: LiveData<List<VoiceMessage>> = _voiceMessages
    
    // Información del corredor
    private val _corridorName = MutableLiveData<String>()
    val corridorName: LiveData<String> = _corridorName
    
    /**
     * Carga los mensajes predeterminados desde el backend
     * Si falla, usa los mensajes locales como fallback
     */
    fun loadPredefinedMessages(operatorCode: String, useLocal: Boolean = false) {
        if (useLocal) {
            // Usar mensajes locales directamente (modo offline)
            Log.d(TAG, "Loading local predefined messages (offline mode)")
            _textMessages.value = messagesRepository.getLocalTextMessages()
            _voiceMessages.value = emptyList()
            _corridorName.value = "Modo Offline"
            _messagesState.value = MessagesState.Success
            return
        }
        
        viewModelScope.launch {
            _messagesState.value = MessagesState.Loading
            Log.d(TAG, "Loading predefined messages for operator: $operatorCode")
            
            when (val result = messagesRepository.getPredefinedMessages(operatorCode)) {
                is Result.Success -> {
                    Log.d(TAG, "Predefined messages loaded successfully:")
                    Log.d(TAG, "  - Text: ${result.data.total_text_messages} messages")
                    Log.d(TAG, "  - Voice: ${result.data.total_voice_messages} messages")
                    Log.d(TAG, "  - Corredor: ${result.data.operator.corredor_nombre}")
                    
                    _textMessages.value = result.data.text_messages
                    _voiceMessages.value = result.data.voice_messages
                    _corridorName.value = result.data.operator.corredor_nombre
                    _messagesState.value = MessagesState.Success
                }
                
                is Result.Error -> {
                    Log.e(TAG, "Error loading messages: ${result.message}")
                    // Usar mensajes locales como fallback
                    _textMessages.value = messagesRepository.getLocalTextMessages()
                    _voiceMessages.value = emptyList()
                    _corridorName.value = "Modo Offline"
                    _messagesState.value = MessagesState.Error(
                        "Error al cargar mensajes del servidor. Usando mensajes locales."
                    )
                }
                
                is Result.NetworkError -> {
                    Log.e(TAG, "Network error loading messages")
                    // Usar mensajes locales como fallback
                    _textMessages.value = messagesRepository.getLocalTextMessages()
                    _voiceMessages.value = emptyList()
                    _corridorName.value = "Modo Offline"
                    _messagesState.value = MessagesState.Error(
                        "Sin conexión. Usando mensajes predeterminados locales."
                    )
                }
                
                is Result.Timeout -> {
                    Log.e(TAG, "Timeout loading messages")
                    // Usar mensajes locales como fallback
                    _textMessages.value = messagesRepository.getLocalTextMessages()
                    _voiceMessages.value = emptyList()
                    _corridorName.value = "Modo Offline"
                    _messagesState.value = MessagesState.Error(
                        "Tiempo de espera agotado. Usando mensajes locales."
                    )
                }
            }
        }
    }
    
    /**
     * Recarga los mensajes predeterminados
     */
    fun reloadMessages(operatorCode: String) {
        loadPredefinedMessages(operatorCode, useLocal = false)
    }
}

/**
 * Estados posibles para la carga de mensajes
 */
sealed class MessagesState {
    object Idle : MessagesState()
    object Loading : MessagesState()
    object Success : MessagesState()
    data class Error(val message: String) : MessagesState()
}
