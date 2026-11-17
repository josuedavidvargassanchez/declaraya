package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ArchivoService archivoService;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private VerificacionService verificacionService;

    private Usuario admin;
    private Usuario contador;

    @BeforeEach
    void preparar() {
        admin = new Usuario("Administrador", "admin@declaraya.com", "x", Rol.ADMIN);
        admin.setId(1L);

        contador = new Usuario("Contador Prueba", "contador@example.com", "x", Rol.CONTADOR_PUBLICO);
        contador.setId(10L);
        contador.setTarjetaProfesional("TP-123");
        contador.setSoporteTarjetaProfesional("soporte.pdf");
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        contador.setVerificado(false);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(contador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aprobarDesdePendientePoneVerificadoEnTrue() {
        Usuario resultado = verificacionService.aprobar(10L, admin, "Todo en orden", true);

        assertThat(resultado.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.APROBADO);
        assertThat(resultado.isVerificado()).isTrue();
        assertThat(resultado.getRevisadoPor()).isSameAs(admin);
        assertThat(resultado.getFechaRevisionVerificacion()).isNotNull();
        assertThat(resultado.getObservacionVerificacion()).isEqualTo("Todo en orden");
        verify(notificacionService).notificar(eq(contador), anyString(), anyString());
    }

    @Test
    void aprobarSinConfirmacionLanzaError() {
        assertThatThrownBy(() -> verificacionService.aprobar(10L, admin, null, false))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(contador.isVerificado()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void aprobarDesdeRechazadoLanzaError() {
        contador.setEstadoVerificacion(EstadoVerificacion.RECHAZADO);

        assertThatThrownBy(() -> verificacionService.aprobar(10L, admin, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(contador.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADO);
        assertThat(contador.isVerificado()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void aprobarSinSoporteDeTarjetaLanzaError() {
        contador.setSoporteTarjetaProfesional(null);

        assertThatThrownBy(() -> verificacionService.aprobar(10L, admin, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(contador.isVerificado()).isFalse();
    }

    @Test
    void rechazarSinMotivoLanzaError() {
        assertThatThrownBy(() -> verificacionService.rechazar(10L, admin, null, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> verificacionService.rechazar(10L, admin, "   ", null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> verificacionService.rechazar(10L, admin, "corto", null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(contador.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazarConMotivoGuardaMotivoYDejaSinVerificar() {
        Usuario resultado = verificacionService.rechazar(10L, admin, "La tarjeta profesional no es legible", null, true);

        assertThat(resultado.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADO);
        assertThat(resultado.getMotivoRechazo()).isEqualTo("La tarjeta profesional no es legible");
        assertThat(resultado.isVerificado()).isFalse();
        assertThat(resultado.getRevisadoPor()).isSameAs(admin);
    }

    @Test
    void solicitarCorreccionSinObservacionLanzaError() {
        assertThatThrownBy(() -> verificacionService.solicitarCorreccion(10L, admin, " ", true))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void solicitarNuevaVerificacionDesdeAprobadoDejaPendienteYSinVerificar() {
        contador.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        contador.setVerificado(true);

        Usuario resultado = verificacionService.solicitarNuevaVerificacion(contador, "TP-999", true);

        assertThat(resultado.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(resultado.isVerificado()).isFalse();
        assertThat(resultado.getTarjetaProfesional()).isEqualTo("TP-999");
        assertThat(resultado.getFechaSolicitudVerificacion()).isNotNull();
    }

    @Test
    void solicitarNuevaVerificacionSinConfirmarOSinEstarAprobadoLanzaError() {
        assertThatThrownBy(() -> verificacionService.solicitarNuevaVerificacion(contador, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        contador.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        contador.setVerificado(true);
        assertThatThrownBy(() -> verificacionService.solicitarNuevaVerificacion(contador, null, false))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(contador.isVerificado()).isTrue();
    }

    @Test
    void solicitarCorreccionCambiaEstadoYGuardaObservacion() {
        Usuario resultado = verificacionService.solicitarCorreccion(10L, admin, "Sube la tarjeta por ambas caras", true);

        assertThat(resultado.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.CORRECCION_SOLICITADA);
        assertThat(resultado.getObservacionVerificacion()).isEqualTo("Sube la tarjeta por ambas caras");
        assertThat(resultado.isVerificado()).isFalse();
    }
}
