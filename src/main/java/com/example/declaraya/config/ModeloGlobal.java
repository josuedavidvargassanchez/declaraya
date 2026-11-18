package com.example.declaraya.config;

import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// datos que necesitan todas las pantallas (menu lateral y campana de notificaciones)
@ControllerAdvice
public class ModeloGlobal {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private NotificacionService notificacionService;

    @ModelAttribute
    public void datosGlobales(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        // tambien corre al mostrar /error: si la base no responde, la pagina sigue sin menu ni campana
        try {
            usuarioService.buscarPorEmail(authentication.getName()).ifPresent(u -> {
                model.addAttribute("navRol", u.getRol().name());
                model.addAttribute("notifCount", notificacionService.contarNoLeidas(u));
            });
        } catch (DataAccessException e) {
            // sin navRol ni notifCount; WebExceptionHandler muestra el error si la pagina necesita la base
        }
    }
}
