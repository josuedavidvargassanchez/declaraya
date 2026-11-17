package com.example.declaraya.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.example.declaraya.enums.CategoriaDocumento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.TipoDocumentoCliente;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.CatalogoRequisitos;
import com.example.declaraya.services.DocumentoClienteService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.UsuarioService;

/**
 * "Mis documentos" del cliente. Las rutas /cliente/** son solo para ROLE_PERSONA (SecurityConfig);
 * /documentos/{id} es la ruta compartida de ver/descargar para el contador o mentor del acuerdo
 * y el admin con disputa activa. Todas pasan por DocumentoClienteService.puedeVer.
 */
@Controller
public class DocumentoClienteController {

    private final DocumentoClienteService documentoService;
    private final SolicitudDeclaracionService solicitudService;
    private final UsuarioService usuarioService;
    private final CatalogoRequisitos catalogo;

    public DocumentoClienteController(DocumentoClienteService documentoService,
                                      SolicitudDeclaracionService solicitudService,
                                      UsuarioService usuarioService, CatalogoRequisitos catalogo) {
        this.documentoService = documentoService;
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
        this.catalogo = catalogo;
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    // solo se vuelve a "Mis documentos" o al detalle de una solicitud o acuerdo (nunca a una URL externa)
    private static String destino(String volver) {
        return volver != null && volver.matches("^/(solicitudes|acuerdos)/\\d+$") ? volver : "/cliente/documentos";
    }

    private static String redirigir(String volver, String parametro) {
        return "redirect:" + destino(volver) + "?" + parametro;
    }

    private static String redirigirError(String volver, String mensaje) {
        return redirigir(volver, "errorDocumento=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------ vista

    @GetMapping("/cliente/documentos")
    public String misDocumentos(Model model, Authentication authentication) {
        Usuario cliente = usuarioActual(authentication);
        List<DocumentoCliente> todos = documentoService.listarDe(cliente);
        model.addAttribute("personales", todos.stream()
                .filter(d -> d.getSolicitud() == null && d.getCategoria() == CategoriaDocumento.PERSONAL).toList());
        model.addAttribute("tributarios", todos.stream()
                .filter(d -> d.getSolicitud() == null && d.getCategoria() == CategoriaDocumento.TRIBUTARIO).toList());
        model.addAttribute("deSolicitudes", todos.stream().filter(d -> d.getSolicitud() != null).toList());
        model.addAttribute("tipos", TipoDocumentoCliente.values());
        List<SolicitudDeclaracion> vinculables = solicitudesVinculables(cliente);
        model.addAttribute("solicitudesVinculables", vinculables);
        // requisitos del tramite de cada solicitud (la lista del formulario se filtra al elegir la solicitud)
        Map<Long, List<CatalogoRequisitos.Requisito>> requisitosPorSolicitud = new LinkedHashMap<>();
        for (SolicitudDeclaracion s : vinculables) {
            requisitosPorSolicitud.put(s.getId(), catalogo.requisitosPara(s.getTitulo()));
        }
        model.addAttribute("requisitosPorSolicitud", requisitosPorSolicitud);
        model.addAttribute("nombreMax", DocumentoClienteService.NOMBRE_MAX);
        return "mis-documentos";
    }

    // las solicitudes propias que admiten documentos nuevos (no cerradas)
    private List<SolicitudDeclaracion> solicitudesVinculables(Usuario cliente) {
        return solicitudService.listarPorPersona(cliente).stream()
                .filter(s -> s.getEstado() != EstadoSolicitud.CERRADA).toList();
    }

    // ------------------------------------------------------------ acciones del propietario

    @PostMapping("/cliente/documentos")
    public String subir(@RequestParam(required = false) TipoDocumentoCliente tipo,
                        @RequestParam(required = false) String nombre,
                        @RequestParam(required = false) MultipartFile archivo,
                        @RequestParam(required = false) Long solicitudId,
                        @RequestParam(required = false) String requisito,
                        @RequestParam(required = false) Long requerimientoId,
                        @RequestParam(required = false) String volver,
                        Authentication authentication) {
        try {
            documentoService.subir(usuarioActual(authentication), tipo, nombre, archivo, solicitudId, requisito, requerimientoId);
            return redirigir(volver, "documentoSubido");
        } catch (ReglaNegocioException e) {
            return redirigirError(volver, e.getMessage());
        } catch (IOException e) {
            return redirigirError(volver, "No se pudo guardar el archivo. Intenta de nuevo.");
        }
    }

    @PostMapping("/cliente/documentos/{id}/reemplazar")
    public String reemplazar(@PathVariable Long id,
                             @RequestParam(required = false) MultipartFile archivo,
                             @RequestParam(required = false) String volver,
                             Authentication authentication) {
        try {
            documentoService.reemplazar(id, usuarioActual(authentication), archivo);
            return redirigir(volver, "documentoReemplazado");
        } catch (ReglaNegocioException e) {
            return redirigirError(volver, e.getMessage());
        } catch (IOException e) {
            return redirigirError(volver, "No se pudo guardar el archivo. Intenta de nuevo.");
        }
    }

    @PostMapping("/cliente/documentos/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @RequestParam(required = false) String volver,
                           Authentication authentication) {
        try {
            documentoService.eliminar(id, usuarioActual(authentication));
            return redirigir(volver, "documentoEliminado");
        } catch (ReglaNegocioException e) {
            return redirigirError(volver, e.getMessage());
        }
    }

    // solicitudId vacio = desasociar
    @PostMapping("/cliente/documentos/{id}/vincular")
    public String vincular(@PathVariable Long id,
                           @RequestParam(required = false) Long solicitudId,
                           @RequestParam(required = false) String volver,
                           Authentication authentication) {
        try {
            documentoService.vincular(id, usuarioActual(authentication), solicitudId);
            return redirigir(volver, "documentoVinculado");
        } catch (ReglaNegocioException e) {
            return redirigirError(volver, e.getMessage());
        }
    }

    // ------------------------------------------------------------ ver y descargar

    @GetMapping({"/cliente/documentos/{id}", "/documentos/{id}"})
    public ResponseEntity<Resource> ver(@PathVariable Long id, Authentication authentication) throws IOException {
        return servir(id, authentication, false);
    }

    @GetMapping({"/cliente/documentos/{id}/descargar", "/documentos/{id}/descargar"})
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication authentication) throws IOException {
        return servir(id, authentication, true);
    }

    // el nombre en disco (UUID) nunca sale al navegador: se envia el nombre original del cliente
    private ResponseEntity<Resource> servir(Long id, Authentication authentication, boolean adjunto) throws IOException {
        DocumentoCliente documento = documentoService.buscarPorId(id);
        documentoService.exigirAcceso(documento, usuarioActual(authentication));
        Resource recurso = documentoService.cargar(documento);
        ContentDisposition disposicion = (adjunto ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(documento.getNombreOriginal(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .cacheControl(CacheControl.noStore())
                .contentType(ArchivoService.tipoDeContenido(documento.getArchivo()))
                .body(recurso);
    }
}
