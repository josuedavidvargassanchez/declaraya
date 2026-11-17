package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.declaraya.enums.AccionDisputa;
import com.example.declaraya.enums.EstadoDisputa;
import com.example.declaraya.enums.ResultadoDisputa;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.HistorialDisputa;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.DisputaRepository;
import com.example.declaraya.repositories.HistorialDisputaRepository;
import com.example.declaraya.repositories.UsuarioRepository;

@Service
public class DisputaService {

    // una disputa "activa" todavia espera la decision del administrador
    public static final List<EstadoDisputa> ESTADOS_ACTIVOS = List.of(EstadoDisputa.ABIERTA, EstadoDisputa.EN_REVISION);
    public static final List<EstadoDisputa> ESTADOS_FINALES = List.of(EstadoDisputa.RESUELTA, EstadoDisputa.CERRADA);

    public static final String DESTINO_CLIENTE = "CLIENTE";
    public static final String DESTINO_CONTADOR = "CONTADOR";
    public static final String TIPO_SOLICITUD_INFO = "SOLICITUD_INFO";
    public static final String TIPO_MENSAJE = "MENSAJE";

    public static final int MENSAJE_MAX = 1000;
    public static final int JUSTIFICACION_MIN = 20;
    public static final int JUSTIFICACION_MAX = 1000;

    @Autowired
    private DisputaRepository disputaRepository;

    @Autowired
    private HistorialDisputaRepository historialRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ActividadAcuerdoService actividadService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Disputa abrir(Acuerdo acuerdo, Usuario usuario, String motivo){
        Disputa disputa = new Disputa();
        disputa.setAcuerdo(acuerdo);
        disputa.setAbiertaPor(usuario);
        disputa.setMotivo(motivo);
        Disputa guardada = disputaRepository.save(disputa);
        registrar(guardada, usuario, AccionDisputa.APERTURA, null, guardada.getEstado(), null, motivo);
        actividadService.registrar(acuerdo, usuario, TipoActividad.DISPUTA_ABIERTA, motivo);

        // aviso a la otra parte y a los administradores
        Usuario cliente = acuerdo.getOferta().getSolicitud().getPersona();
        Usuario contador = acuerdo.getOferta().getContador();
        Usuario otraParte = cliente.getId().equals(usuario.getId()) ? contador : cliente;
        notificacionService.notificar(otraParte,
                usuario.getNombre() + " abrió una disputa en el acuerdo #" + acuerdo.getId(),
                "/acuerdos/" + acuerdo.getId());
        for (Usuario admin : usuarioRepository.findByRol(Rol.ADMIN)) {
            notificacionService.notificar(admin, "Nueva disputa en el acuerdo #" + acuerdo.getId(),
                    "/admin/disputas/" + guardada.getId());
        }
        return guardada;
    }

    // ABIERTA o EN_REVISION: mientras tanto el admin puede ver el chat y no se abre otra disputa
    public boolean hayDisputaAbierta(Acuerdo acuerdo){
        return disputaRepository.existsByAcuerdoAndEstadoIn(acuerdo, ESTADOS_ACTIVOS);
    }

    public List<Disputa> listarActivas(){
        return disputaRepository.findByEstadoInOrderByFechaCreacionDesc(ESTADOS_ACTIVOS);
    }

