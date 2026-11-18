package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.OfertaRepository;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OfertaService {

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private SolicitudDeclaracionRepository solicitudRepository;

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private SupervisionService supervisionService;

    public static final int MAX_DIAS_ENTREGA = 120;

    /** La fecha estimada debe ser futura y no superar 120 dias. Devuelve true si es valida. */
    public static boolean fechaEntregaValida(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        return fecha != null && fecha.isAfter(hoy) && !fecha.isAfter(hoy.plusDays(MAX_DIAS_ENTREGA));
    }

    public Oferta crear(SolicitudDeclaracion solicitud, Usuario contador, String mensaje, BigDecimal valorPropuesto) {
        return ofertaRepository.save(new Oferta(solicitud, contador, mensaje, valorPropuesto));
    }

    public Oferta crear(SolicitudDeclaracion solicitud, Usuario contador, String mensaje, BigDecimal valorPropuesto,
                        LocalDate fechaEntregaEstimada) {
        Oferta oferta = new Oferta(solicitud, contador, mensaje, valorPropuesto);
        oferta.setFechaEntregaEstimada(fechaEntregaEstimada);
        return ofertaRepository.save(oferta);
    }

    public List<Oferta> listarPorSolicitud(SolicitudDeclaracion solicitud) {
        return ofertaRepository.findBySolicitud(solicitud);
    }

    public List<Oferta> listarPorContador(Usuario contador) {
        return ofertaRepository.findByContador(contador);
    }

    public Oferta buscarPorId(Long id) {
        return ofertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
    }

    public Oferta actualizarEstado(Long id, EstadoOferta estado) {
        Oferta oferta = buscarPorId(id);
        oferta.setEstado(estado);
        return ofertaRepository.save(oferta);
    }

    public Oferta editar(Long id, String mensaje, BigDecimal valorPropuesto, LocalDate fechaEntregaEstimada) {
        Oferta oferta = buscarPorId(id);
        oferta.setMensaje(mensaje);
        oferta.setValorPropuesto(valorPropuesto);
        oferta.setFechaEntregaEstimada(fechaEntregaEstimada);
        return ofertaRepository.save(oferta);
    }

    /**
     * La persona acepta una oferta. Todo ocurre en una transaccion que empieza bloqueando la solicitud
     * (SELECT ... FOR UPDATE): si llegan dos aceptaciones a la vez, la segunda espera y, al continuar,
     * ve la solicitud ya EN_PROCESO y falla, asi que nunca se crean dos acuerdos para la misma solicitud.
     */
    @Transactional
    public Acuerdo aceptar(Long ofertaId, Usuario persona) {
        SolicitudDeclaracion solicitud = solicitudRepository.bloquearPorOferta(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
        if (!solicitud.getPersona().getId().equals(persona.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta oferta no es para ti");
        }
        // la oferta se lee despues del bloqueo: refleja lo que haya confirmado otra aceptacion
        Oferta oferta = buscarPorId(ofertaId);
        if (oferta.getEstado() != EstadoOferta.PENDIENTE || solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            throw new ReglaNegocioException("Esta oferta ya no se puede aceptar: la solicitud ya no está abierta o la oferta cambió de estado");
        }

        oferta.setEstado(EstadoOferta.ACEPTADA);
        ofertaRepository.save(oferta);
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        solicitudRepository.save(solicitud);

        for (Oferta otra : ofertaRepository.findBySolicitud(solicitud)) {
            if (!otra.getId().equals(oferta.getId()) && otra.getEstado() == EstadoOferta.PENDIENTE) {
                otra.setEstado(EstadoOferta.RECHAZADA);
                ofertaRepository.save(otra);
                notificacionService.notificar(otra.getContador(),
                        "Tu oferta para \"" + solicitud.getTitulo() + "\" no fue seleccionada",
                        "/solicitudes/" + solicitud.getId());
            }
        }

        Acuerdo acuerdo = acuerdoService.crearDesdeOferta(oferta);
        Usuario contador = oferta.getContador();
        notificacionService.notificar(contador,
                "¡Tu oferta para \"" + solicitud.getTitulo() + "\" fue aceptada! Ya puedes chatear con el cliente",
                "/acuerdos/" + acuerdo.getId());
        notificacionService.notificar(solicitud.getPersona(),
                "Se creó el acuerdo #" + acuerdo.getId() + " con " + contador.getNombre() + " para \"" + solicitud.getTitulo() + "\"",
                "/acuerdos/" + acuerdo.getId());
        if (contador.getRol() == Rol.CONTADOR_PRACTICANTE && contador.getMentor() != null) {
            supervisionService.asignar(contador, contador.getMentor(), acuerdo);
            notificacionService.notificar(contador.getMentor(),
                    "Tu practicante " + contador.getNombre() + " tiene un caso nuevo por aprobar",
                    "/acuerdos/" + acuerdo.getId());
        }
        return acuerdo;
    }

    public void eliminarPorSolicitud(SolicitudDeclaracion solicitud){
        ofertaRepository.deleteAll(ofertaRepository.findBySolicitud(solicitud));
    }

    public List<Oferta> listarTodas() {
        return ofertaRepository.findAll();
    }
}
