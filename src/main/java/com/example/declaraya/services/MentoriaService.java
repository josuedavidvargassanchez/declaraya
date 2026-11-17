package com.example.declaraya.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoEntrega;
import com.example.declaraya.enums.EstadoSolicitudMentoria;
import com.example.declaraya.enums.EstadoSupervision;
import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.enums.TipoSolicitudMentoria;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.ActividadAcuerdo;
import com.example.declaraya.models.AsignacionMentoria;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.SolicitudMentoria;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.ActividadAcuerdoRepository;
import com.example.declaraya.repositories.AsignacionMentoriaRepository;
import com.example.declaraya.repositories.EntregaTrabajoRepository;
import com.example.declaraya.repositories.SolicitudMentoriaRepository;
import com.example.declaraya.repositories.SupervisionRepository;
import com.example.declaraya.repositories.UsuarioRepository;

/**
 * Mentoria de practicantes: el practicante solicita un mentor (o un cambio), el administrador decide y
 * asigna. Usuario.mentor sigue siendo la fuente de verdad (se actualiza con UsuarioService.asignarMentor);
 * solicitudes_mentoria y asignaciones_mentoria, mas las actividades CAMBIO_MENTOR de los acuerdos,
 * forman el historial. Nada de esto se edita ni se borra.
 */
@Service
public class MentoriaService {

    public static final int TEXTO_MAX = 500;
    public static final int MOTIVO_MIN = 10;
    public static final String NOTA_MIGRACION = "Asignación previa al flujo de solicitudes";
    public static final String VERIFICACION_REQUERIDA = "Tu cuenta debe estar verificada para solicitar un mentor";
    static final String MENSAJE_CAMBIO_DIRECTO = "Cambio de mentor realizado directamente por el administrador";

    /** Mentor que el admin puede elegir para una solicitud. */
    public record MentorElegible(Usuario mentor, long practicantes, boolean coincide) {
    }

    /** Fila de "Mentores" en la gestion del admin. */
    public record MentorResumen(Usuario mentor, long practicantes) {
    }

    /** Fila de "Mis practicantes" en el panel del mentor. */
    public record PracticanteResumen(Usuario practicante, LocalDateTime fechaAsignacion, long porRevisar,
                                     long aprobadas, long correccionesPendientes) {
    }

    /** Un hecho del historial de mentoria de un practicante. */
    public record EventoMentoria(LocalDateTime fecha, String categoria, String titulo, String detalle) {
    }

    private final SolicitudMentoriaRepository solicitudRepository;
    private final AsignacionMentoriaRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final SupervisionRepository supervisionRepository;
    private final EntregaTrabajoRepository entregaRepository;
    private final AcuerdoRepository acuerdoRepository;
    private final ActividadAcuerdoService actividadService;
    private final ActividadAcuerdoRepository actividadRepository;
    private final NotificacionService notificacionService;

    public MentoriaService(SolicitudMentoriaRepository solicitudRepository, AsignacionMentoriaRepository asignacionRepository,
                           UsuarioRepository usuarioRepository, UsuarioService usuarioService,
                           SupervisionRepository supervisionRepository, EntregaTrabajoRepository entregaRepository,
                           AcuerdoRepository acuerdoRepository, ActividadAcuerdoService actividadService,
                           ActividadAcuerdoRepository actividadRepository, NotificacionService notificacionService) {
        this.solicitudRepository = solicitudRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.supervisionRepository = supervisionRepository;
        this.entregaRepository = entregaRepository;
        this.acuerdoRepository = acuerdoRepository;
        this.actividadService = actividadService;
        this.actividadRepository = actividadRepository;
        this.notificacionService = notificacionService;
    }

    // ------------------------------------------------------------ consultas

    public SolicitudMentoria buscarSolicitud(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de mentoría no encontrada"));
    }

