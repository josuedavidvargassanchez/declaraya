package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.OfertaRepository;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;

/** A2: aceptar una oferta bloquea la solicitud y vuelve a validar dentro de la transaccion. */
class OfertaServiceTest {

    private OfertaRepository ofertaRepository;
    private SolicitudDeclaracionRepository solicitudRepository;
    private AcuerdoService acuerdoService;
    private OfertaService servicio;

    private Usuario persona;
    private SolicitudDeclaracion solicitud;
    private Oferta oferta;

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@example.com", "x", rol);
        u.setId(id);
        return u;
    }

    @BeforeEach
    void preparar() {
        ofertaRepository = mock(OfertaRepository.class);
        solicitudRepository = mock(SolicitudDeclaracionRepository.class);
        acuerdoService = mock(AcuerdoService.class);
        servicio = new OfertaService();
        ReflectionTestUtils.setField(servicio, "ofertaRepository", ofertaRepository);
        ReflectionTestUtils.setField(servicio, "solicitudRepository", solicitudRepository);
        ReflectionTestUtils.setField(servicio, "acuerdoService", acuerdoService);
        ReflectionTestUtils.setField(servicio, "notificacionService", mock(NotificacionService.class));
        ReflectionTestUtils.setField(servicio, "supervisionService", mock(SupervisionService.class));

        persona = usuario(1, Rol.PERSONA);
        solicitud = new SolicitudDeclaracion(persona, "Declaración de renta", "Ayuda");
        solicitud.setId(10L);
        oferta = new Oferta(solicitud, usuario(2, Rol.CONTADOR_PUBLICO), "Oferta", new BigDecimal("300000"));
        oferta.setId(30L);
        when(solicitudRepository.bloquearPorOferta(30L)).thenReturn(Optional.of(solicitud));
        when(ofertaRepository.findById(30L)).thenReturn(Optional.of(oferta));
        when(ofertaRepository.findBySolicitud(solicitud)).thenReturn(List.of(oferta));
    }

    @Test
    void aceptarCuandoLaSolicitudYaNoEstaAbiertaFallaYNoCreaAcuerdo() {
        // otra pestaña ya acepto: al obtener el bloqueo la solicitud esta EN_PROCESO
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);

        assertThatThrownBy(() -> servicio.aceptar(30L, persona)).isInstanceOf(ReglaNegocioException.class);

        verify(acuerdoService, never()).crearDesdeOferta(any());
        verify(ofertaRepository, never()).save(any());
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.PENDIENTE);
    }

    @Test
    void aceptarUnaOfertaQueYaNoEstaPendienteFalla() {
        oferta.setEstado(EstadoOferta.RETIRADA);

        assertThatThrownBy(() -> servicio.aceptar(30L, persona)).isInstanceOf(ReglaNegocioException.class);
        verify(acuerdoService, never()).crearDesdeOferta(any());
    }

    @Test
    void otraPersonaNoPuedeAceptar() {
        assertThatThrownBy(() -> servicio.aceptar(30L, usuario(5, Rol.PERSONA))).isInstanceOf(ResponseStatusException.class);
        verify(acuerdoService, never()).crearDesdeOferta(any());
    }

    @Test
    void aceptarUnaOfertaPendienteCreaUnSoloAcuerdo() {
        Acuerdo acuerdo = new Acuerdo(oferta, new BigDecimal("30000"));
        acuerdo.setId(20L);
        when(acuerdoService.crearDesdeOferta(oferta)).thenReturn(acuerdo);

        Acuerdo resultado = servicio.aceptar(30L, persona);

        assertThat(resultado).isSameAs(acuerdo);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ACEPTADA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_PROCESO);
        verify(solicitudRepository).bloquearPorOferta(30L);
    }
}
