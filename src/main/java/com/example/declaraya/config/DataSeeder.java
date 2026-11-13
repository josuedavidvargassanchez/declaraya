package com.example.declaraya.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.declaraya.enums.Rol;
import com.example.declaraya.services.UsuarioService;

@Component 
public class DataSeeder implements CommandLineRunner {
    private static final String ADMIN_EMAIL ="admin@declaraya.com";
    private static final String ADMIN_PASSWORD ="admin123";

    private final UsuarioService usuarioService;

    public DataSeeder(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override 
    public void run(String... args){
        if (usuarioService.buscarPorEmail(ADMIN_EMAIL).isEmpty()){
            usuarioService.registrar("Administrador", ADMIN_EMAIL, ADMIN_PASSWORD, Rol.ADMIN, null);
            System.out.println("Usuario administrador de prueba creado: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);
        }
    }

}
