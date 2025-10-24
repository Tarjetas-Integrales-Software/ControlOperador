package com.example.controloperador.ui.voice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.data.MessageRepository
import com.example.controloperador.data.model.VoiceMessage
import com.example.controloperador.databinding.FragmentVoiceMessagesBinding
import com.example.controloperador.utils.AudioPlayerHelper

class VoiceMessagesFragment : Fragment() {

    private var _binding: FragmentVoiceMessagesBinding? = null
    private val binding get() = _binding!!
    
    private val messageRepository = MessageRepository()
    private lateinit var voiceAdapter: VoiceMessageAdapter
    
    // Control de reproducción con AudioPlayerHelper
    private var audioPlayer: AudioPlayerHelper? = null
    private var currentPlayingMessage: VoiceMessage? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceMessagesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        // Inicializar AudioPlayer
        audioPlayer = AudioPlayerHelper(requireContext())
        setupAudioPlayer()
        
        setupRecyclerView()
        loadVoiceMessages()
        
        return root
    }
    
    private fun setupAudioPlayer() {
        audioPlayer?.apply {
            // Listener cuando termina la reproducción
            setOnCompletionListener { messageId ->
                currentPlayingMessage = null
                voiceAdapter.setPlayingMessage(null)
                Toast.makeText(
                    requireContext(),
                    "Reproducción finalizada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Listener de errores
            setOnErrorListener { messageId, error ->
                currentPlayingMessage = null
                voiceAdapter.setPlayingMessage(null)
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
                    "Reproduciendo audio ($duration segundos)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupRecyclerView() {
        voiceAdapter = VoiceMessageAdapter { message ->
            handlePlayPause(message)
        }
        
        binding.voiceMessagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = voiceAdapter
        }
    }
    
    private fun loadVoiceMessages() {
        val messages = messageRepository.getAllVoiceMessages()
        
        if (messages.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.voiceMessagesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.voiceMessagesRecyclerView.visibility = View.VISIBLE
            voiceAdapter.updateMessages(messages)
        }
    }
    
    private fun handlePlayPause(message: VoiceMessage) {
        val player = audioPlayer ?: return
        
        if (currentPlayingMessage?.id == message.id && player.isPlaying()) {
            // Pausar el mensaje actual
            player.pause()
            voiceAdapter.setPlayingMessage(null)
            currentPlayingMessage = null
        } else if (currentPlayingMessage?.id == message.id && !player.isPlaying()) {
            // Reanudar el mensaje pausado
            player.resume()
            voiceAdapter.setPlayingMessage(message.id)
            currentPlayingMessage = message
        } else {
            // Reproducir nuevo mensaje
            startPlayback(message)
        }
    }
    
    private fun startPlayback(message: VoiceMessage) {
        val player = audioPlayer ?: return
        
        // Detener reproducción anterior si existe
        player.stop()
        
        currentPlayingMessage = message
        voiceAdapter.setPlayingMessage(message.id)
        
        // Marcar como reproducido
        messageRepository.markVoiceMessageAsPlayed(message.id)
        
        // Determinar qué método de reproducción usar
        val started = if (message.audioFilePath != null) {
            // Si tiene path local, reproducir desde ahí
            player.playAudioFromPath(message.audioFilePath, message.id)
        } else if (message.audioUrl != null) {
            // Si tiene URL, reproducir desde URL
            player.playAudioFromPath(message.audioUrl, message.id)
        } else {
            // Usar audio de ejemplo de res/raw/ (cambiar R.raw.sample_audio por tu archivo)
            // Por ahora mostramos mensaje de error
            Toast.makeText(
                requireContext(),
                "No hay archivo de audio disponible",
                Toast.LENGTH_SHORT
            ).show()
            currentPlayingMessage = null
            voiceAdapter.setPlayingMessage(null)
            false
        }
        
        if (!started) {
            currentPlayingMessage = null
            voiceAdapter.setPlayingMessage(null)
        }
    }
    
    private fun stopPlayback() {
        audioPlayer?.stop()
        currentPlayingMessage = null
        voiceAdapter.setPlayingMessage(null)
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
}
