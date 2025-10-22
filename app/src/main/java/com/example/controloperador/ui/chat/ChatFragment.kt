package com.example.controloperador.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.data.MessageRepository
import com.example.controloperador.data.model.MessageType
import com.example.controloperador.databinding.FragmentChatBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private val messageRepository = MessageRepository()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        setupRecyclerView()
        loadMessages()
        setupResponseButton()
        
        return root
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }
    
    private fun loadMessages() {
        val messages = messageRepository.getAllTextMessages()
        chatAdapter.updateMessages(messages)
        
        // Hacer scroll al último mensaje
        if (messages.isNotEmpty()) {
            binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }
    
    private fun setupResponseButton() {
        binding.responseButton.setOnClickListener {
            showPredefinedResponsesBottomSheet()
        }
    }
    
    private fun showPredefinedResponsesBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_predefined_responses, null)
        bottomSheetDialog.setContentView(sheetView)
        
        // Configurar clicks para cada respuesta
        sheetView.findViewById<View>(R.id.responseMecanica).setOnClickListener {
            sendResponse(MessageType.MECHANICAL_FAILURE)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.responseNeumatico).setOnClickListener {
            sendResponse(MessageType.FLAT_TIRE)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.responseSiniestro).setOnClickListener {
            sendResponse(MessageType.ACCIDENT)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.responseTrafico).setOnClickListener {
            sendResponse(MessageType.TRAFFIC_STOPPED)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.responseDesviacion).setOnClickListener {
            sendResponse(MessageType.DETOUR)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.responsePrepago).setOnClickListener {
            sendResponse(MessageType.PREPAID_EQUIPMENT_FAILURE)
            bottomSheetDialog.dismiss()
        }
        
        sheetView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    private fun sendResponse(messageType: MessageType) {
        // Enviar la respuesta
        messageRepository.sendPredefinedResponse(messageType)
        
        // Recargar los mensajes
        loadMessages()
        
        // Mostrar confirmación
        Toast.makeText(
            requireContext(),
            getString(R.string.messages_sent),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
