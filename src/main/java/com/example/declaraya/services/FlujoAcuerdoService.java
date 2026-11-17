package com.example.declaraya.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoEntrega;
import com.example.declaraya.enums.EstadoRequerimiento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.repositories.EntregaTrabajoRepository;
import com.example.declaraya.enums.EtapaAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.RequerimientoDocumento;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.DocumentoClienteRepository;
import com.example.declaraya.repositories.RequerimientoDocumentoRepository;

/**
 * Flujo de trabajo del acuerdo: etapas, requerimientos de documentos, entregas y recepcion.
 * Toda transicion de etapa pasa por cambiarEtapa, que solo acepta los pasos permitidos;
 * ninguna etapa avanza sola sin la accion que la justifica.
 */
@Service
public class FlujoAcuerdoService {

    private static final Map<EtapaAcuerdo, Set<EtapaAcuerdo>> TRANSICIONES = Map.of(
            EtapaAcuerdo.APROBACION_MENTOR, EnumSet.of(EtapaAcuerdo.DOCUMENTACION),
            EtapaAcuerdo.DOCUMENTACION, EnumSet.of(EtapaAcuerdo.PREPARACION),
            EtapaAcuerdo.PREPARACION, EnumSet.of(EtapaAcuerdo.REVISION_MENTOR, EtapaAcuerdo.ENTREGADO),
            EtapaAcuerdo.REVISION_MENTOR, EnumSet.of(EtapaAcuerdo.ENTREGADO, EtapaAcuerdo.PREPARACION),
            EtapaAcuerdo.ENTREGADO, EnumSet.of(EtapaAcuerdo.FINALIZADO, EtapaAcuerdo.PREPARACION),
            EtapaAcuerdo.FINALIZADO, EnumSet.noneOf(EtapaAcuerdo.class));

    public static final int REQUERIMIENTO_NOMBRE_MAX = 120;
    public static final int TEXTO_MAX = 500;

    private final AcuerdoRepository acuerdoRepository;
    private final AcuerdoService acuerdoService;
    private final ActividadAcuerdoService actividadService;
    private final NotificacionService notificacionService;
    private final SupervisionService supervisionService;
    private final RequerimientoDocumentoRepository requerimientoRepository;
    private final DocumentoClienteRepository documentoRepository;
    private final CatalogoRequisitos catalogo;
    private final EntregaTrabajoRepository entregaRepository;
    private final ArchivoService archivoService;
    private final SolicitudDeclaracionService solicitudService;

    public FlujoAcuerdoService(AcuerdoRepository acuerdoRepository, AcuerdoService acuerdoService,
                               ActividadAcuerdoService actividadService, NotificacionService notificacionService,
                               SupervisionService supervisionService, RequerimientoDocumentoRepository requerimientoRepository,
                               DocumentoClienteRepository documentoRepository, CatalogoRequisitos catalogo,
                               EntregaTrabajoRepository entregaRepository, ArchivoService archivoService,
                               SolicitudDeclaracionService solicitudService) {
        this.acuerdoRepository = acuerdoRepository;
        this.acuerdoService = acuerdoService;
        this.actividadService = actividadService;
        this.notificacionService = notificacionService;
        this.supervisionService = supervisionService;
        this.requerimientoRepository = requerimientoRepository;
        this.documentoRepository = documentoRepository;
        this.catalogo = catalogo;
        this.entregaRepository = entregaRepository;
        this.archivoService = archivoService;
        this.solicitudService = solicitudService;
    }

    // ------------------------------------------------------------ etapas

    public static boolean transicionPermitida(EtapaAcuerdo desde, EtapaAcuerdo hacia) {
        return desde != null && hacia != null && TRANSICIONES.getOrDefault(desde, Set.of()).contains(hacia);
    }

