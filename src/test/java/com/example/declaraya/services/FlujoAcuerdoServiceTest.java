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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoEntrega;
import com.example.declaraya.enums.EstadoRequerimiento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.EtapaAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.DocumentoClienteRepository;
import com.example.declaraya.repositories.EntregaTrabajoRepository;
import com.example.declaraya.repositories.RequerimientoDocumentoRepository;

class FlujoAcuerdoServiceTest {

    private AcuerdoRepository acuerdoRepository;
    private AcuerdoService acuerdoService;
    private NotificacionService notificacionService;
    private SupervisionService supervisionService;
    private RequerimientoDocumentoRepository requerimientoRepository;
    private EntregaTrabajoRepository entregaRepository;
    private ArchivoService archivoService;
    private SolicitudDeclaracionService solicitudService;
    private FlujoAcuerdoService flujo;

    private Usuario cliente;
    private Usuario contador;
    private Usuario practicante;
    private Usuario mentor;
    private Usuario otroContador;
    private SolicitudDeclaracion solicitud;
    private Acuerdo acuerdo;

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@example.com", "x", rol);
        u.setId(id);
        return u;
    }

    private Acuerdo acuerdoCon(Usuario quienAtiende, EtapaAcuerdo etapa) {
        Oferta oferta = new Oferta(solicitud, quienAtiende, "Oferta", new BigDecimal("300000"));
        Acuerdo a = new Acuerdo(oferta, new BigDecimal("30000"));
        a.setId(20L);
        a.setEtapa(etapa);
        when(acuerdoService.buscarPorId(20L)).thenReturn(a);
        return a;
    }

    @BeforeEach
    void preparar() {
        acuerdoRepository = mock(AcuerdoRepository.class);
        acuerdoService = mock(AcuerdoService.class);
        notificacionService = mock(NotificacionService.class);
        supervisionService = mock(SupervisionService.class);
        requerimientoRepository = mock(RequerimientoDocumentoRepository.class);
        entregaRepository = mock(EntregaTrabajoRepository.class);
        archivoService = mock(ArchivoService.class);
        solicitudService = mock(SolicitudDeclaracionService.class);
        flujo = new FlujoAcuerdoService(acuerdoRepository, acuerdoService, mock(ActividadAcuerdoService.class),
                notificacionService, supervisionService, requerimientoRepository, mock(DocumentoClienteRepository.class),
                new CatalogoRequisitos(), entregaRepository, archivoService, solicitudService);

        cliente = usuario(1, Rol.PERSONA);
        contador = usuario(2, Rol.CONTADOR_PUBLICO);
        practicante = usuario(3, Rol.CONTADOR_PRACTICANTE);
        mentor = usuario(4, Rol.CONTADOR_PUBLICO);
        otroContador = usuario(5, Rol.CONTADOR_PUBLICO);

        solicitud = new SolicitudDeclaracion(cliente, "Declaración de renta", "Ayuda");
        solicitud.setId(10L);
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        acuerdo = acuerdoCon(contador, EtapaAcuerdo.DOCUMENTACION);

        when(acuerdoRepository.save(any(Acuerdo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entregaRepository.save(any(EntregaTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(acuerdoService.finalizar(any(Acuerdo.class))).thenAnswer(inv -> {
            Acuerdo a = inv.getArgument(0);
            a.setEstado(EstadoAcuerdo.FINALIZADO);
            return a;
        });
    }

    // ------------------------------------------------------------ transiciones

    @Test
    void transicionesValidas() {
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.APROBACION_MENTOR, EtapaAcuerdo.DOCUMENTACION)).isTrue();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.DOCUMENTACION, EtapaAcuerdo.PREPARACION)).isTrue();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.PREPARACION, EtapaAcuerdo.REVISION_MENTOR)).isTrue();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.PREPARACION, EtapaAcuerdo.ENTREGADO)).isTrue();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.REVISION_MENTOR, EtapaAcuerdo.PREPARACION)).isTrue();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.ENTREGADO, EtapaAcuerdo.FINALIZADO)).isTrue();
    }

    @Test
    void transicionesInvalidas() {
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.DOCUMENTACION, EtapaAcuerdo.ENTREGADO)).isFalse();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.DOCUMENTACION, EtapaAcuerdo.FINALIZADO)).isFalse();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.APROBACION_MENTOR, EtapaAcuerdo.PREPARACION)).isFalse();
        assertThat(FlujoAcuerdoService.transicionPermitida(EtapaAcuerdo.FINALIZADO, EtapaAcuerdo.PREPARACION)).isFalse();
        assertThat(FlujoAcuerdoService.transicionPermitida(null, EtapaAcuerdo.DOCUMENTACION)).isFalse();

        assertThatThrownBy(() -> flujo.cambiarEtapa(acuerdo, EtapaAcuerdo.FINALIZADO, contador))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(acuerdo.getEtapa()).isEqualTo(EtapaAcuerdo.DOCUMENTACION);
        verify(acuerdoRepository, never()).save(any());
    }

    // ------------------------------------------------------------ preparacion

    @Test
    void noSePuedeIniciarLaPreparacionConRequerimientosPendientes() {
        when(requerimientoRepository.countByAcuerdoAndEstado(acuerdo, EstadoRequerimiento.PENDIENTE)).thenReturn(2L);

        assertThatThrownBy(() -> flujo.iniciarPreparacion(20L, contador)).isInstanceOf(ReglaNegocioException.class);
        assertThat(acuerdo.getEtapa()).isEqualTo(EtapaAcuerdo.DOCUMENTACION);
    }

    @Test
    void sinPendientesLaPreparacionAvanzaAunqueFaltenDocumentosDelCatalogo() {
        when(requerimientoRepository.countByAcuerdoAndEstado(acuerdo, EstadoRequerimiento.PENDIENTE)).thenReturn(0L);

        flujo.iniciarPreparacion(20L, contador);

        assertThat(acuerdo.getEtapa()).isEqualTo(EtapaAcuerdo.PREPARACION);
        assertThat(flujo.obligatoriosFaltantes(acuerdo)).isNotEmpty();
    }

    @Test
    void otroContadorNoPuedeIniciarLaPreparacion() {
        assertThatThrownBy(() -> flujo.iniciarPreparacion(20L, otroContador)).isInstanceOf(ResponseStatusException.class);
    }

    // ------------------------------------------------------------ entregas

    @Test
    void laEntregaDeUnPracticantePasaPorElMentor() throws Exception {
        Acuerdo delPracticante = acuerdoCon(practicante, EtapaAcuerdo.PREPARACION);
        Supervision supervision = new Supervision(practicante, mentor, delPracticante);
        when(supervisionService.buscarPorAcuerdo(delPracticante)).thenReturn(Optional.of(supervision));
        when(archivoService.guardarEn(eq(ArchivoService.CARPETA_ENTREGAS), any())).thenReturn("uuid.pdf");
        MockMultipartFile pdf = new MockMultipartFile("archivo", "declaracion.pdf", "application/pdf", "%PDF".getBytes());

        EntregaTrabajo entrega = flujo.entregar(20L, practicante, "Borrador de la declaración", pdf);

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.EN_REVISION_MENTOR);
        assertThat(delPracticante.getEtapa()).isEqualTo(EtapaAcuerdo.REVISION_MENTOR);
        verify(notificacionService).notificar(eq(mentor), anyString(), eq("/acuerdos/20"));
        verify(notificacionService, never()).notificar(eq(cliente), anyString(), anyString());
    }

    @Test
    void laEntregaDeUnContadorPublicoVaDirectoAlCliente() throws Exception {
        acuerdo.setEtapa(EtapaAcuerdo.PREPARACION);
        when(archivoService.guardarEn(eq(ArchivoService.CARPETA_ENTREGAS), any())).thenReturn("uuid.pdf");
        MockMultipartFile pdf = new MockMultipartFile("archivo", "declaracion.pdf", "application/pdf", "%PDF".getBytes());

        EntregaTrabajo entrega = flujo.entregar(20L, contador, "Declaración lista", pdf);

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENVIADA);
        assertThat(acuerdo.getEtapa()).isEqualTo(EtapaAcuerdo.ENTREGADO);
        verify(notificacionService).notificar(eq(cliente), anyString(), eq("/acuerdos/20"));
    }

    @Test
    void noSePuedeEntregarFueraDePreparacion() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "d.pdf", "application/pdf", "%PDF".getBytes());
        assertThatThrownBy(() -> flujo.entregar(20L, contador, "Antes de tiempo", pdf))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void confirmarLaRecepcionFinalizaElAcuerdo() {
        acuerdo.setEtapa(EtapaAcuerdo.ENTREGADO);
        EntregaTrabajo enviada = entrega(30L, EstadoEntrega.ENVIADA);

        Acuerdo resultado = flujo.confirmarRecepcion(20L, 30L, cliente, true);

        assertThat(enviada.getEstado()).isEqualTo(EstadoEntrega.RECIBIDA);
        assertThat(resultado.getEtapa()).isEqualTo(EtapaAcuerdo.FINALIZADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoAcuerdo.FINALIZADO);
        verify(acuerdoService).finalizar(acuerdo);
        verify(solicitudService).actualizarEstado(10L, EstadoSolicitud.CERRADA);
        verify(notificacionService).notificar(eq(contador), anyString(), anyString());
    }

    @Test
    void confirmarSinCasillaOPorOtroUsuarioSeRechaza() {
        acuerdo.setEtapa(EtapaAcuerdo.ENTREGADO);
        entrega(30L, EstadoEntrega.ENVIADA);

        assertThatThrownBy(() -> flujo.confirmarRecepcion(20L, 30L, cliente, false)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> flujo.confirmarRecepcion(20L, 30L, contador, true)).isInstanceOf(ResponseStatusException.class);
        verify(acuerdoService, never()).finalizar(any());
    }

    @Test
    void reportarInconvenienteVuelveAPreparacion() {
        acuerdo.setEtapa(EtapaAcuerdo.ENTREGADO);
        EntregaTrabajo enviada = entrega(30L, EstadoEntrega.ENVIADA);

        flujo.reportarInconveniente(20L, 30L, cliente, "Falta incluir el certificado de ingresos", true);

        assertThat(enviada.getEstado()).isEqualTo(EstadoEntrega.CON_INCONVENIENTE);
        assertThat(acuerdo.getEtapa()).isEqualTo(EtapaAcuerdo.PREPARACION);
        verify(acuerdoService, never()).finalizar(any());
    }

    // ------------------------------------------------------------ permisos

    @Test
    void unContadorSinAcuerdoNoPuedeVerEntregas() {
        EntregaTrabajo e = entrega(30L, EstadoEntrega.ENVIADA);
        when(acuerdoService.puedeVer(acuerdo, otroContador)).thenReturn(false);
        when(acuerdoService.puedeVer(acuerdo, contador)).thenReturn(true);

        assertThat(flujo.puedeVerEntrega(e, otroContador)).isFalse();
        assertThat(flujo.puedeVerEntrega(e, contador)).isTrue();
        assertThatThrownBy(() -> flujo.exigirAccesoEntrega(e, otroContador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void elClienteNoVeLaEntregaQueRevisaElMentor() {
        EntregaTrabajo enRevision = entrega(31L, EstadoEntrega.EN_REVISION_MENTOR);
        when(acuerdoService.puedeVer(acuerdo, cliente)).thenReturn(true);

        assertThat(flujo.puedeVerEntrega(enRevision, cliente)).isFalse();
    }

    private EntregaTrabajo entrega(long id, EstadoEntrega estado) {
        EntregaTrabajo e = new EntregaTrabajo();
        e.setId(id);
        e.setAcuerdo(acuerdo);
        e.setContador(contador);
        e.setDescripcion("Trabajo");
        e.setArchivo("uuid.pdf");
        e.setNombreOriginal("trabajo.pdf");
        e.setEstado(estado);
        when(entregaRepository.findById(id)).thenReturn(Optional.of(e));
        when(entregaRepository.findByAcuerdoOrderByFechaDesc(acuerdo)).thenReturn(List.of(e));
        return e;
    }
}
