package com.example.controloperador.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.data.MessageRepository
import com.example.controloperador.data.model.MessageType
import com.example.controloperador.databinding.FragmentChatBinding
import com.example.controloperador.ui.login.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private val messageRepository = MessageRepository.getInstance() // Singleton compartido
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sessionManager: SessionManager
    
    // ViewModel compartido con HomeFragment para sincronizar conversaciones
    private val viewModel: ChatViewModel by activityViewModels()
    
    // Lista de mensajes de texto predeterminados cargados
    private var textMessages: List<com.example.controloperador.data.api.model.TextMessage> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        sessionManager = SessionManager(requireContext())
        
        setupRecyclerView()
        loadMessages()
        observeViewModel()
        loadPredefinedMessages()
        
        return root
    }
    
    /**
     * Observa los cambios en el ViewModel
     */
    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.messagesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MessagesState.Loading -> {
                    // Mostrar indicador de carga si es necesario
                }
                is MessagesState.Success -> {
                    // Mensajes cargados exitosamente
                }
                is MessagesState.Error -> {
                    // Mostrar mensaje de error (pero los mensajes locales ya están cargados)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is MessagesState.Idle -> {
                    // Estado inicial
                }
            }
        }
        
        // Observar mensajes de texto predeterminados
        viewModel.textMessages.observe(viewLifecycleOwner) { messages ->
            textMessages = messages
            setupResponseButtons() // Re-configurar botones con los nuevos mensajes
        }
        
        // Observar nombre del corredor (opcional, para mostrar en UI)
        viewModel.corridorName.observe(viewLifecycleOwner) { corridorName ->
            Log.d("ChatFragment", "Corredor: $corridorName")
            // Podrías actualizar la UI con el nombre del corredor si lo necesitas
        }
    }
    
    /**
     * Carga los mensajes predeterminados desde el backend o localmente
     */
    private fun loadPredefinedMessages() {
        val operatorCode = sessionManager.getOperatorCode()
        
        if (operatorCode == "54321") {
            // Usuario de prueba offline: usar mensajes locales
            viewModel.loadPredefinedMessages(operatorCode, useLocal = true)
        } else {
            // Usuario normal: intentar cargar desde backend
            viewModel.loadPredefinedMessages(operatorCode ?: "")
        }
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        
        // Configurar LinearLayoutManager para chat convencional
        // - stackFromEnd = false: items se apilan desde arriba (orden normal)
        // - reverseLayout = false: no invertir el orden de los items
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = false  // Los mensajes llenan desde arriba
            reverseLayout = false // Orden normal: antiguos arriba, nuevos abajo
        }
        
        binding.messagesRecyclerView.apply {
            this.layoutManager = layoutManager
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
    
    private fun setupResponseButtons() {
        // Si no hay mensajes cargados, no hacer nada aún
        if (textMessages.isEmpty()) return
        
        // Detectar si estamos en landscape o portrait
        // Portrait: tiene responseButton, Landscape: tiene los botones directos
        
        val responseButton = binding.root.findViewById<View>(R.id.responseButton)
        
        if (responseButton != null) {
            // Portrait: botón que abre bottom sheet
            responseButton.setOnClickListener {
                showPredefinedResponsesBottomSheet()
            }
        } else {
            // Landscape: botones directos en el panel lateral
            setupLandscapeResponseButtons()
        }
    }
    
    private fun setupLandscapeResponseButtons() {
        val container = binding.root.findViewById<LinearLayout>(R.id.responsesContainer)
        container?.removeAllViews() // Limpiar botones anteriores
        
        // Crear botones dinámicamente según los mensajes del backend
        textMessages.forEach { message ->
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.button_margin)
                }
                text = message.nombre // Título del mensaje
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                textSize = 13f
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingTop,
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingBottom
                )
                cornerRadius = resources.getDimensionPixelSize(R.dimen.button_corner_radius)
                
                setOnClickListener {
                    sendDynamicResponse(message)
                }
            }
            
            container.addView(button)
        }
    }
    
    private fun showPredefinedResponsesBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_predefined_responses, null)
        bottomSheetDialog.setContentView(sheetView)
        
        // Obtener el contenedor de botones en el bottom sheet
        val container = sheetView.findViewById<LinearLayout>(R.id.responsesContainer)
        container?.removeAllViews() // Limpiar botones anteriores
        
        // Crear botones dinámicamente según los mensajes del backend
        textMessages.forEach { message ->
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.button_margin)
                }
                text = message.nombre // Título del mensaje
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                textSize = 13f
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingTop,
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingBottom
                )
                cornerRadius = resources.getDimensionPixelSize(R.dimen.button_corner_radius)
                
                setOnClickListener {
                    sendDynamicResponse(message)
                    bottomSheetDialog.dismiss()
                }
            }
            
            container.addView(button)
        }
        
        // Botón de cancelar
        sheetView.findViewById<View>(R.id.cancelButton)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    /**
     * Envía una respuesta dinámica cargada desde el backend
     */
    private fun sendDynamicResponse(message: com.example.controloperador.data.api.model.TextMessage) {
        // Enviar el mensaje usando el repositorio compartido
        messageRepository.sendTextMessage(message.mensaje)
        
        // Recargar los mensajes en el RecyclerView
        loadMessages()
        
        // Mostrar confirmación
        Toast.makeText(
            requireContext(),
            "Enviando: ${message.nombre}",
            Toast.LENGTH_SHORT
        ).show()
        
        // TODO: Implementar envío real al backend
        // messagesRepository.sendPredefinedMessage(operatorCode, message.id)
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
