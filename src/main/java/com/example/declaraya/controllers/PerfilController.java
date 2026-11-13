package com.example.declaraya.controllers;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.FotoPerfilService;
import com.example.declaraya.services.UsuarioService;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;



@Controller 
public class PerfilController {
    
    @Autowired 
    private UsuarioService usuarioService;

    @Autowired 
    private FotoPerfilService fotoPerfilService;

    private Usuario usuarioActual(Authentication authentication){
        return usuarioService.buscarPorEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model, Authentication authentication) {
        model.addAttribute("usuario", usuarioActual(authentication));
        return "perfil";
    }

    @PostMapping("/perfil/foto")
    public String subirFoto(@RequestParam MultipartFile foto, Authentication authentication) {
        if (foto == null || foto.isEmpty()) {
            return "redirect:/perfil?errorFoto";
        }
        try {
            fotoPerfilService.guardar(usuarioActual(authentication), foto);
        } catch (FotoPerfilService.FotoNoPermitidaException | IOException e) {
            return "redirect:/perfil?errorFoto";
        }

        return "redirect:/perfil?fotoActualizada";
    }

    @PostMapping("/perfil/foto/eliminar")
    public String eliminarFoto(Authentication authentication) {
        fotoPerfilService.eliminar(usuarioActual(authentication));
        
        return "redirect:/perfil?fotoEliminada";
    }

    //cualquier usuario puede ver la foto del otro
    @GetMapping("/usuarios/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) throws IOException {
        Usuario usuario = usuarioService.buscarPorId(id);
        Resource recurso = fotoPerfilService.cargar(usuario);
        return ResponseEntity.ok().contentType(fotoPerfilService.tipoDe(usuario)).cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate()).body(recurso);
    }
    
    
    

}
