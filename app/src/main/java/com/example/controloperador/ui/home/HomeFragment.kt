package com.example.controloperador.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.controloperador.R
import com.example.controloperador.data.MessageRepository
import com.example.controloperador.data.model.TextMessage
import com.example.controloperador.data.model.VoiceMessage
import com.example.controloperador.databinding.FragmentHomeBinding
import com.example.controloperador.ui.login.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionManager: SessionManager
    private val messageRepository = MessageRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        sessionManager = SessionManager(requireContext())
        
        setupWelcomeMessage()
        loadMessagesSummary()
        setupClickListeners()

        return root
    }
    
    private fun setupWelcomeMessage() {
        val operatorCode = sessionManager.getOperatorCode()
        binding.welcomeText.text = "Bienvenido, Operador $operatorCode"
    }
    
    private fun loadMessagesSummary() {
        val summary = messageRepository.getMessagesSummary()
        
        // Actualizar badge de mensajes de texto
        if (summary.unreadTextMessages > 0) {
            binding.unreadTextBadge.visibility = View.VISIBLE
            binding.unreadTextBadge.text = "${summary.unreadTextMessages} sin leer"
        } else {
            binding.unreadTextBadge.visibility = View.GONE
        }
        
        // Actualizar badge de mensajes de voz
        if (summary.unplayedVoiceMessages > 0) {
            binding.unplayedVoiceBadge.visibility = View.VISIBLE
            binding.unplayedVoiceBadge.text = "${summary.unplayedVoiceMessages} sin reproducir"
        } else {
            binding.unplayedVoiceBadge.visibility = View.GONE
        }
        
        // Mostrar últimos mensajes de texto
        displayRecentTextMessages(summary.recentTextMessages)
        
        // Mostrar últimos mensajes de voz
        displayRecentVoiceMessages(summary.recentVoiceMessages)
    }
    
    private fun displayRecentTextMessages(messages: List<TextMessage>) {
        binding.textMessagesContainer.removeAllViews()
        
        if (messages.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = getString(R.string.messages_no_messages)
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 16, 0, 16)
            }
            binding.textMessagesContainer.addView(emptyView)
            return
        }
        
        messages.forEach { message ->
            val itemView = layoutInflater.inflate(
                R.layout.item_text_message_summary,
                binding.textMessagesContainer,
                false
            )
            
            itemView.findViewById<TextView>(R.id.messageSender).text = message.senderName
            itemView.findViewById<TextView>(R.id.messageContent).text = message.content
            itemView.findViewById<TextView>(R.id.messageTime).text = getRelativeTime(message.timestamp)
            
            itemView.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_chat)
            }
            
            binding.textMessagesContainer.addView(itemView)
        }
    }
    
    private fun displayRecentVoiceMessages(messages: List<VoiceMessage>) {
        binding.voiceMessagesContainer.removeAllViews()
        
        if (messages.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = getString(R.string.messages_no_voice)
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 16, 0, 16)
            }
            binding.voiceMessagesContainer.addView(emptyView)
            return
        }
        
        messages.forEach { message ->
            val itemView = layoutInflater.inflate(
                R.layout.item_voice_message_summary,
                binding.voiceMessagesContainer,
                false
            )
            
            itemView.findViewById<TextView>(R.id.voiceSender).text = message.senderName
            itemView.findViewById<TextView>(R.id.voiceDuration).text = formatDuration(message.duration)
            itemView.findViewById<TextView>(R.id.voiceTime).text = getRelativeTime(message.timestamp)
            
            itemView.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_voice)
            }
            
            binding.voiceMessagesContainer.addView(itemView)
        }
    }
    
    private fun setupClickListeners() {
        binding.viewAllTextButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat)
        }
        
        binding.viewAllVoiceButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_voice)
        }
    }
    
    private fun getRelativeTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> getString(R.string.time_just_now)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                getString(R.string.time_minutes_ago, minutes)
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                getString(R.string.time_hours_ago, hours)
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                getString(R.string.time_days_ago, days)
            }
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return getString(R.string.voice_duration, minutes, secs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}