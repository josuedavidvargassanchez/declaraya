package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Notificacion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.NotificacionRepository;

class NotificacionServiceTest {

    private NotificacionRepository repositorio;
    private NotificacionService servicio;
    private Usuario duena;
    private Usuario otro;

    @BeforeEach
    void preparar() {
        repositorio = mock(NotificacionRepository.class);
        servicio = new NotificacionService();
        ReflectionTestUtils.setField(servicio, "notificacionRepository", repositorio);
        duena = new Usuario("Ana", "ana@example.com", "x", Rol.PERSONA);
        duena.setId(1L);
        otro = new Usuario("Otro", "otro@example.com", "x", Rol.PERSONA);
        otro.setId(2L);
    }

    private Notificacion notificacion(String enlace) {
        Notificacion n = new Notificacion(duena, "Mensaje", enlace);
        ReflectionTestUtils.setField(n, "id", 5L);
        when(repositorio.findById(5L)).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    void laNotificacionDeOtroUsuarioNoSePuedeAbrir() {
        Notificacion n = notificacion("/acuerdos/3");

        assertThatThrownBy(() -> servicio.abrir(5L, otro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(n.isLeida()).isFalse();
        verify(repositorio, never()).save(any());
    }

    @Test
    void abrirLaMarcaLeidaYDevuelveLaRutaInterna() {
        Notificacion n = notificacion("/acuerdos/3");

        assertThat(servicio.abrir(5L, duena)).isEqualTo("/acuerdos/3");
        assertThat(n.isLeida()).isTrue();
    }

    @Test
    void unEnlaceExternoNoSeSigue() {
        notificacion("https://sitio-externo.example.com");
        assertThat(servicio.abrir(5L, duena)).isEqualTo("/notificaciones");

        notificacion("//sitio-externo.example.com");
        assertThat(servicio.abrir(5L, duena)).isEqualTo("/notificaciones");
    }
}
