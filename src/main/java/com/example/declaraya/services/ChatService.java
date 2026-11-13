package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Mensaje;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.ChatRepository;
import com.example.declaraya.repositories.MensajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    public Chat guardar(Chat chat) {
        return chatRepository.save(chat);
    }

    public Chat buscarPorAcuerdoId(Long acuerdoId) {
        return chatRepository.findByAcuerdoId(acuerdoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Chat no encontrado"));
    }

    public Mensaje enviarMensaje(Chat chat, Usuario remitente, String contenido) {
        return mensajeRepository.save(new Mensaje(chat, remitente, contenido));
    }

    public List<Mensaje> listarMensajes(Chat chat) {
        return mensajeRepository.findByChatOrderByFechaEnvioAsc(chat);
    }
}
