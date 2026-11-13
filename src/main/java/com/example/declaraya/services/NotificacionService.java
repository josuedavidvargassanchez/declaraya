package com.example.declaraya.services;

import com.example.declaraya.models.Notificacion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    public void notificar(Usuario destinatario, String mensaje, String enlace) {
        if (destinatario == null) {
            return;
        }
        notificacionRepository.save(new Notificacion(destinatario, mensaje, enlace));
    }

    public List<Notificacion> listar(Usuario usuario) {
        return notificacionRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }

    public void marcarTodasLeidas(Usuario usuario) {
        List<Notificacion> pendientes = notificacionRepository.findByUsuarioOrderByFechaDesc(usuario);
        for (Notificacion n : pendientes) {
            n.setLeida(true);
        }
        notificacionRepository.saveAll(pendientes);
    }

    public void eliminarDe(Usuario usuario) {
        notificacionRepository.deleteAll(notificacionRepository.findByUsuarioOrderByFechaDesc(usuario));
    }
}
