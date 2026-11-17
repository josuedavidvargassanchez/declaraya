package com.example.declaraya.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoDocumento;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;
import com.example.declaraya.services.FotoPerfilService;
import com.example.declaraya.services.UsuarioService;
import com.example.declaraya.services.VerificacionService;

/**
 * POST /perfil/datos: solo cambia los campos permitidos. Aunque se envien rol, correo,
 * tarjeta profesional o datos de verificacion, quedan intactos.
 * MockMvc standalone, sin base de datos (el repositorio es un mock).
 */
class PerfilControllerTest {

    private MockMvc mvc;
    private UsuarioRepository usuarioRepository;
    private Usuario contador;
    private final UsernamePasswordAuthenticationToken sesion =
            new UsernamePasswordAuthenticationToken("contador@example.com", "x", List.of());

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", mock(PasswordEncoder.class));

        PerfilController controller = new PerfilController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "fotoPerfilService", mock(FotoPerfilService.class));
        ReflectionTestUtils.setField(controller, "verificacionService", mock(VerificacionService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        contador = new Usuario("Contador Original", "contador@example.com", "hash", Rol.CONTADOR_PUBLICO);
        contador.setId(7L);
        contador.setTarjetaProfesional("TP-ORIGINAL");
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        contador.setVerificado(false);
        contador.setTarifaBase(new BigDecimal("400000"));

        when(usuarioRepository.findByEmail("contador@example.com")).thenReturn(Optional.of(contador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void noCambiaRolTarjetaNiVerificacionAunqueSeEnvien() throws Exception {
        mvc.perform(post("/perfil/datos").principal(sesion)
                        // campos permitidos
                        .param("nombre", "Contador Editado")
                        .param("telefono", "300 123 4567")
                        .param("ciudad", "Medellín")
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "1020304050")
                        .param("descripcionProfesional", "Experto en renta")
                        .param("tarifaBase", "450000")
                        // campos que NO se deben poder cambiar por esta via
                        .param("rol", "ADMIN")
                        .param("email", "atacante@example.com")
                        .param("tarjetaProfesional", "TP-FALSA")
                        .param("verificado", "true")
                        .param("estadoVerificacion", "APROBADO")
                        .param("password", "nueva-clave"))
                .andExpect(redirectedUrl("/perfil?datosActualizados"));

        verify(usuarioRepository).save(contador);
        assertThat(contador.getNombre()).isEqualTo("Contador Editado");
        assertThat(contador.getTelefono()).isEqualTo("300 123 4567");
        assertThat(contador.getCiudad()).isEqualTo("Medellín");
        assertThat(contador.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
        assertThat(contador.getNumeroDocumento()).isEqualTo("1020304050");
        assertThat(contador.getDescripcionProfesional()).isEqualTo("Experto en renta");
        assertThat(contador.getTarifaBase()).isEqualByComparingTo("450000");

        assertThat(contador.getRol()).isEqualTo(Rol.CONTADOR_PUBLICO);
        assertThat(contador.getEmail()).isEqualTo("contador@example.com");
        assertThat(contador.getTarjetaProfesional()).isEqualTo("TP-ORIGINAL");
        assertThat(contador.isVerificado()).isFalse();
        assertThat(contador.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(contador.getPassword()).isEqualTo("hash");
    }

    @Test
    void conDatosInvalidosNoGuardaYVuelveAlPerfil() throws Exception {
        mvc.perform(post("/perfil/datos").principal(sesion)
                        .param("nombre", "Contador")
                        .param("telefono", "abc")
                        .param("tarifaBase", "-5"))
                .andExpect(view().name("perfil"));

        verify(usuarioRepository, never()).save(any());
        assertThat(contador.getTarifaBase()).isEqualByComparingTo("400000");
    }

    @Test
    void unaPersonaNoPuedeGuardarDatosProfesionales() throws Exception {
        Usuario persona = new Usuario("Ana", "ana@example.com", "hash", Rol.PERSONA);
        persona.setId(8L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(persona));

        mvc.perform(post("/perfil/datos").principal(new UsernamePasswordAuthenticationToken("ana@example.com", "x", List.of()))
                        .param("nombre", "Ana María")
                        .param("descripcionProfesional", "No aplica")
                        .param("tarifaBase", "1"))
                .andExpect(redirectedUrl("/perfil?datosActualizados"));

        assertThat(persona.getNombre()).isEqualTo("Ana María");
        assertThat(persona.getDescripcionProfesional()).isNull();
        assertThat(persona.getTarifaBase()).isEqualByComparingTo(Usuario.TARIFA_BASE_POR_DEFECTO);
    }
}
