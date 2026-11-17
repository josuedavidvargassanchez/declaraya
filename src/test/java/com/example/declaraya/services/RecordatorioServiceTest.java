package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EtapaAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;

class RecordatorioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private Acuerdo acuerdo(long id, LocalDate entrega, EtapaAcuerdo etapa, Usuario cliente, Usuario contador) {
        SolicitudDeclaracion s = new SolicitudDeclaracion(cliente, "Declaración de renta", "Ayuda");
        Oferta o = new Oferta(s, contador, "Oferta", new BigDecimal("300000"));
        o.setFechaEntregaEstimada(entrega);
        Acuerdo a = new Acuerdo(o, BigDecimal.ZERO);
        a.setId(id);
        a.setEtapa(etapa);
        return a;
    }

    @Test
    void avisaSoloCuandoFaltanDosDiasOMenosYNoRepite() {
        AcuerdoRepository repo = mock(AcuerdoRepository.class);
        NotificacionService notificaciones = mock(NotificacionService.class);
        RecordatorioService servicio = new RecordatorioService(repo, notificaciones, mock(ActividadAcuerdoService.class));
        Usuario cliente = new Usuario("Cliente", "c@example.com", "x", Rol.PERSONA);
        Usuario contador = new Usuario("Contador", "k@example.com", "x", Rol.CONTADOR_PUBLICO);

        Acuerdo vencePronto = acuerdo(1, HOY.plusDays(2), EtapaAcuerdo.PREPARACION, cliente, contador);
        Acuerdo vaLejos = acuerdo(2, HOY.plusDays(10), EtapaAcuerdo.DOCUMENTACION, cliente, contador);
        Acuerdo yaEntregado = acuerdo(3, HOY.plusDays(1), EtapaAcuerdo.ENTREGADO, cliente, contador);
        Acuerdo sinFecha = acuerdo(4, null, EtapaAcuerdo.DOCUMENTACION, cliente, contador);
        when(repo.findByEstadoAndRecordatorioEnviadoFalse(EstadoAcuerdo.ACTIVO))
                .thenReturn(List.of(vencePronto, vaLejos, yaEntregado, sinFecha));

        assertThat(servicio.enviarRecordatorios(HOY)).isEqualTo(1);
        assertThat(vencePronto.isRecordatorioEnviado()).isTrue();
        assertThat(vaLejos.isRecordatorioEnviado()).isFalse();
        assertThat(yaEntregado.isRecordatorioEnviado()).isFalse();
        verify(notificaciones).notificar(eq(cliente), anyString(), eq("/acuerdos/1"));
        verify(notificaciones).notificar(eq(contador), anyString(), eq("/acuerdos/1"));
        verify(notificaciones, never()).notificar(eq(cliente), anyString(), eq("/acuerdos/2"));
        verify(repo, times(1)).save(vencePronto);
    }
}
