package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;

/** A6: 5 fallos seguidos bloquean el correo 15 minutos; un acceso correcto reinicia la cuenta. */
class IntentosLoginServiceTest {

    /** Reloj que se puede adelantar a mano. */
    static class RelojAjustable extends Clock {
        Instant ahora = Instant.parse("2026-11-18T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private RelojAjustable reloj;
    private IntentosLoginService intentos;
    private static final String CORREO = "camila.herrera@example.com";

    @BeforeEach
    void preparar() {
        reloj = new RelojAjustable();
        intentos = new IntentosLoginService(reloj);
    }

    @Test
    void despuesDeCincoFallosElCorreoQuedaBloqueado() {
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo(CORREO);
        }
        assertThat(intentos.estaBloqueado(CORREO)).isFalse();

        intentos.registrarFallo(CORREO);
        assertThat(intentos.estaBloqueado(CORREO)).isTrue();
        // sin distinguir mayusculas
        assertThat(intentos.estaBloqueado("Camila.Herrera@Example.com")).isTrue();
    }

    @Test
    void elBloqueoDuraQuinceMinutos() {
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo(CORREO);
        }
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(14));
        assertThat(intentos.estaBloqueado(CORREO)).isTrue();
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(2));
        assertThat(intentos.estaBloqueado(CORREO)).isFalse();
        // tras el bloqueo la cuenta empieza de cero
        intentos.registrarFallo(CORREO);
        assertThat(intentos.estaBloqueado(CORREO)).isFalse();
    }

    @Test
    void unAccesoCorrectoReiniciaLaCuenta() {
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo(CORREO);
        }
        intentos.reiniciar(CORREO);
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo(CORREO);
        }
        assertThat(intentos.estaBloqueado(CORREO)).isFalse();
    }

    @Test
    void mientrasDuraElBloqueoElUsuarioSaleConAccountNonLockedFalse() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(new Usuario("Camila", CORREO, "hash", Rol.CONTADOR_PRACTICANTE)));
        CustomUserDetailsService detalles = new CustomUserDetailsService();
        ReflectionTestUtils.setField(detalles, "usuarioRepository", repositorio);
        ReflectionTestUtils.setField(detalles, "intentosLoginService", intentos);

        assertThat(detalles.loadUserByUsername(CORREO).isAccountNonLocked()).isTrue();
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo(CORREO);
        }
        UserDetails bloqueado = detalles.loadUserByUsername(CORREO);
        assertThat(bloqueado.isAccountNonLocked()).isFalse();
    }
}
