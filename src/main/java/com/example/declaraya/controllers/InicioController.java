package com.example.declaraya.controllers;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @Autowired
    private UsuarioService usuarioService;

    // pagina publica de presentacion; si ya hay sesion, entra directo al panel
    @GetMapping("/")
    public String raiz(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/inicio";
        }
        return "landing";
    }

    @GetMapping("/ayuda")
    public String ayuda(Authentication authentication, org.springframework.ui.Model model) {
        model.addAttribute("logueado", authentication != null && authentication.isAuthenticated());
        return "ayuda";
    }

    // punto de entrada unico despues del login; redirige segun el rol del usuario
    @GetMapping("/inicio")
    public String inicio(Authentication authentication) {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        return switch (usuario.getRol()) {
            case PERSONA -> "redirect:/cliente/inicio";
            case CONTADOR_PUBLICO -> "redirect:/contador/inicio";
            case CONTADOR_PRACTICANTE -> "redirect:/practicante/inicio";
            case ADMIN -> "redirect:/admin/inicio";
        };
    }
}
