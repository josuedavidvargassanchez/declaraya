package com.example.declaraya.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Archivo;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;


@Controller 
public class ArchivoController {
    @Autowired 
    private ArchivoService archivoService;

    @Autowired 
    private AcuerdoService acuerdoService;

    @Autowired 
    private UsuarioService usuarioService;

    @GetMapping("/archivos/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication authentication) throws IOException {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        Archivo archivo = archivoService.buscarPorId(id);
        Acuerdo acuerdo = archivo.getMensaje().getChat().getAcuerdo();

        if (!acuerdoService.puedeVer(acuerdo, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este archivo");
        }

        Resource recurso = archivoService.cargar(archivo);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.getNombreArchivo(), StandardCharsets.UTF_8).build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(recurso);
    }
    
}
