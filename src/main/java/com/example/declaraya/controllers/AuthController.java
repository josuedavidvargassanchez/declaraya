package com.example.declaraya.controllers;

import com.example.declaraya.dto.RegistroUsuarioDTO;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    // el rol ADMIN nunca se ofrece ni se acepta en el registro publico
    private Rol[] rolesPublicos() {
        return new Rol[]{Rol.PERSONA, Rol.CONTADOR_PUBLICO, Rol.CONTADOR_PRACTICANTE};
    }

    @GetMapping("/login")
    public String formularioLogin() {
        return "login";
    }

    @GetMapping("/politica-datos")
    public String politicaDatos() {
        return "politica-datos";
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model model) {
        model.addAttribute("registro", new RegistroUsuarioDTO());
        model.addAttribute("roles", rolesPublicos());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("registro") RegistroUsuarioDTO registro,
                                   BindingResult resultado,
                                   Model model) {

        Rol rol = registro.getRol();

        if (rol == Rol.ADMIN) {
            resultado.rejectValue("rol", "rol.invalido", "Tipo de cuenta no permitido");
        } else if (rol != null && rol != Rol.PERSONA
                && (registro.getTarjetaProfesional() == null || registro.getTarjetaProfesional().isBlank())) {
            resultado.rejectValue("tarjetaProfesional", "tarjeta.obligatoria",
                    "La tarjeta profesional es obligatoria para contadores");
        }

        String email = registro.getEmail() == null ? null : registro.getEmail().trim().toLowerCase();
        if (email != null && !email.isEmpty() && usuarioService.buscarPorEmail(email).isPresent()) {
            resultado.rejectValue("email", "email.duplicado", "Ese correo ya esta registrado");
        }

        if (resultado.hasErrors()) {
            model.addAttribute("roles", rolesPublicos());
            return "registro";
        }

        String tarjeta = rol == Rol.PERSONA ? null : registro.getTarjetaProfesional().trim();

        usuarioService.registrar(
                registro.getNombre().trim(),
                email,
                registro.getPassword(),
                rol,
                tarjeta
        );

        return "redirect:/login?registrado";
    }
}
