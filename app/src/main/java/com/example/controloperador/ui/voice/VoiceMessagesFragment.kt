package com.example.controloperador.ui.voice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.databinding.FragmentVoiceMessagesBinding
import com.example.controloperador.ui.login.SessionManager
import com.example.controloperador.utils.AudioPlayerHelper
import com.example.controloperador.data.api.model.VoiceMessageDetail

/**
 * Fragment para mostrar conversaciones y mensajes de voz
 * 
 * Permite al operador:
 * - Ver todas sus conversaciones con mensajes de voz
 * - Acceder a los mensajes de cada conversación
 * - Reproducir mensajes de audio
 * - Marcar automáticamente mensajes como leídos
 */
class VoiceMessagesFragment : Fragment() {

    private var _binding: FragmentVoiceMessagesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: VoiceMessagesViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var conversationsAdapter: VoiceConversationsAdapter
    private lateinit var messagesAdapter: VoiceMessagesAdapter
    
    // Control de reproducción con AudioPlayerHelper
    private var audioPlayer: AudioPlayerHelper? = null
    private var currentPlayingMessage: VoiceMessageDetail? = null
    
    // Token de autenticación
    private var operatorCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceMessagesBinding.inflate(inflater, container, false)
        
        // Inicializar dependencias
        viewModel = ViewModelProvider(this)[VoiceMessagesViewModel::class.java]
        sessionManager = SessionManager(requireContext())
        
        // Obtener código de operador
        operatorCode = sessionManager.getOperatorCode()
        if (operatorCode == null) {
            Toast.makeText(requireContext(), "Error: Sesión no válida", Toast.LENGTH_LONG).show()
            return binding.root
        }
        
        // Inicializar AudioPlayer
        audioPlayer = AudioPlayerHelper(requireContext())
        setupAudioPlayer()
        
        setupRecyclerView()
        setupObservers()
        
        // Cargar conversaciones
        loadConversations()
        
        return binding.root
    }
    
    private fun setupAudioPlayer() {
        audioPlayer?.apply {
            // Listener cuando termina la reproducción
            setOnCompletionListener { messageId ->
                currentPlayingMessage = null
                messagesAdapter.setPlayingMessage(null)
            }
            
            // Listener de errores
            setOnErrorListener { messageId, error ->
                currentPlayingMessage = null
                messagesAdapter.setPlayingMessage(null)
                Toast.makeText(
                    requireContext(),
                    "Error al reproducir: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // Listener cuando el audio está listo
            setOnPreparedListener { messageId, duration ->
                Toast.makeText(
                    requireContext(),
                    "Reproduciendo audio",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupRecyclerView() {
        // Adapter para lista de conversaciones
        conversationsAdapter = VoiceConversationsAdapter { conversation ->
            // Al hacer click en una conversación, mostrar sus mensajes
            viewModel.selectConversation(conversation)
            operatorCode?.let { code ->
                viewModel.loadMessages(code, conversation.conversationId)
            }
        }
        
        // Adapter para mensajes de una conversación
        messagesAdapter = VoiceMessagesAdapter { message ->
            handlePlayPause(message)
        }
        
        // Inicialmente mostrar conversaciones
        binding.voiceMessagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationsAdapter
        }
    }
    
    private fun setupObservers() {
        // Observar conversaciones
        viewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            conversationsAdapter.updateConversations(conversations)
            
            if (conversations.isEmpty()) {
                showEmptyState("No tienes mensajes de voz")
            } else {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
                binding.voiceMessagesRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // Observar mensajes de conversación seleccionada
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (messages.isNotEmpty()) {
                // Cambiar adapter a mensajes
                binding.voiceMessagesRecyclerView.adapter = messagesAdapter
                messagesAdapter.updateMessages(messages)
            }
        }
        
        // Observar estado de carga
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
        
        // Observar conversación seleccionada (para navegación)
        viewModel.selectedConversation.observe(viewLifecycleOwner) { conversation ->
            conversation?.let {
                // Cargar mensajes de la conversación seleccionada
                operatorCode?.let { code ->
                    viewModel.loadMessages(code, it.conversationId)
                }
            }
        }
    }
    
    private fun loadConversations() {
        operatorCode?.let { code ->
            viewModel.loadConversations(code)
        }
    }
    
    private fun showEmptyState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        binding.voiceMessagesRecyclerView.visibility = View.GONE
        // Podrías actualizar el texto del emptyState aquí si tienes un TextView
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    private fun handlePlayPause(message: VoiceMessageDetail) {
        val player = audioPlayer ?: return
        
        val messageIdString = message.id.toString()
        
        if (currentPlayingMessage?.id == message.id && player.isPlaying()) {
            // Pausar el mensaje actual
            player.pause()
            messagesAdapter.setPlayingMessage(null)
            currentPlayingMessage = null
        } else if (currentPlayingMessage?.id == message.id && !player.isPlaying()) {
            // Reanudar el mensaje pausado
            player.resume()
            messagesAdapter.setPlayingMessage(messageIdString)
            currentPlayingMessage = message
        } else {
            // Reproducir nuevo mensaje
            startPlayback(message)
        }
    }
    
    private fun startPlayback(message: VoiceMessageDetail) {
        val player = audioPlayer ?: return
        
        // Detener reproducción anterior si existe
        player.stop()
        
        currentPlayingMessage = message
        messagesAdapter.setPlayingMessage(message.id.toString())
        
        // Reproducir desde URL del backend
        val started = player.playAudioFromPath(message.audioUrl, message.id.toString())
        
        if (!started) {
            Toast.makeText(
                requireContext(),
                "No se pudo reproducir el audio",
                Toast.LENGTH_SHORT
            ).show()
            currentPlayingMessage = null
            messagesAdapter.setPlayingMessage(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer?.release() // IMPORTANTE: Liberar MediaPlayer
        audioPlayer = null
        _binding = null
    }
    
    override fun onPause() {
        super.onPause()
        // Pausar reproducción cuando el fragment no esté visible
        audioPlayer?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar conversaciones al volver al fragment
        operatorCode?.let { code ->
            viewModel.refresh(code)
        }
    }
}