    /** Unico punto donde cambia la etapa: valida el paso, guarda y deja constancia en el historial. */
    public Acuerdo cambiarEtapa(Acuerdo acuerdo, EtapaAcuerdo nueva, Usuario actor) {
        EtapaAcuerdo actual = acuerdo.getEtapa();
        if (!transicionPermitida(actual, nueva)) {
            throw new ReglaNegocioException("No se puede pasar de la etapa "
                    + (actual == null ? "sin etapa" : actual.getEtiqueta()) + " a " + nueva.getEtiqueta());
        }
        acuerdo.setEtapa(nueva);
        Acuerdo guardado = acuerdoRepository.save(acuerdo);
        actividadService.registrar(guardado, actor, TipoActividad.CAMBIO_ETAPA,
                "De " + actual.getEtiqueta() + " a " + nueva.getEtiqueta());
        return guardado;
    }

    /** El mentor aprobo la supervision: el practicante puede empezar a pedir documentos. */
    public Acuerdo mentorAprobo(Acuerdo acuerdo, Usuario mentor) {
        actividadService.registrar(acuerdo, mentor, TipoActividad.MENTOR_APROBO,
                mentor.getNombre() + " aprobó que el practicante atienda este caso");
        Acuerdo resultado = acuerdo;
        if (acuerdo.getEtapa() == EtapaAcuerdo.APROBACION_MENTOR) {
            resultado = cambiarEtapa(acuerdo, EtapaAcuerdo.DOCUMENTACION, mentor);
        }
        notificacionService.notificar(acuerdo.getOferta().getContador(),
                "Tu mentor aprobó el caso \"" + acuerdo.getOferta().getSolicitud().getTitulo() + "\". Ya puedes trabajar en él.",
                "/acuerdos/" + acuerdo.getId());
        return resultado;
    }

    // ------------------------------------------------------------ documentacion (Etapa B)

    public List<RequerimientoDocumento> requerimientos(Acuerdo acuerdo) {
        return requerimientoRepository.findByAcuerdoOrderByFechaSolicitudAsc(acuerdo);
    }

    public long requerimientosPendientes(Acuerdo acuerdo) {
        return requerimientoRepository.countByAcuerdoAndEstado(acuerdo, EstadoRequerimiento.PENDIENTE);
    }

    /** Requisitos obligatorios del catalogo sin documento: solo generan una advertencia. */
    public List<String> obligatoriosFaltantes(Acuerdo acuerdo) {
        SolicitudDeclaracion solicitud = acuerdo.getOferta().getSolicitud();
        return catalogo.obligatoriosFaltantes(solicitud.getTitulo(),
                documentoRepository.findBySolicitudOrderByFechaCargaDesc(solicitud));
    }

