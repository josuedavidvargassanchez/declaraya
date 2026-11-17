package com.example.declaraya.controllers;

import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.FlujoAcuerdoService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SupervisionController {

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private FlujoAcuerdoService flujoService;

    // al aprobar, el acuerdo pasa de APROBACION_MENTOR a DOCUMENTACION
    @PostMapping("/contador/supervisiones/{id}/aprobar")
    public String aprobar(@PathVariable Long id,
                          @RequestParam(required = false) String volver,
                          Authentication authentication) {
        Usuario mentor = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        Supervision supervision = supervisionService.aprobar(id, mentor);
        flujoService.mentorAprobo(supervision.getAcuerdo(), mentor);
        if ("detalle".equals(volver)) {
            return "redirect:/acuerdos/" + supervision.getAcuerdo().getId() + "?mentorAprobo";
        }
        return "redirect:/contador/inicio";
    }
}