    /** Un practicante solo puede ver o cancelar sus propias solicitudes. */
    public SolicitudMentoria solicitudDelPracticante(Long id, Usuario practicante) {
        SolicitudMentoria solicitud = buscarSolicitud(id);
        if (!solicitud.getPracticante().getId().equals(practicante.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta solicitud no es tuya");
        }
        return solicitud;
    }

    public List<SolicitudMentoria> solicitudesDe(Usuario practicante) {
        return solicitudRepository.findByPracticanteOrderByFechaSolicitudDescIdDesc(practicante);
    }

    public Optional<SolicitudMentoria> solicitudPendiente(Usuario practicante) {
        return solicitudRepository.findByPracticanteAndEstado(practicante, EstadoSolicitudMentoria.PENDIENTE).stream().findFirst();
    }

    public Optional<SolicitudMentoria> ultimaSolicitud(Usuario practicante) {
        return solicitudesDe(practicante).stream().findFirst();
    }

    public Optional<AsignacionMentoria> asignacionVigente(Usuario practicante) {
        return asignacionRepository.findByPracticanteAndFechaFinIsNull(practicante).stream()
                .max(Comparator.comparing(AsignacionMentoria::getFechaInicio));
    }

    public List<AsignacionMentoria> asignacionesDe(Usuario practicante) {
        return asignacionRepository.findByPracticanteOrderByFechaInicioDescIdDesc(practicante);
    }

    public List<SolicitudMentoria> solicitudesPendientes() {
        return solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoSolicitudMentoria.PENDIENTE);
    }

