package com.example.declaraya.controllers;

import com.example.declaraya.dto.RegistroUsuarioDTO;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.UsuarioService;
import com.example.declaraya.services.VerificacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VerificacionService verificacionService;

    // el rol ADMIN nunca se ofrece ni se acepta en el registro publico
    private Rol[] rolesPublicos() {
        return new Rol[]{Rol.PERSONA, Rol.CONTADOR_PUBLICO, Rol.CONTADOR_PRACTICANTE};
    }

    @GetMapping("/login")
    public String formularioLogin() {
        return "login";
    }

    @GetMapping("/politica-datos")
    public String politicaDatos(Authentication authentication, Model model) {
        model.addAttribute("logueado", authentication != null && authentication.isAuthenticated());
        return "politica-datos";
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model model) {
        model.addAttribute("registro", new RegistroUsuarioDTO());
        model.addAttribute("roles", rolesPublicos());
        return "registro";
    }

    // multipart: los contadores adjuntan el soporte de la tarjeta profesional (obligatorio)
    // y su documento de identidad (opcional)
    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("registro") RegistroUsuarioDTO registro,
                                   BindingResult resultado,
                                   @RequestParam(required = false) MultipartFile soporteTarjeta,
                                   @RequestParam(required = false) MultipartFile soporteDocumento,
                                   Model model) {

        Rol rol = registro.getRol();
        boolean esContador = rol == Rol.CONTADOR_PUBLICO || rol == Rol.CONTADOR_PRACTICANTE;
        String errorTarjeta = esContador ? verificacionService.validarSoporte(soporteTarjeta, true) : null;
        String errorDocumento = esContador ? verificacionService.validarSoporte(soporteDocumento, false) : null;
        if (errorTarjeta != null) {
            model.addAttribute("errorSoporteTarjeta", errorTarjeta);
        }
        if (errorDocumento != null) {
            model.addAttribute("errorSoporteDocumento", errorDocumento);
        }

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

        if (resultado.hasErrors() || errorTarjeta != null || errorDocumento != null) {
            model.addAttribute("roles", rolesPublicos());
            return "registro";
        }

        String tarjeta = rol == Rol.PERSONA ? null : registro.getTarjetaProfesional().trim();

        Usuario usuario = usuarioService.registrar(
                registro.getNombre().trim(),
                email,
                registro.getPassword(),
                rol,
                tarjeta
        );

        if (esContador) {
            verificacionService.registrarSoportesIniciales(usuario, soporteTarjeta, soporteDocumento);
            return "redirect:/login?registrado&verificacion";
        }
        return "redirect:/login?registrado";
    }
}
