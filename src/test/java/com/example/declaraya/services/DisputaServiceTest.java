package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.declaraya.enums.AccionDisputa;
import com.example.declaraya.enums.EstadoDisputa;
import com.example.declaraya.enums.ResultadoDisputa;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.HistorialDisputa;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.DisputaRepository;
import com.example.declaraya.repositories.HistorialDisputaRepository;
import com.example.declaraya.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisputaServiceTest {

    private static final String JUSTIFICACION = "El contador entregó la declaración según lo acordado en el chat.";

    @Mock
    private DisputaRepository disputaRepository;

    @Mock
    private HistorialDisputaRepository historialRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private ActividadAcuerdoService actividadService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DisputaService disputaService;

    private Usuario admin;
    private Usuario cliente;
    private Usuario contador;
    private Acuerdo acuerdo;
    private Disputa disputa;
    private Chat chat;

    @BeforeEach
    void preparar() {
        admin = new Usuario("Administrador", "admin@declaraya.com", "x", Rol.ADMIN);
        admin.setId(1L);
        cliente = new Usuario("Cliente", "cliente@example.com", "x", Rol.PERSONA);
        cliente.setId(2L);
        contador = new Usuario("Contador", "contador@example.com", "x", Rol.CONTADOR_PUBLICO);
        contador.setId(3L);

        SolicitudDeclaracion solicitud = new SolicitudDeclaracion();
        solicitud.setPersona(cliente);
        Oferta oferta = new Oferta();
        oferta.setSolicitud(solicitud);
        oferta.setContador(contador);
        oferta.setValorPropuesto(new BigDecimal("300000"));
        acuerdo = new Acuerdo(oferta, new BigDecimal("30000"));
        acuerdo.setId(7L);
        chat = new Chat(acuerdo);

        disputa = new Disputa();
        disputa.setId(5L);
        disputa.setAcuerdo(acuerdo);
        disputa.setAbiertaPor(cliente);
        disputa.setMotivo("El contador no responde");
        disputa.setEstado(EstadoDisputa.EN_REVISION);

        when(disputaRepository.findById(5L)).thenReturn(Optional.of(disputa));
        when(disputaRepository.save(any(Disputa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatService.buscarPorAcuerdoId(7L)).thenReturn(chat);
    }

    private HistorialDisputa historialGuardado() {
        ArgumentCaptor<HistorialDisputa> captor = ArgumentCaptor.forClass(HistorialDisputa.class);
        verify(historialRepository).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------ resolver

    @Test
    void resolverSinJustificacionLanzaError() {
        assertThatThrownBy(() -> disputaService.resolver(5L, admin, ResultadoDisputa.A_FAVOR_CLIENTE, null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> disputaService.resolver(5L, admin, ResultadoDisputa.A_FAVOR_CLIENTE, "Muy corta"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(disputa.getEstado()).isEqualTo(EstadoDisputa.EN_REVISION);
        verify(disputaRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    void resolverSinDecisionLanzaError() {
        assertThatThrownBy(() -> disputaService.resolver(5L, admin, null, JUSTIFICACION))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void resolverUnaDisputaYaResueltaLanzaError() {
        disputa.setEstado(EstadoDisputa.RESUELTA);

        assertThatThrownBy(() -> disputaService.resolver(5L, admin, ResultadoDisputa.A_FAVOR_CONTADOR, JUSTIFICACION))
                .isInstanceOf(ReglaNegocioException.class);
        verify(disputaRepository, never()).save(any());
    }

    @Test
    void resolverAFavorDeUnaParteDejaResueltaYGuardaHistorial() {
        Disputa resultado = disputaService.resolver(5L, admin, ResultadoDisputa.A_FAVOR_CLIENTE, JUSTIFICACION);

        assertThat(resultado.getEstado()).isEqualTo(EstadoDisputa.RESUELTA);
        assertThat(resultado.getResultado()).isEqualTo(ResultadoDisputa.A_FAVOR_CLIENTE);
        assertThat(resultado.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(resultado.getResueltaPor()).isSameAs(admin);
        assertThat(resultado.getFechaResolucion()).isNotNull();

        HistorialDisputa h = historialGuardado();
        assertThat(h.getAccion()).isEqualTo(AccionDisputa.RESOLUCION);
        assertThat(h.getEstadoAnterior()).isEqualTo(EstadoDisputa.EN_REVISION);
        assertThat(h.getEstadoNuevo()).isEqualTo(EstadoDisputa.RESUELTA);
        assertThat(h.getDecision()).isEqualTo(ResultadoDisputa.A_FAVOR_CLIENTE);
        assertThat(h.getDetalle()).isEqualTo(JUSTIFICACION);
        assertThat(h.getActor()).isSameAs(admin);

        verify(notificacionService).notificar(eq(cliente), anyString(), anyString());
        verify(notificacionService).notificar(eq(contador), anyString(), anyString());
    }

    @Test
    void resolverSinSancionDejaCerrada() {
        Disputa resultado = disputaService.resolver(5L, admin, ResultadoDisputa.SIN_SANCION, JUSTIFICACION);

        assertThat(resultado.getEstado()).isEqualTo(EstadoDisputa.CERRADA);
        HistorialDisputa h = historialGuardado();
        assertThat(h.getEstadoAnterior()).isEqualTo(EstadoDisputa.EN_REVISION);
        assertThat(h.getEstadoNuevo()).isEqualTo(EstadoDisputa.CERRADA);
        assertThat(h.getDecision()).isEqualTo(ResultadoDisputa.SIN_SANCION);
    }

    @Test
    void resolverNoModificaElAcuerdo() {
        var estadoAntes = acuerdo.getEstado();
        disputaService.resolver(5L, admin, ResultadoDisputa.A_FAVOR_CLIENTE, JUSTIFICACION);

        assertThat(acuerdo.getEstado()).isEqualTo(estadoAntes);
        assertThat(acuerdo.getComisionPlataforma()).isEqualByComparingTo("30000");
    }

    // ------------------------------------------------------------ mensajes

    @Test
    void enviarMensajePasaDeAbiertaAEnRevisionYGuardaHistorial() {
        disputa.setEstado(EstadoDisputa.ABIERTA);

        Disputa resultado = disputaService.enviarMensaje(5L, admin, DisputaService.DESTINO_CONTADOR,
                DisputaService.TIPO_SOLICITUD_INFO, "Comparte el borrador de la declaración");

        assertThat(resultado.getEstado()).isEqualTo(EstadoDisputa.EN_REVISION);
        verify(chatService).enviarMensaje(eq(chat), eq(admin),
                eq("[Administración → Contador] Solicitud de información: Comparte el borrador de la declaración"));
        verify(notificacionService).notificar(eq(contador), anyString(), eq("/acuerdos/7/chat"));

        HistorialDisputa h = historialGuardado();
        assertThat(h.getAccion()).isEqualTo(AccionDisputa.SOLICITUD_INFO_CONTADOR);
        assertThat(h.getEstadoAnterior()).isEqualTo(EstadoDisputa.ABIERTA);
        assertThat(h.getEstadoNuevo()).isEqualTo(EstadoDisputa.EN_REVISION);
        assertThat(h.getDecision()).isNull();
    }

    @Test
    void enviarMensajeEnRevisionMantieneElEstado() {
        disputaService.enviarMensaje(5L, admin, DisputaService.DESTINO_CLIENTE, DisputaService.TIPO_MENSAJE, "Gracias por la información");

        assertThat(disputa.getEstado()).isEqualTo(EstadoDisputa.EN_REVISION);
        verify(chatService).enviarMensaje(eq(chat), eq(admin), eq("[Administración → Cliente] Gracias por la información"));
        verify(notificacionService).notificar(eq(cliente), anyString(), anyString());
        HistorialDisputa h = historialGuardado();
        assertThat(h.getAccion()).isEqualTo(AccionDisputa.MENSAJE_CLIENTE);
        assertThat(h.getEstadoAnterior()).isEqualTo(EstadoDisputa.EN_REVISION);
        assertThat(h.getEstadoNuevo()).isEqualTo(EstadoDisputa.EN_REVISION);
    }

    @Test
    void enviarMensajeVacioLanzaError() {
        assertThatThrownBy(() -> disputaService.enviarMensaje(5L, admin, DisputaService.DESTINO_CLIENTE, DisputaService.TIPO_MENSAJE, "  "))
                .isInstanceOf(ReglaNegocioException.class);
        verify(chatService, never()).enviarMensaje(any(), any(), anyString());
    }

    @Test
    void enviarMensajeAUnaDisputaCerradaLanzaError() {
        disputa.setEstado(EstadoDisputa.CERRADA);

        assertThatThrownBy(() -> disputaService.enviarMensaje(5L, admin, DisputaService.DESTINO_CLIENTE, DisputaService.TIPO_MENSAJE, "Hola"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    // ------------------------------------------------------------ apertura

    @Test
    void abrirRegistraAperturaEnElHistorial() {
        when(disputaRepository.save(any(Disputa.class))).thenAnswer(inv -> inv.getArgument(0));

        Disputa nueva = disputaService.abrir(acuerdo, cliente, "No recibí el borrador");

        assertThat(nueva.getEstado()).isEqualTo(EstadoDisputa.ABIERTA);
        HistorialDisputa h = historialGuardado();
        assertThat(h.getAccion()).isEqualTo(AccionDisputa.APERTURA);
        assertThat(h.getEstadoAnterior()).isNull();
        assertThat(h.getEstadoNuevo()).isEqualTo(EstadoDisputa.ABIERTA);
        assertThat(h.getActor()).isSameAs(cliente);
        assertThat(h.getDetalle()).isEqualTo("No recibí el borrador");
    }
}
