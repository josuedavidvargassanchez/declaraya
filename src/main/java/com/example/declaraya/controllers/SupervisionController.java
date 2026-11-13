package com.example.declaraya.controllers;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SupervisionController {

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/contador/supervisiones/{id}/aprobar")
    public String aprobar(@PathVariable Long id, Authentication authentication) {
        Usuario mentor = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        supervisionService.aprobar(id, mentor);
        return "redirect:/contador/inicio";
    }
}