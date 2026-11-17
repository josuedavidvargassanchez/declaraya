package com.example.declaraya.services;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Calificacion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.CalificacionRepository;

/**
 * Calificaciones de los contadores. Solo la persona del acuerdo califica, una sola vez y solo
 * cuando el acuerdo esta FINALIZADO. El promedio cuenta unicamente las calificaciones no ocultas.
 */
@Service
public class CalificacionService {

    public static final int COMENTARIO_MAX = 500;

    private final CalificacionRepository calificacionRepository;
    private final AcuerdoService acuerdoService;
    private final AcuerdoRepository acuerdoRepository;
    private final NotificacionService notificacionService;
    private final ActividadAcuerdoService actividadService;

    public CalificacionService(CalificacionRepository calificacionRepository, AcuerdoService acuerdoService,
                               AcuerdoRepository acuerdoRepository, NotificacionService notificacionService,
                               ActividadAcuerdoService actividadService) {
        this.calificacionRepository = calificacionRepository;
        this.acuerdoService = acuerdoService;
        this.acuerdoRepository = acuerdoRepository;
        this.notificacionService = notificacionService;
        this.actividadService = actividadService;
    }

    public Optional<Calificacion> buscarPorAcuerdo(Acuerdo acuerdo) {
        return calificacionRepository.findByAcuerdo(acuerdo);
    }

    public boolean puedeCalificar(Acuerdo acuerdo, Usuario usuario) {
        return usuario != null
                && acuerdo.getOferta().getSolicitud().getPersona().getId().equals(usuario.getId())
                && acuerdo.getEstado() == EstadoAcuerdo.FINALIZADO
                && !calificacionRepository.existsByAcuerdo(acuerdo);
    }

    public Calificacion calificar(Long acuerdoId, Usuario persona, Integer puntaje, String comentario, boolean confirmado) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(acuerdoId);
        if (!acuerdo.getOferta().getSolicitud().getPersona().getId().equals(persona.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente del acuerdo puede calificar");
        }
        if (acuerdo.getEstado() != EstadoAcuerdo.FINALIZADO) {
            throw new ReglaNegocioException("Solo puedes calificar cuando el acuerdo está finalizado");
        }
        if (calificacionRepository.existsByAcuerdo(acuerdo)) {
            throw new ReglaNegocioException("Ya calificaste este acuerdo");
        }
        if (puntaje == null || puntaje < 1 || puntaje > 5) {
            throw new ReglaNegocioException("Elige un puntaje de 1 a 5 estrellas");
        }
        String texto = comentario == null || comentario.isBlank() ? null : comentario.trim();
        if (texto != null && texto.length() > COMENTARIO_MAX) {
            throw new ReglaNegocioException("El comentario no puede superar " + COMENTARIO_MAX + " caracteres");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma tu calificación marcando la casilla");
        }
        Usuario contador = acuerdo.getOferta().getContador();
        Calificacion guardada;
        try {
            guardada = calificacionRepository.save(new Calificacion(acuerdo, persona, contador, puntaje, texto));
        } catch (DataIntegrityViolationException e) {
            // la restriccion unica tambien protege contra dos envios simultaneos
            throw new ReglaNegocioException("Ya calificaste este acuerdo");
        }
        actividadService.registrar(acuerdo, persona, TipoActividad.CALIFICACION, "Calificación de " + puntaje + " de 5");
        notificacionService.notificar(contador,
                persona.getNombre() + " te calificó con " + puntaje + " de 5 en el acuerdo #" + acuerdo.getId(),
                "/contadores/" + contador.getId());
        return guardada;
    }

    /** Promedio de las calificaciones visibles (null si no tiene). */
    public Double promedio(Usuario contador) {
        List<Calificacion> visibles = calificacionRepository.findByContadorAndOcultaFalseOrderByFechaDesc(contador);
        return visibles.isEmpty() ? null : visibles.stream().mapToInt(Calificacion::getPuntaje).average().orElse(0);
    }

    public long cantidadVisibles(Usuario contador) {
        return calificacionRepository.findByContadorAndOcultaFalseOrderByFechaDesc(contador).size();
    }

    /** Ultimas resenas visibles con comentario, para el perfil publico. */
    public List<Calificacion> resenasVisibles(Usuario contador, int limite) {
        return calificacionRepository.findByContadorAndOcultaFalseOrderByFechaDesc(contador).stream()
                .filter(c -> c.getComentario() != null).limit(limite).toList();
    }

    public long acuerdosFinalizados(Usuario contador) {
        return acuerdoRepository.countByOfertaContadorAndEstado(contador, EstadoAcuerdo.FINALIZADO);
    }

    // ------------------------------------------------------------ administracion

    public List<Calificacion> listarConComentario() {
        return calificacionRepository.findByComentarioIsNotNullOrderByFechaDesc();
    }

    /** El admin oculta la calificacion (motivo obligatorio); no se borra y deja de contar en el promedio. */
    public Calificacion ocultar(Long id, String motivo, boolean confirmado) {
        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Calificación no encontrada"));
        if (calificacion.isOculta()) {
            throw new ReglaNegocioException("Ese comentario ya está oculto");
        }
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.isEmpty() || texto.length() > COMENTARIO_MAX) {
            throw new ReglaNegocioException("Indica el motivo para ocultar el comentario (máximo " + COMENTARIO_MAX + " caracteres)");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma que deseas ocultar el comentario");
        }
        calificacion.setOculta(true);
        calificacion.setMotivoOcultamiento(texto);
        return calificacionRepository.save(calificacion);
    }
}
