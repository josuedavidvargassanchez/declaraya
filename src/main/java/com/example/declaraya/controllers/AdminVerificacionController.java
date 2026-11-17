package com.example.declaraya.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.UsuarioService;
import com.example.declaraya.services.VerificacionService;

/**
 * Revision de la verificacion de un contador: VER (datos y documentos) → DECIDIR (modal)
 * → CONFIRMAR (casilla obligatoria) → REGISTRAR (estado, revisor, fecha y notificacion).
 * Todo bajo /admin/**, que SecurityConfig ya restringe a ROLE_ADMIN.
 */
@Controller
@RequestMapping("/admin/verificaciones")
public class AdminVerificacionController {

    @Autowired
    private VerificacionService verificacionService;

    @Autowired
    private UsuarioService usuarioService;

    private Usuario adminActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private String volverAlDetalle(Long id, String parametro) {
        return "redirect:/admin/verificaciones/" + id + "?" + parametro;
    }

    private String volverConError(Long id, String mensaje) {
        return volverAlDetalle(id, "error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Usuario contador = verificacionService.buscarContador(id);
        model.addAttribute("c", contador);
        model.addAttribute("pendiente", contador.getEstadoVerificacion() == EstadoVerificacion.PENDIENTE);
        model.addAttribute("motivoMin", VerificacionService.MOTIVO_MIN);
        model.addAttribute("textoMax", VerificacionService.TEXTO_MAX);
        return "admin-verificacion";
    }

    // se muestra en linea (PDF o imagen) en una pestaña nueva; no se guarda en cache del navegador
    @GetMapping("/{id}/documentos/{tipo}")
    public ResponseEntity<Resource> documento(@PathVariable Long id, @PathVariable String tipo) throws IOException {
        Usuario contador = verificacionService.buscarContador(id);
        return servirDocumento(verificacionService, contador, tipo);
    }

    static ResponseEntity<Resource> servirDocumento(VerificacionService servicio, Usuario contador, String tipo) throws IOException {
        String nombre = servicio.nombreDocumento(contador, tipo);
        Resource recurso = servicio.cargarDocumento(contador, tipo);
        String descarga = (VerificacionService.DOC_TARJETA.equals(tipo) ? "soporte-tarjeta-profesional" : "documento-identidad")
                + nombre.substring(nombre.lastIndexOf('.'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(descarga, StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())
                .contentType(ArchivoService.tipoDeContenido(nombre))
                .body(recurso);
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id,
                          @RequestParam(required = false) String observacion,
                          @RequestParam(defaultValue = "false") boolean confirmado,
                          Authentication authentication) {
        try {
            verificacionService.aprobar(id, adminActual(authentication), observacion, confirmado);
            return volverAlDetalle(id, "aprobado");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id,
                           @RequestParam(required = false) String motivo,
                           @RequestParam(required = false) String observacion,
                           @RequestParam(defaultValue = "false") boolean confirmado,
                           Authentication authentication) {
        try {
            verificacionService.rechazar(id, adminActual(authentication), motivo, observacion, confirmado);
            return volverAlDetalle(id, "rechazado");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/{id}/correccion")
    public String correccion(@PathVariable Long id,
                             @RequestParam(required = false) String observacion,
                             @RequestParam(defaultValue = "false") boolean confirmado,
                             Authentication authentication) {
        try {
            verificacionService.solicitarCorreccion(id, adminActual(authentication), observacion, confirmado);
            return volverAlDetalle(id, "correccion");
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }
}
