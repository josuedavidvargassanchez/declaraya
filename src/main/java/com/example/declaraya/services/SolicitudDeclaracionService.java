package com.example.declaraya.services;

import com.example.declaraya.dto.NuevaSolicitudDTO;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.OfertaRepository;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SolicitudDeclaracionService {

    @Autowired
    private SolicitudDeclaracionRepository solicitudRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private AcuerdoRepository acuerdoRepository;

    @Autowired
    private SupervisionService supervisionService;

    // ------------------------------------------------------------ permisos

    /**
     * Regla unica de quien puede enviar ofertas (la usa OfertaController y la vista de la solicitud).
     * Devuelve null si puede, o el codigo del motivo por el que no puede (el parametro de error del panel).
     */
    public String motivoNoPuedeOfertar(Usuario contador) {
        if (contador == null || !contador.isContador()) {
            return "errorRol";
        }
        if (!contador.isVerificado()) {
            return "errorVerificacion";
        }
        if (contador.getRol() == Rol.CONTADOR_PRACTICANTE) {
            if (contador.getMentor() == null) {
                return "errorMentor";
            }
            if (acuerdoRepository.countByOfertaContadorAndEstado(contador, EstadoAcuerdo.ACTIVO) > 0) {
                return "errorCasoActivo";
            }
        }
        return null;
    }

    public boolean puedeOfertar(Usuario contador) {
        return motivoNoPuedeOfertar(contador) == null;
    }

    /**
     * Quien ve /solicitudes/{id}: el dueño, el admin, un contador que puede ofertar mientras esta ABIERTA,
     * un contador que ya oferto en ella (en cualquier estado) y el contador o el mentor de su acuerdo.
     */
    public boolean puedeVer(SolicitudDeclaracion solicitud, Usuario usuario) {
        if (solicitud == null || usuario == null) {
            return false;
        }
        if (usuario.getRol() == Rol.ADMIN || solicitud.getPersona().getId().equals(usuario.getId())) {
            return true;
        }
        if (!usuario.isContador()) {
            return false;
        }
        if (solicitud.getEstado() == EstadoSolicitud.ABIERTA && puedeOfertar(usuario)) {
            return true;
        }
        // el contador del acuerdo tambien entra aqui: el acuerdo nace de su oferta
        if (ofertaRepository.existsBySolicitudAndContador(solicitud, usuario)) {
            return true;
        }
        if (usuario.getRol() == Rol.CONTADOR_PUBLICO) {
            for (Acuerdo acuerdo : acuerdoRepository.findByOfertaSolicitud(solicitud)) {
                if (supervisionService.esMentorDe(usuario, acuerdo) || supervisionService.esMentorResponsable(usuario, acuerdo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Lanza 403 si el usuario no puede ver la solicitud. */
    public void exigirVisible(SolicitudDeclaracion solicitud, Usuario usuario) {
        if (!puedeVer(solicitud, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta solicitud");
        }
    }

    public SolicitudDeclaracion crear(Usuario persona, String titulo, String descripcion) {
        SolicitudDeclaracion solicitud = new SolicitudDeclaracion(persona, titulo, descripcion);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion crear(Usuario persona, NuevaSolicitudDTO dto) {
        SolicitudDeclaracion solicitud = new SolicitudDeclaracion(persona, dto.getTitulo(), dto.getDescripcion());
        copiarDetalles(solicitud, dto);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion editar(Long id, NuevaSolicitudDTO dto) {
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setTitulo(dto.getTitulo());
        solicitud.setDescripcion(dto.getDescripcion());
        copiarDetalles(solicitud, dto);
        return solicitudRepository.save(solicitud);
    }

    private void copiarDetalles(SolicitudDeclaracion solicitud, NuevaSolicitudDTO dto) {
        solicitud.setAnioGravable(dto.getAnioGravable());
        solicitud.setUrgencia(dto.getUrgencia());
        solicitud.setPresupuestoMaximo(dto.getPresupuestoMaximo());
        List<String> situaciones = dto.getSituaciones();
        solicitud.setSituaciones(situaciones == null || situaciones.isEmpty() ? null : String.join(",", situaciones));
    }

    public List<SolicitudDeclaracion> listarAbiertas() {
        return solicitudRepository.findByEstado(EstadoSolicitud.ABIERTA);
    }

    public List<SolicitudDeclaracion> listarPorPersona(Usuario persona) {
        return solicitudRepository.findByPersona(persona);
    }

    public SolicitudDeclaracion buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }

    public SolicitudDeclaracion actualizarEstado(Long id, EstadoSolicitud estado) {
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setEstado(estado);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion editar(Long id, String titulo, String descripcion){
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion(descripcion);
        return solicitudRepository.save(solicitud);
    }

    public void eliminar(Long id){
        solicitudRepository.deleteById(id);
    }

    public List<SolicitudDeclaracion> listarTodas() {
        return solicitudRepository.findAll();
    }
}
