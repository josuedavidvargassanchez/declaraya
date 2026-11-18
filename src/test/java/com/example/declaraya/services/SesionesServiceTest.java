package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;

/** B1: expirar sesiones al cambiar rol, eliminar o cambiar la contraseña. */
class SesionesServiceTest {

    private SessionRegistryImpl registro;
    private SesionesService sesiones;
    private final User camila = new User("camila.herrera@example.com", "x", List.of());
    private final User otro = new User("otro@example.com", "x", List.of());

    @BeforeEach
    void preparar() {
        registro = new SessionRegistryImpl();
        sesiones = new SesionesService(registro);
        registro.registerNewSession("s1", camila);
        registro.registerNewSession("s2", camila);
        registro.registerNewSession("s3", otro);
    }

    @Test
    void alCambiarLaContrasenaSeConservaLaSesionActual() {
        sesiones.expirarOtras("camila.herrera@example.com", "s1");

        assertThat(registro.getSessionInformation("s1").isExpired()).isFalse();
        assertThat(registro.getSessionInformation("s2").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("s3").isExpired()).isFalse();
    }

    @Test
    void alCambiarElRolSeExpiranTodas() {
        sesiones.expirarTodas("Camila.Herrera@example.com");

        assertThat(registro.getSessionInformation("s1").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("s2").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("s3").isExpired()).isFalse();
    }
}
