package com.example.declaraya.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.EtapaAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoDocumentoCliente;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.ActividadAcuerdoService;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.CalificacionService;
import com.example.declaraya.services.CatalogoRequisitos;
import com.example.declaraya.services.DisputaService;
import com.example.declaraya.services.DocumentoClienteService;
import com.example.declaraya.services.FlujoAcuerdoService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;

/**
 * Detalle del acuerdo (GET /acuerdos/{id}) y sus acciones: pedir documentos, iniciar la preparacion,
 * entregar, revisar como mentor, responder como cliente y calificar. Los permisos viven en los servicios.
 */
@Controller
public class AcuerdoDetalleController {

    private final FlujoAcuerdoService flujoService;
    private final ActividadAcuerdoService actividadService;
    private final DocumentoClienteService documentoService;
    private final DisputaService disputaService;
    private final SupervisionService supervisionService;
    private final SolicitudDeclaracionService solicitudService;
    private final CatalogoRequisitos catalogo;
    private final CalificacionService calificacionService;
    private final UsuarioService usuarioService;

    public AcuerdoDetalleController(FlujoAcuerdoService flujoService, ActividadAcuerdoService actividadService,
                                    DocumentoClienteService documentoService, DisputaService disputaService,
                                    SupervisionService supervisionService, SolicitudDeclaracionService solicitudService,
                                    CatalogoRequisitos catalogo, CalificacionService calificacionService,
                                    UsuarioService usuarioService) {
        this.flujoService = flujoService;
        this.actividadService = actividadService;
        this.documentoService = documentoService;
        this.disputaService = disputaService;
        this.supervisionService = supervisionService;
        this.solicitudService = solicitudService;
        this.catalogo = catalogo;
        this.calificacionService = calificacionService;
        this.usuarioService = usuarioService;
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private static String volver(Long id, String parametro) {
        return "redirect:/acuerdos/" + id + "?" + parametro;
    }

    private static String volverConError(Long id, String mensaje) {
        return volver(id, "error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------ detalle

    @GetMapping("/acuerdos/{id}")
    public String detalle(@PathVariable Long id, Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Acuerdo acuerdo = flujoService.exigirVisible(id, usuario);
        SolicitudDeclaracion solicitud = acuerdo.getOferta().getSolicitud();
        Usuario contador = acuerdo.getOferta().getContador();
        boolean practicante = contador.getRol() == Rol.CONTADOR_PRACTICANTE;

        boolean esCliente = flujoService.esClienteDelAcuerdo(acuerdo, usuario);
        boolean esContador = flujoService.esContadorDelAcuerdo(acuerdo, usuario);
        boolean esMentor = flujoService.esMentorDelAcuerdo(acuerdo, usuario);
        boolean activo = acuerdo.getEstado() == EstadoAcuerdo.ACTIVO;
        EtapaAcuerdo etapa = acuerdo.getEtapa();

        model.addAttribute("acuerdo", acuerdo);
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("cliente", solicitud.getPersona());
        model.addAttribute("contador", contador);
        model.addAttribute("esCliente", esCliente);
        model.addAttribute("esContador", esContador);
        model.addAttribute("esMentor", esMentor);
        model.addAttribute("disputaActiva", disputaService.hayDisputaAbierta(acuerdo));
        model.addAttribute("supervision", supervisionService.buscarPorAcuerdo(acuerdo).orElse(null));

        // indicador de pasos: para contadores publicos se omiten las etapas del mentor
        List<EtapaAcuerdo> etapas = Arrays.stream(EtapaAcuerdo.values())
                .filter(e -> practicante || !e.isSoloPracticante()).toList();
        model.addAttribute("etapas", etapas);
        model.addAttribute("indiceEtapa", etapa == null ? -1 : etapas.indexOf(etapa));

        // documentos, lista de verificacion y documentos solicitados
        List<DocumentoCliente> documentos = documentoService.listarPorSolicitud(solicitud);
        model.addAttribute("documentos", documentos);
        model.addAttribute("checklist", catalogo.verificar(solicitud.getTitulo(), documentos));
        model.addAttribute("requerimientos", flujoService.requerimientos(acuerdo));
        model.addAttribute("requisitos", catalogo.requisitosPara(solicitud.getTitulo()));
        model.addAttribute("tipos", TipoDocumentoCliente.values());
        model.addAttribute("solicitudesVinculables", esCliente
                ? solicitudService.listarPorPersona(usuario).stream().filter(s -> s.getEstado() != EstadoSolicitud.CERRADA).toList()
                : List.of());
        model.addAttribute("nombreMax", DocumentoClienteService.NOMBRE_MAX);
        model.addAttribute("puedeSubirDocumentos", esCliente && solicitud.getEstado() != EstadoSolicitud.CERRADA);

        // entregas e historial
        model.addAttribute("entregas", flujoService.entregasVisibles(acuerdo, usuario));
        model.addAttribute("entregaEnviada", flujoService.entregaEnviada(acuerdo));
        model.addAttribute("actividades", actividadService.listar(acuerdo));

        // acciones disponibles segun rol y etapa
        boolean etapaDocumental = etapa == EtapaAcuerdo.DOCUMENTACION || etapa == EtapaAcuerdo.PREPARACION;
        model.addAttribute("puedePedirDocumento", activo && etapaDocumental && (esContador || esMentor));
        model.addAttribute("puedeIniciarPreparacion", activo && esContador && etapa == EtapaAcuerdo.DOCUMENTACION);
        model.addAttribute("requerimientosPendientes", flujoService.requerimientosPendientes(acuerdo));
        model.addAttribute("faltantes", flujoService.obligatoriosFaltantes(acuerdo));
        model.addAttribute("puedeEntregar", activo && esContador && etapa == EtapaAcuerdo.PREPARACION);
        EntregaTrabajo enRevision = flujoService.entregaEnRevision(acuerdo);
        model.addAttribute("entregaEnRevision", enRevision);
        model.addAttribute("puedeRevisarComoMentor", etapa == EtapaAcuerdo.REVISION_MENTOR && enRevision != null
                && flujoService.esRevisorDe(enRevision, usuario));
        model.addAttribute("puedeResponderEntrega", esCliente && etapa == EtapaAcuerdo.ENTREGADO
                && flujoService.entregaEnviada(acuerdo) != null);

        // calificacion (Etapa D)
        model.addAttribute("calificacion", calificacionService.buscarPorAcuerdo(acuerdo).orElse(null));
        model.addAttribute("puedeCalificar", esCliente && calificacionService.puedeCalificar(acuerdo, usuario));

        model.addAttribute("textoMax", FlujoAcuerdoService.TEXTO_MAX);
        model.addAttribute("descripcionMax", FlujoAcuerdoService.ENTREGA_DESCRIPCION_MAX);
        return "acuerdo-detalle";
    }

    // ------------------------------------------------------------ documentacion

    @PostMapping("/acuerdos/{id}/requerimientos")
    public String pedirDocumento(@PathVariable Long id,
                                 @RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 Authentication authentication) {
        try {
            flujoService.solicitarDocumento(id, usuarioActual(authentication), nombre, descripcion);
            return volver(id, "documentoPedido");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/acuerdos/{id}/preparacion")
    public String iniciarPreparacion(@PathVariable Long id, Authentication authentication) {
        try {
            flujoService.iniciarPreparacion(id, usuarioActual(authentication));
            return volver(id, "preparacionIniciada");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    // ------------------------------------------------------------ entregas

    @PostMapping("/acuerdos/{id}/entregas")
    public String entregar(@PathVariable Long id,
                           @RequestParam(required = false) String descripcion,
                           @RequestParam(required = false) MultipartFile archivo,
                           Authentication authentication) {
        try {
            flujoService.entregar(id, usuarioActual(authentication), descripcion, archivo);
            return volver(id, "trabajoEntregado");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        } catch (IOException e) {
            return volverConError(id, "No se pudo guardar el archivo. Intenta de nuevo.");
        }
    }

    @PostMapping("/acuerdos/{id}/entregas/{entregaId}/aprobar")
    public String aprobarEntrega(@PathVariable Long id, @PathVariable Long entregaId,
                                 @RequestParam(required = false) String observacion,
                                 @RequestParam(defaultValue = "false") boolean confirmado,
                                 Authentication authentication) {
        try {
            flujoService.aprobarEntrega(id, entregaId, usuarioActual(authentication), observacion, confirmado);
            return volver(id, "entregaAprobada");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/acuerdos/{id}/entregas/{entregaId}/correccion")
    public String corregirEntrega(@PathVariable Long id, @PathVariable Long entregaId,
                                  @RequestParam(required = false) String observacion,
                                  @RequestParam(defaultValue = "false") boolean confirmado,
                                  Authentication authentication) {
        try {
            flujoService.corregirEntrega(id, entregaId, usuarioActual(authentication), observacion, confirmado);
            return volver(id, "correccionPedida");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/acuerdos/{id}/entregas/{entregaId}/recepcion")
    public String confirmarRecepcion(@PathVariable Long id, @PathVariable Long entregaId,
                                     @RequestParam(defaultValue = "false") boolean confirmado,
                                     Authentication authentication) {
        try {
            flujoService.confirmarRecepcion(id, entregaId, usuarioActual(authentication), confirmado);
            return volver(id, "recepcionConfirmada");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/acuerdos/{id}/entregas/{entregaId}/inconveniente")
    public String reportarInconveniente(@PathVariable Long id, @PathVariable Long entregaId,
                                        @RequestParam(required = false) String descripcion,
                                        @RequestParam(defaultValue = "false") boolean confirmado,
                                        Authentication authentication) {
        try {
            flujoService.reportarInconveniente(id, entregaId, usuarioActual(authentication), descripcion, confirmado);
            return volver(id, "inconvenienteReportado");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    // archivo de una entrega: participantes, mentor y admin con disputa activa; nunca la ruta del disco
    @GetMapping({"/acuerdos/{id}/entregas/{entregaId}", "/acuerdos/{id}/entregas/{entregaId}/descargar"})
    public ResponseEntity<Resource> archivoEntrega(@PathVariable Long id, @PathVariable Long entregaId,
                                                   Authentication authentication,
                                                   jakarta.servlet.http.HttpServletRequest request) throws IOException {
        EntregaTrabajo entrega = flujoService.buscarEntrega(id, entregaId);
        flujoService.exigirAccesoEntrega(entrega, usuarioActual(authentication));
        Resource recurso = flujoService.cargarArchivoEntrega(entrega);
        boolean adjunto = request.getRequestURI().endsWith("/descargar");
        ContentDisposition disposicion = (adjunto ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(entrega.getNombreOriginal(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .cacheControl(CacheControl.noStore())
                .contentType(ArchivoService.tipoDeContenido(entrega.getArchivo()))
                .body(recurso);
    }

    // ------------------------------------------------------------ calificacion

    @PostMapping("/acuerdos/{id}/calificacion")
    public String calificar(@PathVariable Long id,
                            @RequestParam(required = false) Integer puntaje,
                            @RequestParam(required = false) String comentario,
                            @RequestParam(defaultValue = "false") boolean confirmado,
                            Authentication authentication) {
        try {
            calificacionService.calificar(id, usuarioActual(authentication), puntaje, comentario, confirmado);
            return volver(id, "calificado");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }
}