    public List<Disputa> listarCerradas(){
        return disputaRepository.findByEstadoInOrderByFechaCreacionDesc(ESTADOS_FINALES).stream()
                .sorted(Comparator.comparing(Disputa::getFechaResolucion, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Disputa buscarPorId(Long id){
        return disputaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Disputa no encontrada"));
    }

    public List<HistorialDisputa> historial(Disputa disputa){
        return historialRepository.findByDisputaOrderByFechaAscIdAsc(disputa);
    }

    /**
     * El administrador escribe a una de las partes. El mensaje queda en el chat del acuerdo
     * (para que respondan ahi mismo), se notifica al destinatario y se registra en el historial.
     * Si la disputa estaba ABIERTA pasa a EN_REVISION.
     */
    public Disputa enviarMensaje(Long disputaId, Usuario admin, String destinatario, String tipo, String texto){
        Disputa disputa = buscarPorId(disputaId);
        if (!disputa.isActiva()) {
            throw new ReglaNegocioException("La disputa ya fue resuelta; no admite más mensajes");
        }
        boolean aCliente = DESTINO_CLIENTE.equals(destinatario);
        if (!aCliente && !DESTINO_CONTADOR.equals(destinatario)) {
            throw new ReglaNegocioException("Elige si el mensaje es para el cliente o para el contador");
        }
        boolean solicitud = TIPO_SOLICITUD_INFO.equals(tipo);
        if (!solicitud && !TIPO_MENSAJE.equals(tipo)) {
            throw new ReglaNegocioException("Elige el tipo de mensaje");
        }
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("Escribe el mensaje");
        }
        if (limpio.length() > MENSAJE_MAX) {
            throw new ReglaNegocioException("El mensaje no puede superar " + MENSAJE_MAX + " caracteres");
        }

        Acuerdo acuerdo = disputa.getAcuerdo();
        Usuario receptor = aCliente ? acuerdo.getOferta().getSolicitud().getPersona() : acuerdo.getOferta().getContador();

        String prefijo = aCliente ? "[Administración → Cliente] " : "[Administración → Contador] ";
        String contenido = prefijo + (solicitud ? "Solicitud de información: " : "") + limpio;
        Chat chat = chatService.buscarPorAcuerdoId(acuerdo.getId());
        chatService.enviarMensaje(chat, admin, contenido);

        notificacionService.notificar(receptor,
                (solicitud ? "La administración te pide información" : "La administración te envió un mensaje")
                        + " sobre la disputa del acuerdo #" + acuerdo.getId(),
                "/acuerdos/" + acuerdo.getId() + "/chat");

        EstadoDisputa anterior = disputa.getEstado();
        if (anterior == EstadoDisputa.ABIERTA) {
            disputa.setEstado(EstadoDisputa.EN_REVISION);
        }
        Disputa guardada = disputaRepository.save(disputa);

        AccionDisputa accion = solicitud
                ? (aCliente ? AccionDisputa.SOLICITUD_INFO_CLIENTE : AccionDisputa.SOLICITUD_INFO_CONTADOR)
                : (aCliente ? AccionDisputa.MENSAJE_CLIENTE : AccionDisputa.MENSAJE_CONTADOR);
        registrar(guardada, admin, accion, anterior, guardada.getEstado(), null, limpio);
        return guardada;
    }

    /**
     * Decision final del administrador. No toca el acuerdo ni dinero alguno: la plataforma
     * no maneja reembolsos, solo deja constancia de la decision y su justificacion.
     */
    public Disputa resolver(Long disputaId, Usuario admin, ResultadoDisputa resultado, String justificacion){
        Disputa disputa = buscarPorId(disputaId);
        if (!disputa.isActiva()) {
            throw new ReglaNegocioException("Esta disputa ya fue resuelta o cerrada");
        }
        if (resultado == null) {
            throw new ReglaNegocioException("Elige una decisión");
        }
        String limpia = justificacion == null ? "" : justificacion.trim();
        if (limpia.length() < JUSTIFICACION_MIN || limpia.length() > JUSTIFICACION_MAX) {
            throw new ReglaNegocioException("La justificación debe tener entre " + JUSTIFICACION_MIN
                    + " y " + JUSTIFICACION_MAX + " caracteres");
        }

        EstadoDisputa anterior = disputa.getEstado();
        disputa.setEstado(resultado.estadoFinal());
        disputa.setResultado(resultado);
        disputa.setJustificacion(limpia);
        disputa.setFechaResolucion(LocalDateTime.now());
        disputa.setResueltaPor(admin);
        Disputa guardada = disputaRepository.save(disputa);

        registrar(guardada, admin, AccionDisputa.RESOLUCION, anterior, guardada.getEstado(), resultado, limpia);

        Acuerdo acuerdo = guardada.getAcuerdo();
        actividadService.registrar(acuerdo, admin, TipoActividad.DISPUTA_RESUELTA,
                "Decisión: " + resultado.getEtiqueta() + ". " + limpia);
        String aviso = "La disputa del acuerdo #" + acuerdo.getId() + " fue cerrada por la administración. Decisión: "
                + resultado.getEtiqueta() + ". " + limpia;
        String enlace = "/acuerdos/" + acuerdo.getId() + "/chat";
        notificacionService.notificar(acuerdo.getOferta().getSolicitud().getPersona(), aviso, enlace);
        notificacionService.notificar(acuerdo.getOferta().getContador(), aviso, enlace);
        return guardada;
    }

    private void registrar(Disputa disputa, Usuario actor, AccionDisputa accion, EstadoDisputa anterior,
                           EstadoDisputa nuevo, ResultadoDisputa decision, String detalle) {
        String recortado = detalle != null && detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle;
        historialRepository.save(new HistorialDisputa(disputa, actor, accion, anterior, nuevo, decision, recortado));
    }
}
