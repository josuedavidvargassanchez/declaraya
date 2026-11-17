package com.example.declaraya.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.ActividadAcuerdoRepository;
import com.example.declaraya.repositories.AsignacionMentoriaRepository;
import com.example.declaraya.repositories.EntregaTrabajoRepository;
import com.example.declaraya.repositories.SolicitudMentoriaRepository;
import com.example.declaraya.repositories.SupervisionRepository;
import com.example.declaraya.repositories.UsuarioRepository;
import com.example.declaraya.services.ActividadAcuerdoService;
import com.example.declaraya.services.MentoriaService;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.UsuarioService;

/**
 * GET /contador/practicantes/{id}: solo el mentor vigente del practicante. Otro contador publico
 * (aunque SecurityConfig le deje entrar a /contador/**) recibe 403. MockMvc standalone, sin base de datos.
 */
class MentoriaControllerTest {

    private MockMvc mvc;

    private static Usuario usuario(long id, String email, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, email, "x", rol);
        u.setId(id);
        if (rol == Rol.CONTADOR_PUBLICO) {
            u.setEstadoVerificacion(EstadoVerificacion.APROBADO);
            u.setVerificado(true);
        }
        return u;
    }

    @BeforeEach
    void preparar() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", mock(PasswordEncoder.class));

        MentoriaService mentoriaService = new MentoriaService(mock(SolicitudMentoriaRepository.class),
                mock(AsignacionMentoriaRepository.class), usuarioRepository, usuarioService,
                mock(SupervisionRepository.class), mock(EntregaTrabajoRepository.class), mock(AcuerdoRepository.class),
                mock(ActividadAcuerdoService.class), mock(ActividadAcuerdoRepository.class), mock(NotificacionService.class));
        mvc = MockMvcBuilders.standaloneSetup(new MentoriaController(mentoriaService, usuarioService)).build();

        Usuario mentor = usuario(5, "mentor@example.com", Rol.CONTADOR_PUBLICO);
        Usuario otroContador = usuario(4, "otro@example.com", Rol.CONTADOR_PUBLICO);
        Usuario practicante = usuario(3, "practicante@example.com", Rol.CONTADOR_PRACTICANTE);
        practicante.setMentor(mentor);

        when(usuarioRepository.findByEmail("mentor@example.com")).thenReturn(Optional.of(mentor));
        when(usuarioRepository.findByEmail("otro@example.com")).thenReturn(Optional.of(otroContador));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(practicante));
    }

    private static UsernamePasswordAuthenticationToken sesion(String email) {
        return new UsernamePasswordAuthenticationToken(email, "x", List.of());
    }

    @Test
    void unContadorQueNoEsSuMentorRecibe403() throws Exception {
        mvc.perform(get("/contador/practicantes/3").principal(sesion("otro@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unPracticanteQueNoExisteTambienDa403() throws Exception {
        mvc.perform(get("/contador/practicantes/99").principal(sesion("otro@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elMentorVigenteVeLasSupervisiones() throws Exception {
        mvc.perform(get("/contador/practicantes/3").principal(sesion("mentor@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("contador-practicante"));
    }
}
