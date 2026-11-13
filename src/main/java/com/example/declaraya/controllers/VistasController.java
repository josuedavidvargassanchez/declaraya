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
import org.springframework.web.bind.annotation.PostMapping;

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
    public String notificaciones(Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        model.addAttribute("notificaciones", notificacionService.listar(usuario));
        return "notificaciones";
    }

    @PostMapping("/notificaciones/leer")
    public String marcarLeidas(Authentication authentication) {
        notificacionService.marcarTodasLeidas(usuarioActual(authentication));
        return "redirect:/notificaciones";
    }
}
