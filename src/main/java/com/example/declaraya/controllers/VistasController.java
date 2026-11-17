package com.example.declaraya.controllers;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VistasController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private NotificacionService notificacionService;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    @GetMapping("/acuerdos")
    public String misAcuerdos(Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        model.addAttribute("acuerdos", acuerdoService.listarDe(usuario));
        model.addAttribute("esPersona", usuario.getRol() == com.example.declaraya.enums.Rol.PERSONA);
        return "mis-acuerdos";
    }

    @GetMapping("/notificaciones")
    public String notificaciones(@RequestParam(required = false) String filtro, Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        String filtroValido = "pendientes".equals(filtro) || "leidas".equals(filtro) ? filtro : "todas";
        model.addAttribute("notificaciones", notificacionService.listar(usuario, filtroValido));
        model.addAttribute("filtro", filtroValido);
        return "notificaciones";
    }

    // marca la notificacion como leida y lleva a su enlace (solo rutas internas)
    @GetMapping("/notificaciones/{id}/abrir")
    public String abrirNotificacion(@PathVariable Long id, Authentication authentication) {
        return "redirect:" + notificacionService.abrir(id, usuarioActual(authentication));
    }

    @PostMapping("/notificaciones/leer")
    public String marcarLeidas(Authentication authentication) {
        notificacionService.marcarTodasLeidas(usuarioActual(authentication));
        return "redirect:/notificaciones";
    }
}
