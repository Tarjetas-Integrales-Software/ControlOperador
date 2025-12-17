package com.example.controloperador.ui.voice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controloperador.data.api.model.VoiceConversation
import com.example.controloperador.data.api.model.VoiceMessageDetail
import com.example.controloperador.data.repository.VoiceMessagesRepositoryV2
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar mensajes de voz
 * 
 * Funcionalidades:
 * - Cargar conversaciones con mensajes de voz
 * - Cargar mensajes de una conversación específica
 * - Marcar mensajes como leídos
 * - Gestionar estados de carga y errores
 */
class VoiceMessagesViewModel(
    private val repository: VoiceMessagesRepositoryV2 = VoiceMessagesRepositoryV2()
) : ViewModel() {
    
    // Lista de conversaciones
    private val _conversations = MutableLiveData<List<VoiceConversation>>()
    val conversations: LiveData<List<VoiceConversation>> = _conversations
    
    // Lista de mensajes de la conversación actual
    private val _messages = MutableLiveData<List<VoiceMessageDetail>>()
    val messages: LiveData<List<VoiceMessageDetail>> = _messages
    
    // Estado de carga
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Total de mensajes sin leer
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount
    
    // Conversación seleccionada actualmente
    private val _selectedConversation = MutableLiveData<VoiceConversation?>()
    val selectedConversation: LiveData<VoiceConversation?> = _selectedConversation
    
    /**
     * Carga todas las conversaciones del operador
     * 
     * @param operatorCode Código del operador (5 dígitos)
     */
    fun loadConversations(operatorCode: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            when (val result = repository.getConversations(operatorCode)) {
                is VoiceMessagesRepositoryV2.Result.Success -> {
                    _conversations.value = result.data
                    // Calcular total de mensajes sin leer
                    _unreadCount.value = result.data.sumOf { it.unreadCount }
                    _loading.value = false
                    
                    // Cargar automáticamente los mensajes de la primera conversación
                    if (result.data.isNotEmpty()) {
                        val firstConversation = result.data.first()
                        loadMessages(operatorCode, firstConversation.conversationId)
                    }
                }
                
                is VoiceMessagesRepositoryV2.Result.Error -> {
                    _error.value = result.message
                    _loading.value = false
                }
                
                is VoiceMessagesRepositoryV2.Result.NetworkError -> {
                    _error.value = "Error de conexión. Verifica tu internet."
                    _loading.value = false
                }
                
                is VoiceMessagesRepositoryV2.Result.Timeout -> {
                    _error.value = "Tiempo de espera agotado. Intenta nuevamente."
                    _loading.value = false
                }
            }
        }
    }
    
    /**
     * Carga los mensajes de una conversación específica
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación
     */
    fun loadMessages(operatorCode: String, conversationId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            when (val result = repository.getConversationMessages(operatorCode, conversationId)) {
                is VoiceMessagesRepositoryV2.Result.Success -> {
                    _messages.value = result.data
                    _loading.value = false
                    
                    // Marcar mensajes como leídos automáticamente
                    markAsRead(operatorCode, conversationId)
                }
                
                is VoiceMessagesRepositoryV2.Result.Error -> {
                    _error.value = result.message
                    _loading.value = false
                }
                
                is VoiceMessagesRepositoryV2.Result.NetworkError -> {
                    _error.value = "Error de conexión. Verifica tu internet."
                    _loading.value = false
                }
                
                is VoiceMessagesRepositoryV2.Result.Timeout -> {
                    _error.value = "Tiempo de espera agotado. Intenta nuevamente."
                    _loading.value = false
                }
            }
        }
    }
    
    /**
     * Marca los mensajes de una conversación como leídos
     * 
     * @param operatorCode Código del operador (5 dígitos)
     * @param conversationId ID de la conversación
     */
    private fun markAsRead(operatorCode: String, conversationId: String) {
        viewModelScope.launch {
            when (val result = repository.markAsRead(operatorCode, conversationId)) {
                is VoiceMessagesRepositoryV2.Result.Success -> {
                    // Actualizar contador de no leídos en la conversación
                    _conversations.value = _conversations.value?.map { conversation ->
                        if (conversation.conversationId == conversationId) {
                            conversation.copy(unreadCount = 0)
                        } else {
                            conversation
                        }
                    }
                    
                    // Actualizar contador total
                    _unreadCount.value = _conversations.value?.sumOf { it.unreadCount } ?: 0
                }
                
                else -> {
                    // No hacer nada si falla, no es crítico
                }
            }
        }
    }
    
    /**
     * Selecciona una conversación
     * 
     * @param conversation Conversación seleccionada
     */
    fun selectConversation(conversation: VoiceConversation) {
        _selectedConversation.value = conversation
    }
    
    /**
     * Deselecciona la conversación actual
     */
    fun clearSelectedConversation() {
        _selectedConversation.value = null
        _messages.value = emptyList()
    }
    
    /**
     * Limpia el error actual
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Recarga las conversaciones (pull to refresh)
     * 
     * @param operatorCode Código del operador (5 dígitos)
     */
    fun refresh(operatorCode: String) {
        loadConversations(operatorCode)
    }
}

// Extension para crear copias de VoiceConversation con valores modificados
private fun VoiceConversation.copy(
    unreadCount: Int = this.unreadCount
): VoiceConversation {
    return VoiceConversation(
        conversationId = this.conversationId,
        operatorCode = this.operatorCode,
        operatorName = this.operatorName,
        operatorApellidoPaterno = this.operatorApellidoPaterno,
        operatorApellidoMaterno = this.operatorApellidoMaterno,
        analystName = this.analystName,
        lastMessageDate = this.lastMessageDate,
        lastAudioDuration = this.lastAudioDuration,
        unreadCount = unreadCount,
        totalMessages = this.totalMessages
    )
}
