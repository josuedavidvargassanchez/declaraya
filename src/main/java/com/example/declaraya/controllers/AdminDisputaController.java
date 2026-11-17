package com.example.declaraya.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.declaraya.enums.ResultadoDisputa;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Archivo;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.Mensaje;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.ChatService;
import com.example.declaraya.services.DisputaService;
import com.example.declaraya.services.DocumentoClienteService;
import com.example.declaraya.services.UsuarioService;

/**
 * Revision de disputas: VER (resumen, partes, acuerdo, chat y evidencias) → REVISAR (pedir
 * informacion a las partes) → DECIDIR y CONFIRMAR (modal) → REGISTRAR (historial y notificaciones).
 * Abrir el detalle no cambia el estado de la disputa.
 */
@Controller
@RequestMapping("/admin/disputas")
public class AdminDisputaController {

    @Autowired
    private DisputaService disputaService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ArchivoService archivoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DocumentoClienteService documentoService;

    private Usuario adminActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private String volverConError(Long id, String mensaje) {
        return "redirect:/admin/disputas/" + id + "?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    @GetMapping("/cerradas")
    public String cerradas(Model model) {
        model.addAttribute("disputas", disputaService.listarCerradas());
        return "admin-disputas-cerradas";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Disputa disputa = disputaService.buscarPorId(id);
        model.addAttribute("d", disputa);
        model.addAttribute("historial", disputaService.historial(disputa));
        model.addAttribute("resultados", ResultadoDisputa.values());
        model.addAttribute("mensajeMax", DisputaService.MENSAJE_MAX);
        model.addAttribute("justificacionMin", DisputaService.JUSTIFICACION_MIN);
        model.addAttribute("justificacionMax", DisputaService.JUSTIFICACION_MAX);

        // la politica de datos solo permite al admin ver el chat mientras la disputa esta activa
        List<Mensaje> mensajes = new ArrayList<>();
        Map<Long, List<Archivo>> archivosPorMensaje = new HashMap<>();
        List<Archivo> evidencias = new ArrayList<>();
        List<DocumentoCliente> documentosCliente = new ArrayList<>();
        if (disputa.isActiva()) {
            documentosCliente = documentoService.listarPorSolicitud(disputa.getAcuerdo().getOferta().getSolicitud());
            Chat chat = chatService.buscarPorAcuerdoId(disputa.getAcuerdo().getId());
            mensajes = chatService.listarMensajes(chat);
            for (Mensaje m : mensajes) {
                List<Archivo> archivos = archivoService.listarPorMensaje(m);
                archivosPorMensaje.put(m.getId(), archivos);
                evidencias.addAll(archivos);
            }
        }
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("archivosPorMensaje", archivosPorMensaje);
        model.addAttribute("evidencias", evidencias);
        model.addAttribute("documentosCliente", documentosCliente);
        return "admin-disputa";
    }

    @PostMapping("/{id}/mensaje")
    public String mensaje(@PathVariable Long id,
                          @RequestParam(required = false) String destinatario,
                          @RequestParam(required = false) String tipo,
                          @RequestParam(required = false) String texto,
                          Authentication authentication) {
        try {
            disputaService.enviarMensaje(id, adminActual(authentication), destinatario, tipo, texto);
            return "redirect:/admin/disputas/" + id + "?mensajeEnviado";
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }

    @PostMapping("/{id}/resolver")
    public String resolver(@PathVariable Long id,
                           @RequestParam(required = false) String decision,
                           @RequestParam(required = false) String justificacion,
                           @RequestParam(defaultValue = "false") boolean confirmado,
                           Authentication authentication) {
        if (!confirmado) {
            return volverConError(id, "Debes confirmar que revisaste la conversación y las evidencias");
        }
        ResultadoDisputa resultado = null;
        try {
            resultado = decision == null ? null : ResultadoDisputa.valueOf(decision);
        } catch (IllegalArgumentException e) {
            // se reporta abajo como "elige una decision"
        }
        try {
            disputaService.resolver(id, adminActual(authentication), resultado, justificacion);
            return "redirect:/admin/disputas/" + id + "?resuelta";
        } catch (ReglaNegocioException e) {
            return volverConError(id, e.getMessage());
        }
    }
}
