package com.example.declaraya.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.CalificacionService;
import com.example.declaraya.services.UsuarioService;

/**
 * Perfil publico de un contador (cualquier usuario autenticado). Solo muestra datos profesionales:
 * nunca correo, telefono, documento de identidad ni archivos de verificacion.
 */
@Controller
public class PerfilPublicoController {

    private static final int RESENAS = 5;

    private final UsuarioService usuarioService;
    private final CalificacionService calificacionService;

    public PerfilPublicoController(UsuarioService usuarioService, CalificacionService calificacionService) {
        this.usuarioService = usuarioService;
        this.calificacionService = calificacionService;
    }

    @GetMapping("/contadores/{id}")
    public String perfil(@PathVariable Long id, Model model) {
        Usuario contador = usuarioService.buscarPorId(id);
        if (!contador.isContador()) {
            // las personas y los administradores no tienen perfil publico
            throw new RecursoNoEncontradoException("Contador no encontrado");
        }
        model.addAttribute("contador", contador);
        model.addAttribute("verificado", contador.getEstadoVerificacion() == EstadoVerificacion.APROBADO);
        model.addAttribute("promedio", calificacionService.promedio(contador));
        model.addAttribute("cantidadCalificaciones", calificacionService.cantidadVisibles(contador));
        model.addAttribute("finalizados", calificacionService.acuerdosFinalizados(contador));
        model.addAttribute("resenas", calificacionService.resenasVisibles(contador, RESENAS));
        return "perfil-contador";
    }
}
