package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;

/** A7 (ultimo administrador) y A4 (validacion de los formularios de usuarios del admin). */
class UsuarioServiceAdminTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        servicio = new UsuarioService();
        ReflectionTestUtils.setField(servicio, "usuarioRepository", repositorio);
        ReflectionTestUtils.setField(servicio, "passwordEncoder", mock(PasswordEncoder.class));

        admin = new Usuario("Administrador", "admin@declaraya.com", "hash", Rol.ADMIN);
        admin.setId(1L);
        when(repositorio.findById(1L)).thenReturn(Optional.of(admin));
        when(repositorio.findByEmail("admin@declaraya.com")).thenReturn(Optional.of(admin));
        when(repositorio.countByRol(Rol.ADMIN)).thenReturn(1L);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void noSePuedeQuitarElRolAlUltimoAdmin() {
        Usuario otroEditor = new Usuario("Otro", "otro@example.com", "x", Rol.ADMIN);
        otroEditor.setId(2L);

        assertThatThrownBy(() -> servicio.actualizarPorAdmin(1L, "Administrador", "admin@declaraya.com", null,
                Rol.PERSONA, null, null, otroEditor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("último administrador");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        verify(repositorio, never()).save(any());
    }

    @Test
    void unAdminNoPuedeCambiarSuPropioRol() {
        when(repositorio.countByRol(Rol.ADMIN)).thenReturn(3L);

        assertThatThrownBy(() -> servicio.actualizarPorAdmin(1L, "Administrador", "admin@declaraya.com", null,
                Rol.CONTADOR_PUBLICO, null, null, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propio rol");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void noSePuedeEliminarAlUltimoAdminNiASiMismo() {
        Usuario otroEditor = new Usuario("Otro", "otro@example.com", "x", Rol.ADMIN);
        otroEditor.setId(2L);

        assertThatThrownBy(() -> servicio.exigirEliminable(admin, otroEditor)).isInstanceOf(IllegalArgumentException.class);
        when(repositorio.countByRol(Rol.ADMIN)).thenReturn(2L);
        assertThatThrownBy(() -> servicio.exigirEliminable(admin, admin)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elAdminPuedeEditarSuNombreSinCambiarSuRol() {
        Usuario guardado = servicio.actualizarPorAdmin(1L, "Admin Principal", "admin@declaraya.com", null,
                Rol.ADMIN, null, null, admin);
        assertThat(guardado.getNombre()).isEqualTo("Admin Principal");
    }

    @Test
    void validaCorreoNombreYTarifaDelFormularioDeUsuarios() {
        assertThatThrownBy(() -> servicio.crearPorAdmin("Ana", "no-es-correo", "secreto1", Rol.PERSONA, null, false, null, admin))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.crearPorAdmin("x".repeat(101), "ana@example.com", "secreto1", Rol.PERSONA, null, false, null, admin))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.crearPorAdmin("Ana", "ana@example.com", "secreto1", Rol.CONTADOR_PUBLICO, null, true,
                new BigDecimal("-1"), admin)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.crearPorAdmin("Ana", "ana@example.com", "secreto1", Rol.CONTADOR_PUBLICO, null, true,
                new BigDecimal("100000001"), admin)).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio, never()).save(any());
    }
}
