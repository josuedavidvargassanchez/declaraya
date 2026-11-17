package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.models.Notificacion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    public void notificar(Usuario destinatario, String mensaje, String enlace) {
        if (destinatario == null) {
            return;
        }
        // la columna admite 300 caracteres; los textos largos (motivos, justificaciones) se recortan
        if (mensaje != null && mensaje.length() > 300) {
            mensaje = mensaje.substring(0, 297) + "...";
        }
        notificacionRepository.save(new Notificacion(destinatario, mensaje, enlace));
    }

    public List<Notificacion> listar(Usuario usuario) {
        return notificacionRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    /** filtro: "pendientes" (no leidas), "leidas" o cualquier otro valor para todas. */
    public List<Notificacion> listar(Usuario usuario, String filtro) {
        if ("pendientes".equals(filtro)) {
            return notificacionRepository.findByUsuarioAndLeidaOrderByFechaDesc(usuario, false);
        }
        if ("leidas".equals(filtro)) {
            return notificacionRepository.findByUsuarioAndLeidaOrderByFechaDesc(usuario, true);
        }
        return listar(usuario);
    }

    /**
     * Abre una notificacion: debe ser del usuario (si no, 403), la marca leida y devuelve a donde ir.
     * Solo se aceptan rutas internas que empiezan por "/" (nunca "//" ni URLs externas).
     */
    public String abrir(Long id, Usuario usuario) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));
        if (!notificacion.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta notificación no es tuya");
        }
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
        return esRutaInterna(notificacion.getEnlace()) ? notificacion.getEnlace() : "/notificaciones";
    }

    static boolean esRutaInterna(String enlace) {
        return enlace != null && enlace.startsWith("/") && !enlace.startsWith("//") && !enlace.contains("\\")
                && !enlace.contains("\r") && !enlace.contains("\n");
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