    public List<Usuario> practicantes() {
        return usuarioRepository.findByRol(Rol.CONTADOR_PRACTICANTE).stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<Usuario> practicantesSinMentor() {
        return practicantes().stream().filter(p -> p.getMentor() == null).toList();
    }

    public List<MentorResumen> resumenMentores() {
        Map<Long, Long> conteo = practicantesPorMentor();
        return usuarioService.listarMentoresDisponibles().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(m -> new MentorResumen(m, conteo.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    /** Historial de asignaciones, de la mas reciente a la mas antigua; opcionalmente de un practicante. */
    public List<AsignacionMentoria> historialAsignaciones(Long practicanteId) {
        if (practicanteId == null) {
            return asignacionRepository.findAllByOrderByFechaInicioDescIdDesc();
        }
        return asignacionesDe(usuarioService.buscarPorId(practicanteId));
    }

    /**
     * Contadores publicos aprobados distintos del mentor actual. Primero los que tienen la especialidad
     * pedida y luego los que tienen menos practicantes (no hay limite: es solo un orden sugerido).
     */
    public List<MentorElegible> mentoresElegibles(Usuario practicante, String especialidad) {
        Map<Long, Long> conteo = practicantesPorMentor();
        Long actual = practicante.getMentor() == null ? null : practicante.getMentor().getId();
        return usuarioService.listarMentoresDisponibles().stream()
                .filter(m -> !m.getId().equals(actual))
                .map(m -> new MentorElegible(m, conteo.getOrDefault(m.getId(), 0L),
                        especialidad != null && m.getEspecialidadesLista().contains(especialidad)))
                .sorted(Comparator.comparing((MentorElegible e) -> !e.coincide())
                        .thenComparingLong(MentorElegible::practicantes)
                        .thenComparing(e -> e.mentor().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Long, Long> practicantesPorMentor() {
        return usuarioRepository.findByRol(Rol.CONTADOR_PRACTICANTE).stream()
                .filter(p -> p.getMentor() != null)
                .collect(Collectors.groupingBy(p -> p.getMentor().getId(), Collectors.counting()));
    }

    // ------------------------------------------------------------ panel del mentor

    public List<PracticanteResumen> resumenPracticantes(Usuario mentor) {
        List<PracticanteResumen> filas = new ArrayList<>();
        for (Usuario p : usuarioService.listarPracticantesDe(mentor)) {
            List<EntregaTrabajo> entregas = entregaRepository.findByContadorOrderByFechaDesc(p);
            long porRevisar = entregas.stream().filter(e -> e.getEstado() == EstadoEntrega.EN_REVISION_MENTOR).count();
            long aprobadas = entregas.stream().filter(e -> e.getEstado().isVisibleParaCliente()).count();
            filas.add(new PracticanteResumen(p, asignacionVigente(p).map(AsignacionMentoria::getFechaInicio).orElse(null),
                    porRevisar, aprobadas, correccionesPendientes(entregas)));
        }
        filas.sort(Comparator.comparing(f -> f.practicante().getNombre(), String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    // una correccion sigue pendiente mientras sea la ultima entrega de un acuerdo activo (aun no se reenvio)
    static long correccionesPendientes(List<EntregaTrabajo> entregasRecientesPrimero) {
        Map<Long, EntregaTrabajo> ultimaPorAcuerdo = new LinkedHashMap<>();
        for (EntregaTrabajo e : entregasRecientesPrimero) {
            ultimaPorAcuerdo.putIfAbsent(e.getAcuerdo().getId(), e);
        }
        return ultimaPorAcuerdo.values().stream()
                .filter(e -> e.getEstado() == EstadoEntrega.CORRECCION_SOLICITADA && e.getAcuerdo().getEstado() == EstadoAcuerdo.ACTIVO)
                .count();
    }

    /** /contador/practicantes/{id}: solo el mentor vigente del practicante; si no, 403. */
    public Usuario exigirPracticanteDe(Usuario mentor, Long practicanteId) {
        Usuario practicante = usuarioRepository.findById(practicanteId).orElse(null);
        boolean esSuMentor = practicante != null && mentor != null && mentor.getRol() == Rol.CONTADOR_PUBLICO
                && practicante.getRol() == Rol.CONTADOR_PRACTICANTE && practicante.getMentor() != null
                && practicante.getMentor().getId().equals(mentor.getId());
        if (!esSuMentor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el mentor vigente puede ver las supervisiones de este practicante");
        }
        return practicante;
    }

    public List<Acuerdo> acuerdosDe(Usuario practicante) {
        return acuerdoRepository.findByOfertaContador(practicante).stream()
                .sorted(Comparator.comparing(Acuerdo::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<EntregaTrabajo> entregasDe(Usuario practicante) {
        return entregaRepository.findByContadorOrderByFechaDesc(practicante);
    }

    public List<Supervision> supervisionesDe(Usuario practicante) {
        return supervisionRepository.findByPracticante(practicante);
    }

    // ------------------------------------------------------------ historial

    /** Solicitudes, asignaciones y cambios registrados en los acuerdos, del mas reciente al mas antiguo. */
    public List<EventoMentoria> historial(Usuario practicante) {
        List<EventoMentoria> eventos = new ArrayList<>();
        for (SolicitudMentoria s : solicitudesDe(practicante)) {
            boolean directa = MENSAJE_CAMBIO_DIRECTO.equals(s.getMensaje());
            if (!directa) {
                String detalle = (s.getEspecialidad() != null ? "Especialidad: " + s.getEspecialidad() + ". " : "")
                        + (s.isCambio() ? "Motivo: " + s.getMotivoCambio() : s.getMensaje());
                eventos.add(new EventoMentoria(s.getFechaSolicitud(), "Solicitud",
                        s.getTipo().getEtiqueta() + ": solicitud enviada", detalle));
            }
            LocalDateTime cierre = s.getFechaRevision() != null ? s.getFechaRevision() : s.getFechaSolicitud();
            switch (s.getEstado()) {
                case APROBADA -> eventos.add(new EventoMentoria(cierre, "Solicitud",
                        directa ? "Cambio de mentor hecho por el administrador" : "Solicitud aprobada",
                        "Mentor asignado: " + nombre(s.getMentorAsignado())
                                + (s.getRevisadaPor() != null ? ". Revisó: " + s.getRevisadaPor().getNombre() : "")
                                + (directa ? ". Motivo: " + s.getMotivoCambio() : "")));
                case RECHAZADA -> eventos.add(new EventoMentoria(cierre, "Solicitud", "Solicitud rechazada",
                        "Motivo: " + s.getMotivoRechazo()));
                case CANCELADA -> eventos.add(new EventoMentoria(cierre, "Solicitud", "Solicitud cancelada por el practicante", null));
                default -> { }
            }
        }
        for (AsignacionMentoria a : asignacionesDe(practicante)) {
            eventos.add(new EventoMentoria(a.getFechaInicio(), "Asignación",
                    "Inicio de la mentoría con " + a.getMentor().getNombre(), a.getObservacion()));
            if (a.getFechaFin() != null) {
                eventos.add(new EventoMentoria(a.getFechaFin(), "Asignación",
                        "Fin de la mentoría con " + a.getMentor().getNombre(), null));
            }
        }
        List<Acuerdo> acuerdos = acuerdoRepository.findByOfertaContador(practicante);
        if (!acuerdos.isEmpty()) {
            for (ActividadAcuerdo act : actividadRepository.findByAcuerdoInAndTipo(acuerdos, TipoActividad.CAMBIO_MENTOR)) {
                eventos.add(new EventoMentoria(act.getFecha(), "Acuerdo",
                        "Acuerdo #" + act.getAcuerdo().getId() + " · " + act.getAcuerdo().getOferta().getSolicitud().getTitulo(),
                        act.getDescripcion()));
            }
        }
        eventos.sort(Comparator.comparing(EventoMentoria::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return eventos;
    }

    private static String nombre(Usuario u) {
        return u == null ? "No registrado" : u.getNombre();
    }

    // ------------------------------------------------------------ practicante

    @Transactional
    public SolicitudMentoria solicitar(Usuario practicante, String especialidad, String mensaje) {
        exigirPuedeSolicitar(practicante);
        if (practicante.getMentor() != null) {
            throw new ReglaNegocioException("Ya tienes un mentor asignado. Si necesitas otro, solicita un cambio de mentor.");
        }
        String tramite = especialidadDelCatalogo(especialidad, true);
        String texto = textoObligatorio(mensaje, "Escribe un mensaje para el administrador");
        SolicitudMentoria solicitud = solicitudRepository.save(
                new SolicitudMentoria(practicante, TipoSolicitudMentoria.NUEVA, tramite, texto));
        notificacionService.notificar(practicante,
                "Enviaste tu solicitud de mentoría. El administrador la revisará pronto.", "/practicante/mentor");
        notificarAdministradores("Nueva solicitud de mentoría de " + practicante.getNombre(), solicitud);
        return solicitud;
    }

    @Transactional
    public SolicitudMentoria solicitarCambio(Usuario practicante, String motivo, String especialidad, String mensaje) {
        exigirPuedeSolicitar(practicante);
        if (practicante.getMentor() == null) {
            throw new ReglaNegocioException("No tienes un mentor asignado. Solicita uno nuevo.");
        }
        String motivoLimpio = textoObligatorio(motivo, "Indica el motivo del cambio de mentor");
        String tramite = especialidadDelCatalogo(especialidad, false);
        String texto = textoOpcional(mensaje);
        SolicitudMentoria solicitud = new SolicitudMentoria(practicante, TipoSolicitudMentoria.CAMBIO, tramite,
                texto != null ? texto : motivoLimpio);
        solicitud.setMotivoCambio(motivoLimpio);
        solicitud.setMentorAnterior(practicante.getMentor());
        SolicitudMentoria guardada = solicitudRepository.save(solicitud);
        notificacionService.notificar(practicante,
                "Enviaste tu solicitud de cambio de mentor. Mientras se revisa, conservas a tu mentor actual.", "/practicante/mentor");
        notificarAdministradores("Nueva solicitud de cambio de mentor de " + practicante.getNombre(), guardada);
        return guardada;
    }

    @Transactional
    public SolicitudMentoria cancelar(Long solicitudId, Usuario practicante, boolean confirmado) {
        SolicitudMentoria solicitud = solicitudDelPracticante(solicitudId, practicante);
        if (!solicitud.isPendiente()) {
            throw new ReglaNegocioException("Solo puedes cancelar una solicitud pendiente");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma que quieres cancelar la solicitud");
        }
        solicitud.setEstado(EstadoSolicitudMentoria.CANCELADA);
        solicitud.setFechaRevision(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    private void exigirPuedeSolicitar(Usuario practicante) {
        if (practicante.getRol() != Rol.CONTADOR_PRACTICANTE) {
            throw new ReglaNegocioException("Solo los contadores practicantes pueden solicitar un mentor");
        }
        if (practicante.getEstadoVerificacion() != EstadoVerificacion.APROBADO) {
            throw new ReglaNegocioException(VERIFICACION_REQUERIDA);
        }
        if (solicitudRepository.existsByPracticanteAndEstado(practicante, EstadoSolicitudMentoria.PENDIENTE)) {
            throw new ReglaNegocioException("Ya tienes una solicitud de mentoría pendiente de revisión");
        }
    }

    // ------------------------------------------------------------ administrador

    @Transactional
    public SolicitudMentoria asignar(Long solicitudId, Long mentorId, Usuario admin, String observacion, boolean confirmado) {
        SolicitudMentoria solicitud = paraDecidir(solicitudId, confirmado);
        Usuario practicante = solicitud.getPracticante();
        Usuario mentor = mentorElegiblePara(practicante, mentorId);
        aplicarAsignacion(solicitud, practicante, mentor, admin, textoOpcional(observacion));
        return solicitud;
    }

    @Transactional
    public SolicitudMentoria rechazar(Long solicitudId, Usuario admin, String motivo, boolean confirmado) {
        SolicitudMentoria solicitud = paraDecidir(solicitudId, confirmado);
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < MOTIVO_MIN || motivoLimpio.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("El motivo del rechazo debe tener entre " + MOTIVO_MIN + " y " + TEXTO_MAX + " caracteres");
        }
        solicitud.setEstado(EstadoSolicitudMentoria.RECHAZADA);
        solicitud.setMotivoRechazo(motivoLimpio);
        solicitud.setRevisadaPor(admin);
        solicitud.setFechaRevision(LocalDateTime.now());
        SolicitudMentoria guardada = solicitudRepository.save(solicitud);
        notificacionService.notificar(solicitud.getPracticante(),
                (solicitud.isCambio()
                        ? "Tu solicitud de cambio de mentor fue rechazada; conservas a tu mentor actual. Motivo: "
                        : "Tu solicitud de mentoría fue rechazada. Motivo: ") + motivoLimpio,
                "/practicante/mentor");
        return guardada;
    }

    /**
     * Cambio hecho por el admin desde la ficha del practicante, sin que el practicante lo pida.
     * Crea una solicitud CAMBIO que queda APROBADA en el mismo paso, para que conste en el historial.
     */
    @Transactional
    public SolicitudMentoria cambiarDirecto(Long practicanteId, Long mentorId, Usuario admin, String motivo, boolean confirmado) {
        if (!confirmado) {
            throw new ReglaNegocioException("Debes confirmar el cambio de mentor");
        }
        Usuario practicante = usuarioService.buscarPorId(practicanteId);
        if (practicante.getRol() != Rol.CONTADOR_PRACTICANTE) {
            throw new ReglaNegocioException("Solo los contadores practicantes tienen mentor");
        }
        if (practicante.getMentor() == null) {
            throw new ReglaNegocioException("Este practicante no tiene mentor: debe enviar una solicitud de mentoría");
        }
        if (solicitudRepository.existsByPracticanteAndEstado(practicante, EstadoSolicitudMentoria.PENDIENTE)) {
            throw new ReglaNegocioException("Este practicante tiene una solicitud pendiente. Resuélvela desde la solicitud.");
        }
        String motivoLimpio = textoObligatorio(motivo, "Indica el motivo del cambio de mentor");
        Usuario mentor = mentorElegiblePara(practicante, mentorId);

        SolicitudMentoria solicitud = new SolicitudMentoria(practicante, TipoSolicitudMentoria.CAMBIO, null, MENSAJE_CAMBIO_DIRECTO);
        solicitud.setMotivoCambio(motivoLimpio);
        solicitud.setMentorAnterior(practicante.getMentor());
        solicitud = solicitudRepository.save(solicitud);
        aplicarAsignacion(solicitud, practicante, mentor, admin, motivoLimpio);
        return solicitud;
    }

    private SolicitudMentoria paraDecidir(Long solicitudId, boolean confirmado) {
        SolicitudMentoria solicitud = buscarSolicitud(solicitudId);
        if (!solicitud.isPendiente()) {
            throw new ReglaNegocioException("Solo se pueden decidir solicitudes pendientes (estado actual: "
                    + solicitud.getEstado().getEtiqueta() + ")");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Debes confirmar la decisión");
        }
        return solicitud;
    }

    private Usuario mentorElegiblePara(Usuario practicante, Long mentorId) {
        if (mentorId == null) {
            throw new ReglaNegocioException("Selecciona un mentor");
        }
        Usuario mentor = usuarioRepository.findById(mentorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mentor no encontrado"));
        if (!UsuarioService.esMentorElegible(mentor)) {
            throw new ReglaNegocioException("El mentor debe ser un contador público con la verificación aprobada");
        }
        if (practicante.getMentor() != null && practicante.getMentor().getId().equals(mentor.getId())) {
            throw new ReglaNegocioException("Ese contador ya es el mentor actual del practicante");
        }
        return mentor;
    }

    /** Un solo paso (dentro de la transaccion del llamador): cierra, crea, actualiza, aprueba, traslada y notifica. */
    private void aplicarAsignacion(SolicitudMentoria solicitud, Usuario practicante, Usuario mentor, Usuario admin,
                                   String observacion) {
        LocalDateTime ahora = LocalDateTime.now();
        Usuario anterior = practicante.getMentor();

        cerrarVigentes(practicante, ahora);
        asignacionRepository.save(new AsignacionMentoria(practicante, mentor, solicitud, admin, ahora, observacion));
        usuarioService.asignarMentor(practicante, mentor.getId());

        solicitud.setEstado(EstadoSolicitudMentoria.APROBADA);
        solicitud.setMentorAsignado(mentor);
        solicitud.setRevisadaPor(admin);
        solicitud.setFechaRevision(ahora);
        if (solicitud.getMentorAnterior() == null) {
            solicitud.setMentorAnterior(anterior);
        }
        solicitudRepository.save(solicitud);

        int trasladados = anterior != null ? trasladarTrabajoPendiente(practicante, anterior, mentor, admin) : 0;

        boolean directa = MENSAJE_CAMBIO_DIRECTO.equals(solicitud.getMensaje());
        String alPracticante;
        if (directa) {
            alPracticante = "El administrador cambió tu mentor. Tu nuevo mentor es " + mentor.getNombre() + ".";
        } else if (anterior != null) {
            alPracticante = "Se aplicó tu cambio de mentor. Tu nuevo mentor es " + mentor.getNombre() + ".";
        } else {
            alPracticante = "Tu solicitud de mentoría fue aprobada. Tu mentor es " + mentor.getNombre() + ".";
        }
        notificacionService.notificar(practicante, alPracticante, "/practicante/mentor");
        notificacionService.notificar(mentor, "Se te asignó como mentor de " + practicante.getNombre()
                        + (trasladados > 0 ? ". Recibes " + trasladados + " pendiente(s) de revisión de su mentor anterior." : "."),
                "/contador/practicantes/" + practicante.getId());
        if (anterior != null) {
            notificacionService.notificar(anterior, practicante.getNombre()
                            + " dejó de ser tu practicante por un cambio de mentor. Conservas los casos que ya aprobaste.",
                    "/contador/inicio");
        }
    }

    /**
     * Supervisiones por aprobar y entregas en revision pasan al mentor nuevo; las supervisiones ya
     * aprobadas conservan al mentor que las aprobo. Cada acuerdo afectado lo registra en su historial.
     */
    private int trasladarTrabajoPendiente(Usuario practicante, Usuario anterior, Usuario nuevo, Usuario admin) {
        Map<Long, Acuerdo> acuerdos = new LinkedHashMap<>();
        Map<Long, List<String>> notas = new HashMap<>();
        int trasladados = 0;
        for (Supervision supervision : supervisionRepository.findByPracticanteAndEstado(practicante, EstadoSupervision.PENDIENTE_APROBACION)) {
            supervision.setMentor(nuevo);
            supervisionRepository.save(supervision);
            trasladados++;
            anotar(acuerdos, notas, supervision.getAcuerdo(), "la aprobación del caso pasa a " + nuevo.getNombre());
        }
        for (EntregaTrabajo entrega : entregaRepository.findByContadorAndEstado(practicante, EstadoEntrega.EN_REVISION_MENTOR)) {
            entrega.setMentor(nuevo);
            entregaRepository.save(entrega);
            trasladados++;
            anotar(acuerdos, notas, entrega.getAcuerdo(), "la entrega en revisión pasa a " + nuevo.getNombre());
        }
        for (Supervision supervision : supervisionRepository.findByPracticanteAndEstado(practicante, EstadoSupervision.APROBADO)) {
            if (supervision.getAcuerdo().getEstado() == EstadoAcuerdo.ACTIVO) {
                anotar(acuerdos, notas, supervision.getAcuerdo(), "la aprobación del caso se conserva a nombre de "
                        + supervision.getMentor().getNombre() + " y el seguimiento pasa a " + nuevo.getNombre());
            }
        }
        for (Acuerdo acuerdo : acuerdos.values()) {
            actividadService.registrar(acuerdo, admin, TipoActividad.CAMBIO_MENTOR, "Cambio de mentor de "
                    + anterior.getNombre() + " a " + nuevo.getNombre() + ": " + String.join("; ", notas.get(acuerdo.getId())) + ".");
        }
        return trasladados;
    }

    private static void anotar(Map<Long, Acuerdo> acuerdos, Map<Long, List<String>> notas, Acuerdo acuerdo, String nota) {
        acuerdos.putIfAbsent(acuerdo.getId(), acuerdo);
        notas.computeIfAbsent(acuerdo.getId(), k -> new ArrayList<>()).add(nota);
    }

    private void cerrarVigentes(Usuario practicante, LocalDateTime fecha) {
        for (AsignacionMentoria vigente : asignacionRepository.findByPracticanteAndFechaFinIsNull(practicante)) {
            vigente.setFechaFin(fecha);
            asignacionRepository.save(vigente);
        }
    }

    private void notificarAdministradores(String mensaje, SolicitudMentoria solicitud) {
        for (Usuario admin : usuarioRepository.findByRol(Rol.ADMIN)) {
            notificacionService.notificar(admin, mensaje, "/admin/mentorias/" + solicitud.getId());
        }
    }

    // ------------------------------------------------------------ migracion y sincronizacion

    /**
     * Al arrancar: cierra las asignaciones vigentes que ya no coinciden con Usuario.mentor y crea la
     * asignacion vigente de cada practicante que ya tenia mentor (fechaInicio = ahora).
     */
    @Transactional
    public int migrarAsignacionesPrevias() {
        LocalDateTime ahora = LocalDateTime.now();
        for (AsignacionMentoria vigente : asignacionRepository.findByFechaFinIsNull()) {
            Usuario p = vigente.getPracticante();
            boolean coincide = p.getRol() == Rol.CONTADOR_PRACTICANTE && p.getMentor() != null
                    && p.getMentor().getId().equals(vigente.getMentor().getId());
            if (!coincide) {
                vigente.setFechaFin(ahora);
                asignacionRepository.save(vigente);
            }
        }
        int creadas = 0;
        for (Usuario p : usuarioRepository.findByRol(Rol.CONTADOR_PRACTICANTE)) {
            if (registrarAsignacionPrevia(p, ahora)) {
                creadas++;
            }
        }
        return creadas;
    }

    /** Asignacion vigente para un practicante que ya tenia mentor antes de este flujo (sin solicitud ni admin). */
    @Transactional
    public boolean registrarAsignacionPrevia(Usuario practicante, LocalDateTime fechaInicio) {
        if (practicante.getRol() != Rol.CONTADOR_PRACTICANTE || practicante.getMentor() == null
                || !asignacionRepository.findByPracticanteAndFechaFinIsNull(practicante).isEmpty()) {
            return false;
        }
        asignacionRepository.save(new AsignacionMentoria(practicante, practicante.getMentor(), null, null,
                fechaInicio, NOTA_MIGRACION));
        return true;
    }

    /** Tras editar un usuario en el admin: si ya no es practicante con mentor, se cierra su asignacion vigente. */
    @Transactional
    public void sincronizarAsignacion(Usuario usuario) {
        if (usuario.getRol() == Rol.CONTADOR_PRACTICANTE && usuario.getMentor() != null) {
            return;
        }
        cerrarVigentes(usuario, LocalDateTime.now());
    }

    // ------------------------------------------------------------ validaciones de texto

    private static String especialidadDelCatalogo(String especialidad, boolean obligatoria) {
        String limpia = especialidad == null ? "" : especialidad.trim();
        if (limpia.isEmpty()) {
            if (obligatoria) {
                throw new ReglaNegocioException("Elige la especialidad en la que necesitas mentoría");
            }
            return null;
        }
        if (!CatalogoRequisitos.TRAMITES.contains(limpia)) {
            throw new ReglaNegocioException("Elige una especialidad del catálogo de trámites");
        }
        return limpia;
    }

    private static String textoObligatorio(String texto, String mensajeSiFalta) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException(mensajeSiFalta);
        }
        if (limpio.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("El texto no puede superar " + TEXTO_MAX + " caracteres");
        }
        return limpio;
    }

    private static String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("La observación no puede superar " + TEXTO_MAX + " caracteres");
        }
        return limpio;
    }
}
