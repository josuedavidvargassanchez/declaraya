package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Calificacion;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.CalificacionRepository;

class CalificacionServiceTest {

    private CalificacionRepository calificacionRepository;
    private AcuerdoService acuerdoService;
    private NotificacionService notificacionService;
    private CalificacionService servicio;
    private Usuario cliente;
    private Usuario contador;
    private Acuerdo acuerdo;

    @BeforeEach
    void preparar() {
        calificacionRepository = mock(CalificacionRepository.class);
        acuerdoService = mock(AcuerdoService.class);
        notificacionService = mock(NotificacionService.class);
        servicio = new CalificacionService(calificacionRepository, acuerdoService, mock(AcuerdoRepository.class),
                notificacionService, mock(ActividadAcuerdoService.class));

        cliente = new Usuario("Cliente", "c@example.com", "x", Rol.PERSONA);
        cliente.setId(1L);
        contador = new Usuario("Contador", "k@example.com", "x", Rol.CONTADOR_PUBLICO);
        contador.setId(2L);
        SolicitudDeclaracion solicitud = new SolicitudDeclaracion(cliente, "Declaración de renta", "Ayuda");
        acuerdo = new Acuerdo(new Oferta(solicitud, contador, "Oferta", new BigDecimal("300000")), new BigDecimal("30000"));
        acuerdo.setId(20L);
        acuerdo.setEstado(EstadoAcuerdo.FINALIZADO);
        when(acuerdoService.buscarPorId(20L)).thenReturn(acuerdo);
        when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void calificarUnAcuerdoFinalizadoNotificaAlContador() {
        Calificacion c = servicio.calificar(20L, cliente, 5, "Excelente", true);

        assertThat(c.getPuntaje()).isEqualTo(5);
        assertThat(c.getContador()).isSameAs(contador);
        verify(notificacionService).notificar(eq(contador), anyString(), eq("/contadores/2"));
    }

    @Test
    void calificacionDuplicadaRechazada() {
        when(calificacionRepository.existsByAcuerdo(acuerdo)).thenReturn(true);

        assertThatThrownBy(() -> servicio.calificar(20L, cliente, 4, null, true)).isInstanceOf(ReglaNegocioException.class);
        verify(calificacionRepository, never()).save(any());
    }

    @Test
    void laRestriccionUnicaTambienSeTraduceEnError() {
        when(calificacionRepository.save(any(Calificacion.class))).thenThrow(new DataIntegrityViolationException("uk_calificacion_acuerdo"));

        assertThatThrownBy(() -> servicio.calificar(20L, cliente, 4, null, true)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void calificacionDeUnAcuerdoNoFinalizadoRechazada() {
        acuerdo.setEstado(EstadoAcuerdo.ACTIVO);

        assertThatThrownBy(() -> servicio.calificar(20L, cliente, 5, null, true)).isInstanceOf(ReglaNegocioException.class);
        assertThat(servicio.puedeCalificar(acuerdo, cliente)).isFalse();
        verify(calificacionRepository, never()).save(any());
    }

    @Test
    void puntajeFueraDeRangoRechazado() {
        assertThatThrownBy(() -> servicio.calificar(20L, cliente, 6, null, true)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.calificar(20L, cliente, 0, null, true)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elPromedioSoloCuentaLasVisibles() {
        Calificacion visible1 = new Calificacion(acuerdo, cliente, contador, 5, null);
        Calificacion visible2 = new Calificacion(acuerdo, cliente, contador, 3, null);
        when(calificacionRepository.findByContadorAndOcultaFalseOrderByFechaDesc(contador)).thenReturn(List.of(visible1, visible2));

        assertThat(servicio.promedio(contador)).isEqualTo(4.0);
    }

    @Test
    void ocultarExigeMotivoYNoBorra() {
        Calificacion c = new Calificacion(acuerdo, cliente, contador, 1, "Comentario ofensivo");
        c.setId(9L);
        when(calificacionRepository.findById(9L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> servicio.ocultar(9L, " ", true)).isInstanceOf(ReglaNegocioException.class);
        servicio.ocultar(9L, "Lenguaje ofensivo", true);

        assertThat(c.isOculta()).isTrue();
        assertThat(c.getMotivoOcultamiento()).isEqualTo("Lenguaje ofensivo");
        verify(calificacionRepository, never()).delete(any());
    }
}
