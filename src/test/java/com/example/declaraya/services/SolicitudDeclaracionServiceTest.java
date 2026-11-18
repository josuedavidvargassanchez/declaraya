package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.OfertaRepository;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;

/** A1: quien puede ver /solicitudes/{id}. */
class SolicitudDeclaracionServiceTest {

    private OfertaRepository ofertaRepository;
    private AcuerdoRepository acuerdoRepository;
    private SolicitudDeclaracionService servicio;

    private Usuario dueno;
    private Usuario otraPersona;
    private Usuario contador;
    private SolicitudDeclaracion solicitud;

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@example.com", "x", rol);
        u.setId(id);
        if (rol == Rol.CONTADOR_PUBLICO || rol == Rol.CONTADOR_PRACTICANTE) {
            u.setEstadoVerificacion(EstadoVerificacion.APROBADO);
            u.setVerificado(true);
        }
        return u;
    }

    @BeforeEach
    void preparar() {
        ofertaRepository = mock(OfertaRepository.class);
        acuerdoRepository = mock(AcuerdoRepository.class);
        servicio = new SolicitudDeclaracionService();
        ReflectionTestUtils.setField(servicio, "solicitudRepository", mock(SolicitudDeclaracionRepository.class));
        ReflectionTestUtils.setField(servicio, "ofertaRepository", ofertaRepository);
        ReflectionTestUtils.setField(servicio, "acuerdoRepository", acuerdoRepository);
        ReflectionTestUtils.setField(servicio, "supervisionService", mock(SupervisionService.class));

        dueno = usuario(1, Rol.PERSONA);
        otraPersona = usuario(2, Rol.PERSONA);
        contador = usuario(3, Rol.CONTADOR_PUBLICO);
        solicitud = new SolicitudDeclaracion(dueno, "Declaración de renta", "Ayuda");
        solicitud.setId(10L);
        when(acuerdoRepository.findByOfertaSolicitud(solicitud)).thenReturn(List.of());
    }

    @Test
    void elDuenoVeSuSolicitud() {
        assertThat(servicio.puedeVer(solicitud, dueno)).isTrue();
    }

    @Test
    void otraPersonaRecibe403() {
        assertThat(servicio.puedeVer(solicitud, otraPersona)).isFalse();
        assertThatThrownBy(() -> servicio.exigirVisible(solicitud, otraPersona))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unContadorVerificadoVeUnaSolicitudAbierta() {
        solicitud.setEstado(EstadoSolicitud.ABIERTA);
        assertThat(servicio.puedeVer(solicitud, contador)).isTrue();
    }

    @Test
    void unContadorSinVerificarNoVeUnaSolicitudAbiertaAjena() {
        contador.setVerificado(false);
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        solicitud.setEstado(EstadoSolicitud.ABIERTA);
        assertThat(servicio.puedeVer(solicitud, contador)).isFalse();
    }

    @Test
    void unContadorSinRelacionNoVeUnaSolicitudEnProcesoAjena() {
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        when(ofertaRepository.existsBySolicitudAndContador(solicitud, contador)).thenReturn(false);

        assertThat(servicio.puedeVer(solicitud, contador)).isFalse();
        assertThatThrownBy(() -> servicio.exigirVisible(solicitud, contador)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unContadorCuyaOfertaFueRechazadaSiLaVe() {
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        when(ofertaRepository.existsBySolicitudAndContador(solicitud, contador)).thenReturn(true);

        assertThat(servicio.puedeVer(solicitud, contador)).isTrue();
    }

    @Test
    void elAdminVeCualquierSolicitud() {
        solicitud.setEstado(EstadoSolicitud.CERRADA);
        assertThat(servicio.puedeVer(solicitud, usuario(9, Rol.ADMIN))).isTrue();
    }
}
