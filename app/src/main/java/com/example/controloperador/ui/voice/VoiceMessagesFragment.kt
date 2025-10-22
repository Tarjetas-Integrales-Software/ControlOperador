package com.example.controloperador.ui.voice

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class VoiceMessagesFragment : Fragment() {

    private var _binding: FragmentVoiceMessagesBinding? = null
    private val binding get() = _binding!!
    
    private val messageRepository = MessageRepository()
    private lateinit var voiceAdapter: VoiceMessageAdapter
    
    // Control de reproducción simulada
    private var currentPlayingMessage: VoiceMessage? = null
    private var playbackHandler: Handler? = null
    private var playbackRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceMessagesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        setupRecyclerView()
        loadVoiceMessages()
        
        return root
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
        if (currentPlayingMessage?.id == message.id) {
            // Pausar el mensaje actual
            stopPlayback()
        } else {
            // Reproducir nuevo mensaje
            startPlayback(message)
        }
    }
    
    private fun startPlayback(message: VoiceMessage) {
        // Detener reproducción anterior si existe
        stopPlayback()
        
        currentPlayingMessage = message
        voiceAdapter.setPlayingMessage(message.id)
        
        // Marcar como reproducido
        messageRepository.markVoiceMessageAsPlayed(message.id)
        
        // Mostrar toast de reproducción (simulada)
        Toast.makeText(
            requireContext(),
            getString(R.string.voice_play),
            Toast.LENGTH_SHORT
        ).show()
        
        // Simular reproducción con un handler
        playbackHandler = Handler(Looper.getMainLooper())
        playbackRunnable = Runnable {
            // Cuando termina la reproducción
            stopPlayback()
            Toast.makeText(
                requireContext(),
                "Reproducción finalizada",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Programar el fin de la reproducción según la duración
        playbackHandler?.postDelayed(playbackRunnable!!, message.duration * 1000L)
    }
    
    private fun stopPlayback() {
        currentPlayingMessage = null
        voiceAdapter.setPlayingMessage(null)
        
        // Cancelar el runnable programado
        playbackRunnable?.let { playbackHandler?.removeCallbacks(it) }
        playbackHandler = null
        playbackRunnable = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayback()
        _binding = null
    }
}