    /** El contador del acuerdo o su mentor pide un documento adicional (en DOCUMENTACION o PREPARACION). */
    public RequerimientoDocumento solicitarDocumento(Long acuerdoId, Usuario usuario, String nombre, String descripcion) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(acuerdoId);
        if (!esContadorDelAcuerdo(acuerdo, usuario) && !esMentorDelAcuerdo(acuerdo, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el contador del acuerdo o su mentor pueden pedir documentos");
        }
        if (acuerdo.getEstado() != EstadoAcuerdo.ACTIVO
                || (acuerdo.getEtapa() != EtapaAcuerdo.DOCUMENTACION && acuerdo.getEtapa() != EtapaAcuerdo.PREPARACION)) {
            throw new ReglaNegocioException("Solo se pueden pedir documentos en las etapas de documentación o preparación");
        }
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > REQUERIMIENTO_NOMBRE_MAX) {
            throw new ReglaNegocioException("Indica el documento que necesitas (máximo " + REQUERIMIENTO_NOMBRE_MAX + " caracteres)");
        }
        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (descripcionLimpia != null && descripcionLimpia.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("La descripción no puede superar " + TEXTO_MAX + " caracteres");
        }
        RequerimientoDocumento requerimiento = requerimientoRepository.save(
                new RequerimientoDocumento(acuerdo, usuario, nombreLimpio, descripcionLimpia));
        actividadService.registrar(acuerdo, usuario, TipoActividad.DOCUMENTO_SOLICITADO, "Se pidió: " + nombreLimpio);
        notificacionService.notificar(acuerdo.getOferta().getSolicitud().getPersona(),
                usuario.getNombre() + " te pidió un documento: " + nombreLimpio, "/acuerdos/" + acuerdo.getId());
        return requerimiento;
    }

    /**
     * DOCUMENTACION → PREPARACION. Bloquea si hay documentos solicitados pendientes; los requisitos
     * del catalogo que falten solo se advierten en la vista (el contador decide si continua).
     */
    public Acuerdo iniciarPreparacion(Long acuerdoId, Usuario contador) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(acuerdoId);
        if (!esContadorDelAcuerdo(acuerdo, contador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el contador del acuerdo puede iniciar la preparación");
        }
        if (acuerdo.getEstado() != EstadoAcuerdo.ACTIVO || acuerdo.getEtapa() != EtapaAcuerdo.DOCUMENTACION) {
            throw new ReglaNegocioException("La preparación solo se inicia desde la etapa de documentación");
        }
        long pendientes = requerimientosPendientes(acuerdo);
        if (pendientes > 0) {
            throw new ReglaNegocioException("Hay " + pendientes + " documento(s) solicitado(s) pendiente(s). Espera a que el cliente los suba.");
        }
        return cambiarEtapa(acuerdo, EtapaAcuerdo.PREPARACION, contador);
    }

    // ------------------------------------------------------------ entregas (Etapa C)

    public static final int ENTREGA_DESCRIPCION_MAX = 1000;

    public EntregaTrabajo buscarEntrega(Long acuerdoId, Long entregaId) {
        EntregaTrabajo entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega no encontrada"));
        if (!entrega.getAcuerdo().getId().equals(acuerdoId)) {
            throw new RecursoNoEncontradoException("Entrega no encontrada");
        }
        return entrega;
    }

    /**
     * Ver o descargar una entrega: participantes, mentor y admin con disputa activa (AcuerdoService.puedeVer).
     * El cliente solo ve lo que ya le fue enviado; los borradores en revision del mentor no.
     */
    public boolean puedeVerEntrega(EntregaTrabajo entrega, Usuario usuario) {
        Acuerdo acuerdo = entrega.getAcuerdo();
        if (usuario == null || !acuerdoService.puedeVer(acuerdo, usuario)) {
            return false;
        }
        return !esClienteDelAcuerdo(acuerdo, usuario) || entrega.getEstado().isVisibleParaCliente();
    }

    public void exigirAccesoEntrega(EntregaTrabajo entrega, Usuario usuario) {
        if (!puedeVerEntrega(entrega, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta entrega");
        }
    }

    public List<EntregaTrabajo> entregasVisibles(Acuerdo acuerdo, Usuario usuario) {
        return entregaRepository.findByAcuerdoOrderByFechaDesc(acuerdo).stream()
                .filter(e -> puedeVerEntrega(e, usuario)).toList();
    }

    /** Entrega ENVIADA que espera la respuesta del cliente, si existe. */
    public EntregaTrabajo entregaEnviada(Acuerdo acuerdo) {
        return entregaRepository.findByAcuerdoOrderByFechaDesc(acuerdo).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.ENVIADA).findFirst().orElse(null);
    }

    /** Entrega que espera la revision del mentor, si existe. */
    public EntregaTrabajo entregaEnRevision(Acuerdo acuerdo) {
        return entregaRepository.findByAcuerdoOrderByFechaDesc(acuerdo).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.EN_REVISION_MENTOR).findFirst().orElse(null);
    }

    /** Panel del mentor: entregas de sus practicantes que esperan revision. */
    public List<EntregaTrabajo> entregasPorRevisar(Usuario mentor) {
        return entregaRepository.findByEstadoOrderByFechaAsc(EstadoEntrega.EN_REVISION_MENTOR).stream()
                .filter(e -> esRevisorDe(e, mentor)).toList();
    }

    /**
     * Mentor que revisa la entrega: el guardado en la entrega (se traslada si el practicante cambia de
     * mentor mientras esta en revision) o, en las entregas anteriores a ese campo, el mentor responsable.
     */
    public boolean esRevisorDe(EntregaTrabajo entrega, Usuario usuario) {
        if (usuario == null || usuario.getRol() != Rol.CONTADOR_PUBLICO) {
            return false;
        }
        if (entrega.getMentor() != null) {
            return entrega.getMentor().getId().equals(usuario.getId());
        }
        return esMentorDelAcuerdo(entrega.getAcuerdo(), usuario);
    }

    public Resource cargarArchivoEntrega(EntregaTrabajo entrega) throws IOException {
        return archivoService.cargarDe(ArchivoService.CARPETA_ENTREGAS, entrega.getArchivo());
    }

    /** PREPARACION: un practicante envia a REVISION_MENTOR; un contador publico entrega directo al cliente. */
    public EntregaTrabajo entregar(Long acuerdoId, Usuario contador, String descripcion, MultipartFile archivo) throws IOException {
        Acuerdo acuerdo = acuerdoService.buscarPorId(acuerdoId);
        if (!esContadorDelAcuerdo(acuerdo, contador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el contador del acuerdo puede entregar el trabajo");
        }
        if (acuerdo.getEstado() != EstadoAcuerdo.ACTIVO || acuerdo.getEtapa() != EtapaAcuerdo.PREPARACION) {
            throw new ReglaNegocioException("Solo puedes entregar el trabajo durante la etapa de preparación");
        }
        String texto = descripcion == null ? "" : descripcion.trim();
        if (texto.isEmpty() || texto.length() > ENTREGA_DESCRIPCION_MAX) {
            throw new ReglaNegocioException("Describe lo que entregas (máximo " + ENTREGA_DESCRIPCION_MAX + " caracteres)");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Adjunta el archivo del trabajo");
        }
        boolean practicante = contador.getRol() == Rol.CONTADOR_PRACTICANTE;
        Usuario mentor = practicante ? mentorDe(acuerdo, contador) : null;
        if (practicante && mentor == null) {
            throw new ReglaNegocioException("Necesitas un mentor asignado para entregar el trabajo");
        }
        String guardado;
        try {
            guardado = archivoService.guardarEn(ArchivoService.CARPETA_ENTREGAS, archivo);
        } catch (ArchivoService.ArchivoNoPermitidoException e) {
            throw new ReglaNegocioException(e.getMessage());
        }

        // si el mentor habia pedido una correccion, esta entrega es el reenvio
        boolean reenvio = practicante && entregaRepository.existsByAcuerdoAndEstado(acuerdo, EstadoEntrega.CORRECCION_SOLICITADA);

        EntregaTrabajo entrega = new EntregaTrabajo();
        entrega.setAcuerdo(acuerdo);
        entrega.setContador(contador);
        entrega.setDescripcion(texto);
        entrega.setArchivo(guardado);
        entrega.setNombreOriginal(nombreOriginal(archivo));
        entrega.setEstado(practicante ? EstadoEntrega.EN_REVISION_MENTOR : EstadoEntrega.ENVIADA);
        entrega.setMentor(mentor);
        EntregaTrabajo guardada = entregaRepository.save(entrega);

        actividadService.registrar(acuerdo, contador, TipoActividad.ENTREGA_ENVIADA,
                (practicante ? "Entrega enviada al mentor para revisión: " : "Entrega enviada al cliente: ") + texto);
        String titulo = acuerdo.getOferta().getSolicitud().getTitulo();
        if (practicante) {
            cambiarEtapa(acuerdo, EtapaAcuerdo.REVISION_MENTOR, contador);
            notificacionService.notificar(mentor, contador.getNombre()
                            + (reenvio ? " reenvió la entrega corregida de \"" : " envió una entrega por revisar en \"") + titulo + "\"",
                    "/acuerdos/" + acuerdo.getId());
        } else {
            cambiarEtapa(acuerdo, EtapaAcuerdo.ENTREGADO, contador);
            notificacionService.notificar(acuerdo.getOferta().getSolicitud().getPersona(),
                    contador.getNombre() + " entregó el trabajo de \"" + titulo + "\". Revísalo y confirma la recepción.",
                    "/acuerdos/" + acuerdo.getId());
        }
        return guardada;
    }

    /** El mentor aprueba: la entrega pasa al cliente (ENVIADA) y la etapa a ENTREGADO. */
    public EntregaTrabajo aprobarEntrega(Long acuerdoId, Long entregaId, Usuario mentor, String observacion, boolean confirmado) {
        EntregaTrabajo entrega = entregaParaMentor(acuerdoId, entregaId, mentor, confirmado);
        String obs = textoOpcional(observacion);
        entrega.setEstado(EstadoEntrega.ENVIADA);
        entrega.setObservacionMentor(obs);
        entrega.setFechaRespuesta(LocalDateTime.now());
        EntregaTrabajo guardada = entregaRepository.save(entrega);

        Acuerdo acuerdo = entrega.getAcuerdo();
        actividadService.registrar(acuerdo, mentor, TipoActividad.ENTREGA_APROBADA,
                "El mentor aprobó la entrega" + (obs != null ? ": " + obs : ""));
        cambiarEtapa(acuerdo, EtapaAcuerdo.ENTREGADO, mentor);
        String titulo = acuerdo.getOferta().getSolicitud().getTitulo();
        notificacionService.notificar(acuerdo.getOferta().getSolicitud().getPersona(),
                "Recibiste el trabajo de \"" + titulo + "\". Revísalo y confirma la recepción.", "/acuerdos/" + acuerdo.getId());
        notificacionService.notificar(entrega.getContador(),
                "Tu mentor aprobó tu entrega de \"" + titulo + "\" y ya la recibió el cliente.", "/acuerdos/" + acuerdo.getId());
        return guardada;
    }

    /** El mentor pide correccion (observacion obligatoria): el practicante vuelve a PREPARACION. */
    public EntregaTrabajo corregirEntrega(Long acuerdoId, Long entregaId, Usuario mentor, String observacion, boolean confirmado) {
        EntregaTrabajo entrega = entregaParaMentor(acuerdoId, entregaId, mentor, confirmado);
        String obs = observacion == null ? "" : observacion.trim();
        if (obs.isEmpty() || obs.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("Indica qué debe corregir el practicante (máximo " + TEXTO_MAX + " caracteres)");
        }
        entrega.setEstado(EstadoEntrega.CORRECCION_SOLICITADA);
        entrega.setObservacionMentor(obs);
        entrega.setFechaRespuesta(LocalDateTime.now());
        EntregaTrabajo guardada = entregaRepository.save(entrega);

        Acuerdo acuerdo = entrega.getAcuerdo();
        actividadService.registrar(acuerdo, mentor, TipoActividad.CORRECCION_MENTOR, obs);
        cambiarEtapa(acuerdo, EtapaAcuerdo.PREPARACION, mentor);
        notificacionService.notificar(entrega.getContador(),
                "Tu mentor pidió una corrección en \"" + acuerdo.getOferta().getSolicitud().getTitulo() + "\": " + obs,
                "/acuerdos/" + acuerdo.getId());
        return guardada;
    }

    /** El cliente confirma la recepcion: la entrega queda RECIBIDA y el acuerdo FINALIZADO. */
    public Acuerdo confirmarRecepcion(Long acuerdoId, Long entregaId, Usuario cliente, boolean confirmado) {
        EntregaTrabajo entrega = entregaParaCliente(acuerdoId, entregaId, cliente, confirmado);
        entrega.setEstado(EstadoEntrega.RECIBIDA);
        entrega.setFechaRespuesta(LocalDateTime.now());
        entregaRepository.save(entrega);

        Acuerdo acuerdo = entrega.getAcuerdo();
        actividadService.registrar(acuerdo, cliente, TipoActividad.RECEPCION_CONFIRMADA,
                "El cliente confirmó la recepción del trabajo");
        Acuerdo actualizado = cambiarEtapa(acuerdo, EtapaAcuerdo.FINALIZADO, cliente);
        actualizado = acuerdoService.finalizar(actualizado);
        SolicitudDeclaracion solicitud = acuerdo.getOferta().getSolicitud();
        solicitudService.actualizarEstado(solicitud.getId(), EstadoSolicitud.CERRADA);
        notificacionService.notificar(acuerdo.getOferta().getContador(),
                cliente.getNombre() + " confirmó la recepción de \"" + solicitud.getTitulo() + "\". El acuerdo quedó finalizado.",
                "/acuerdos/" + acuerdo.getId());
        return actualizado;
    }

    /** El cliente reporta un inconveniente: la entrega queda CON_INCONVENIENTE y se vuelve a PREPARACION. */
    public EntregaTrabajo reportarInconveniente(Long acuerdoId, Long entregaId, Usuario cliente, String descripcion,
                                                boolean confirmado) {
        EntregaTrabajo entrega = entregaParaCliente(acuerdoId, entregaId, cliente, confirmado);
        String texto = descripcion == null ? "" : descripcion.trim();
        if (texto.isEmpty() || texto.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("Describe el inconveniente (máximo " + TEXTO_MAX + " caracteres)");
        }
        entrega.setEstado(EstadoEntrega.CON_INCONVENIENTE);
        entrega.setObservacionCliente(texto);
        entrega.setFechaRespuesta(LocalDateTime.now());
        EntregaTrabajo guardada = entregaRepository.save(entrega);

        Acuerdo acuerdo = entrega.getAcuerdo();
        actividadService.registrar(acuerdo, cliente, TipoActividad.INCONVENIENTE_REPORTADO, texto);
        cambiarEtapa(acuerdo, EtapaAcuerdo.PREPARACION, cliente);
        notificacionService.notificar(acuerdo.getOferta().getContador(),
                cliente.getNombre() + " reportó un inconveniente con la entrega: " + texto, "/acuerdos/" + acuerdo.getId());
        return guardada;
    }

    private EntregaTrabajo entregaParaMentor(Long acuerdoId, Long entregaId, Usuario mentor, boolean confirmado) {
        EntregaTrabajo entrega = buscarEntrega(acuerdoId, entregaId);
        if (!esRevisorDe(entrega, mentor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el mentor del practicante revisa esta entrega");
        }
        if (entrega.getEstado() != EstadoEntrega.EN_REVISION_MENTOR) {
            throw new ReglaNegocioException("Esta entrega ya fue revisada");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma que revisaste la entrega");
        }
        return entrega;
    }

    private EntregaTrabajo entregaParaCliente(Long acuerdoId, Long entregaId, Usuario cliente, boolean confirmado) {
        EntregaTrabajo entrega = buscarEntrega(acuerdoId, entregaId);
        if (!esClienteDelAcuerdo(entrega.getAcuerdo(), cliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente del acuerdo puede responder la entrega");
        }
        if (entrega.getEstado() != EstadoEntrega.ENVIADA || entrega.getAcuerdo().getEtapa() != EtapaAcuerdo.ENTREGADO) {
            throw new ReglaNegocioException("Esta entrega ya fue respondida");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma tu respuesta marcando la casilla");
        }
        return entrega;
    }

    // el mentor vigente del practicante; si no tiene, el de la supervision del acuerdo
    private Usuario mentorDe(Acuerdo acuerdo, Usuario practicante) {
        if (practicante.getMentor() != null) {
            return practicante.getMentor();
        }
        return supervisionService.buscarPorAcuerdo(acuerdo).map(Supervision::getMentor).orElse(null);
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

    private static String nombreOriginal(MultipartFile archivo) {
        String original = StringUtils.getFilename(StringUtils.cleanPath(archivo.getOriginalFilename()));
        if (original == null || original.isBlank()) {
            return "entrega";
        }
        return original.length() > 200 ? original.substring(original.length() - 200) : original;
    }

    // ------------------------------------------------------------ roles dentro del acuerdo

    public boolean esContadorDelAcuerdo(Acuerdo acuerdo, Usuario usuario) {
        return usuario != null && acuerdo.getOferta().getContador().getId().equals(usuario.getId());
    }

    public boolean esClienteDelAcuerdo(Acuerdo acuerdo, Usuario usuario) {
        return usuario != null && acuerdo.getOferta().getSolicitud().getPersona().getId().equals(usuario.getId());
    }

    // mentor que actua sobre el acuerdo (el vigente del practicante); ver SupervisionService.esMentorResponsable
    public boolean esMentorDelAcuerdo(Acuerdo acuerdo, Usuario usuario) {
        return usuario != null && usuario.getRol() == Rol.CONTADOR_PUBLICO && supervisionService.esMentorResponsable(usuario, acuerdo);
    }

    /** Lanza 403 si el usuario no puede ver el acuerdo (participantes, mentor, admin con disputa activa). */
    public Acuerdo exigirVisible(Long acuerdoId, Usuario usuario) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(acuerdoId);
        if (!acuerdoService.puedeVer(acuerdo, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este acuerdo");
        }
        return acuerdo;
    }
}
