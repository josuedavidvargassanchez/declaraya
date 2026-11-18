package com.example.declaraya.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.services.CalificacionService;

// moderacion de comentarios (bajo /admin/**, solo ROLE_ADMIN): se ocultan, nunca se borran
@Controller
@RequestMapping("/admin/calificaciones")
public class AdminCalificacionController {

    private final CalificacionService calificacionService;

    public AdminCalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("calificaciones", calificacionService.listarConComentario());
        model.addAttribute("motivoMax", CalificacionService.COMENTARIO_MAX);
        return "admin-calificaciones";
    }

    @PostMapping("/{id}/ocultar")
    public String ocultar(@PathVariable Long id,
                          @RequestParam(required = false) String motivo,
                          @RequestParam(defaultValue = "false") boolean confirmado,
                          RedirectAttributes redirect) {
        try {
            calificacionService.ocultar(id, motivo, confirmado);
            return "redirect:/admin/calificaciones?ocultada";
        } catch (ReglaNegocioException e) {
            // el mensaje viaja como atributo flash (en la sesion), no en la URL
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/calificaciones";
        }
    }
}
