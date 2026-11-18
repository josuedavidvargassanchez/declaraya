package com.example.declaraya.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.declaraya.enums.Rol;
import com.example.declaraya.services.UsuarioService;

@Component
@Order(10) // antes de DemoDataSeeder
public class DataSeeder implements CommandLineRunner {
    private static final String ADMIN_EMAIL ="admin@declaraya.com";
    private static final String CLAVE_DE_DESARROLLO = "admin123";

    private final UsuarioService usuarioService;

    // declaraya.admin.password=${ADMIN_PASSWORD:admin123}: en produccion se define ADMIN_PASSWORD
    private final String adminPassword;

    public DataSeeder(UsuarioService usuarioService, @Value("${declaraya.admin.password}") String adminPassword) {
        this.usuarioService = usuarioService;
        this.adminPassword = adminPassword;
    }

    // solo crea el administrador inicial si no existe; una cuenta ya creada conserva su clave
    @Override
    public void run(String... args){
        if (usuarioService.buscarPorEmail(ADMIN_EMAIL).isEmpty()){
            usuarioService.registrar("Administrador", ADMIN_EMAIL, adminPassword, Rol.ADMIN, null);
            // la clave solo se imprime si es la de desarrollo
            System.out.println("Usuario administrador inicial creado: " + ADMIN_EMAIL
                    + (CLAVE_DE_DESARROLLO.equals(adminPassword) ? " / " + CLAVE_DE_DESARROLLO : " (clave tomada de ADMIN_PASSWORD)"));
        }
    }

}
