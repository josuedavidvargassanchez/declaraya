package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.declaraya.enums.EstadoEntrega;
import com.example.declaraya.enums.EstadoSolicitudMentoria;
import com.example.declaraya.enums.EstadoSupervision;
import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.enums.TipoSolicitudMentoria;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.AsignacionMentoria;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.SolicitudMentoria;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.ActividadAcuerdoRepository;
import com.example.declaraya.repositories.AsignacionMentoriaRepository;
import com.example.declaraya.repositories.EntregaTrabajoRepository;
import com.example.declaraya.repositories.SolicitudMentoriaRepository;
import com.example.declaraya.repositories.SupervisionRepository;
import com.example.declaraya.repositories.UsuarioRepository;

/** Reglas del flujo de mentoria. Sin base de datos: repositorios simulados y UsuarioService real. */
class MentoriaServiceTest {

    private SolicitudMentoriaRepository solicitudRepository;
    private AsignacionMentoriaRepository asignacionRepository;
    private UsuarioRepository usuarioRepository;
    private SupervisionRepository supervisionRepository;
    private EntregaTrabajoRepository entregaRepository;
    private ActividadAcuerdoService actividadService;
    private NotificacionService notificacionService;
    private UsuarioService usuarioService;
    private MentoriaService mentoria;

    private Usuario admin;
    private Usuario practicante;
    private Usuario mentorActual;
    private Usuario mentorNuevo;

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@example.com", "x", rol);
        u.setId(id);
        return u;
    }

    private static Usuario contadorAprobado(long id) {
        Usuario u = usuario(id, Rol.CONTADOR_PUBLICO);
        u.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        u.setVerificado(true);
        return u;
    }

    @BeforeEach
    void preparar() {
        solicitudRepository = mock(SolicitudMentoriaRepository.class);
        asignacionRepository = mock(AsignacionMentoriaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        supervisionRepository = mock(SupervisionRepository.class);
        entregaRepository = mock(EntregaTrabajoRepository.class);
        actividadService = mock(ActividadAcuerdoService.class);
        notificacionService = mock(NotificacionService.class);

        usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", mock(PasswordEncoder.class));

        mentoria = new MentoriaService(solicitudRepository, asignacionRepository, usuarioRepository, usuarioService,
                supervisionRepository, entregaRepository, mock(AcuerdoRepository.class), actividadService,
                mock(ActividadAcuerdoRepository.class), notificacionService);

        admin = usuario(1, Rol.ADMIN);
        practicante = usuario(3, Rol.CONTADOR_PRACTICANTE);
        practicante.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        practicante.setVerificado(true);
        mentorActual = contadorAprobado(4);
        mentorNuevo = contadorAprobado(5);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(mentorActual));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(mentorNuevo));
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(solicitudRepository.save(any(SolicitudMentoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.save(any(AsignacionMentoria.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SolicitudMentoria solicitudPendiente(TipoSolicitudMentoria tipo) {
        SolicitudMentoria s = new SolicitudMentoria(practicante, tipo, "Declaración de renta", "Necesito un mentor");
        s.setId(50L);
        if (tipo == TipoSolicitudMentoria.CAMBIO) {
            s.setMotivoCambio("Mi mentor no tiene disponibilidad");
            s.setMentorAnterior(practicante.getMentor());
        }
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(s));
        return s;
    }

    private Acuerdo acuerdo(long id) {
        SolicitudDeclaracion sd = new SolicitudDeclaracion(usuario(90, Rol.PERSONA), "Declaración de renta", "Ayuda");
        Acuerdo a = new Acuerdo(new Oferta(sd, practicante, "Oferta", new BigDecimal("200000")), new BigDecimal("20000"));
        a.setId(id);
        return a;
    }

    // ------------------------------------------------------------ solicitudes del practicante

    @Test
    void noPermiteDosSolicitudesPendientes() {
        when(solicitudRepository.existsByPracticanteAndEstado(practicante, EstadoSolicitudMentoria.PENDIENTE))
                .thenReturn(false, true);

        mentoria.solicitar(practicante, "Declaración de renta", "Quiero aprender renta de personas naturales");

        assertThatThrownBy(() -> mentoria.solicitar(practicante, "Declaración de IVA", "Otra solicitud"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("pendiente");
        verify(solicitudRepository, times(1)).save(any(SolicitudMentoria.class));
        verify(notificacionService).notificar(eq(admin), anyString(), anyString());
    }

    @Test
    void noPermiteSolicitarConLaVerificacionPendiente() {
        practicante.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        practicante.setVerificado(false);

        assertThatThrownBy(() -> mentoria.solicitar(practicante, "Declaración de renta", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MentoriaService.VERIFICACION_REQUERIDA);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void laEspecialidadDebeSerDelCatalogo() {
        assertThatThrownBy(() -> mentoria.solicitar(practicante, "Contabilidad de costos", "Hola"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(solicitudRepository, never()).save(any());
    }

    // ------------------------------------------------------------ decisiones del administrador

    @Test
    void asignarUnMentorNoVerificadoLanzaError() {
        SolicitudMentoria solicitud = solicitudPendiente(TipoSolicitudMentoria.NUEVA);
        Usuario sinVerificar = usuario(6, Rol.CONTADOR_PUBLICO);
        sinVerificar.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(sinVerificar));

        assertThatThrownBy(() -> mentoria.asignar(50L, 6L, admin, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        // UsuarioService.asignarMentor aplica la misma regla
        assertThatThrownBy(() -> usuarioService.asignarMentor(practicante, 6L))
                .isInstanceOf(ReglaNegocioException.class);

        assertThat(practicante.getMentor()).isNull();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMentoria.PENDIENTE);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void aprobarUnaSolicitudYaRechazadaLanzaError() {
        SolicitudMentoria solicitud = solicitudPendiente(TipoSolicitudMentoria.NUEVA);
        solicitud.setEstado(EstadoSolicitudMentoria.RECHAZADA);

        assertThatThrownBy(() -> mentoria.asignar(50L, 5L, admin, null, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMentoria.RECHAZADA);
        assertThat(practicante.getMentor()).isNull();
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void asignarSinConfirmacionLanzaError() {
        solicitudPendiente(TipoSolicitudMentoria.NUEVA);

        assertThatThrownBy(() -> mentoria.asignar(50L, 5L, admin, null, false))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(practicante.getMentor()).isNull();
    }

    @Test
    void elCambioDeMentorCierraLaAsignacionAnteriorYTrasladaLasSupervisionesPendientes() {
        practicante.setMentor(mentorActual);
        SolicitudMentoria solicitud = solicitudPendiente(TipoSolicitudMentoria.CAMBIO);
        AsignacionMentoria anterior = new AsignacionMentoria(practicante, mentorActual, null, null,
                LocalDateTime.now().minusMonths(3), MentoriaService.NOTA_MIGRACION);
        when(asignacionRepository.findByPracticanteAndFechaFinIsNull(practicante)).thenReturn(List.of(anterior));

        Acuerdo porAprobar = acuerdo(20);
        Acuerdo enCurso = acuerdo(21);
        Supervision pendiente = new Supervision(practicante, mentorActual, porAprobar);
        Supervision aprobada = new Supervision(practicante, mentorActual, enCurso);
        aprobada.setEstado(EstadoSupervision.APROBADO);
        EntregaTrabajo enRevision = new EntregaTrabajo();
        enRevision.setAcuerdo(enCurso);
        enRevision.setContador(practicante);
        enRevision.setEstado(EstadoEntrega.EN_REVISION_MENTOR);
        enRevision.setMentor(mentorActual);
        when(supervisionRepository.findByPracticanteAndEstado(practicante, EstadoSupervision.PENDIENTE_APROBACION))
                .thenReturn(List.of(pendiente));
        when(supervisionRepository.findByPracticanteAndEstado(practicante, EstadoSupervision.APROBADO))
                .thenReturn(List.of(aprobada));
        when(entregaRepository.findByContadorAndEstado(practicante, EstadoEntrega.EN_REVISION_MENTOR))
                .thenReturn(List.of(enRevision));

        mentoria.asignar(50L, 5L, admin, "Mentor con experiencia en renta", true);

        // la asignacion anterior queda cerrada y se crea la nueva
        assertThat(anterior.getFechaFin()).isNotNull();
        ArgumentCaptor<AsignacionMentoria> guardadas = ArgumentCaptor.forClass(AsignacionMentoria.class);
        verify(asignacionRepository, times(2)).save(guardadas.capture());
        AsignacionMentoria nueva = guardadas.getAllValues().stream().filter(AsignacionMentoria::isVigente).findFirst().orElseThrow();
        assertThat(nueva.getMentor()).isSameAs(mentorNuevo);
        assertThat(nueva.getSolicitud()).isSameAs(solicitud);
        assertThat(nueva.getAsignadaPor()).isSameAs(admin);
        assertThat(nueva.getObservacion()).isEqualTo("Mentor con experiencia en renta");

        // Usuario.mentor sigue siendo la fuente de verdad
        assertThat(practicante.getMentor()).isSameAs(mentorNuevo);

        // lo pendiente pasa al mentor nuevo; lo aprobado conserva a quien lo aprobo
        assertThat(pendiente.getMentor()).isSameAs(mentorNuevo);
        assertThat(enRevision.getMentor()).isSameAs(mentorNuevo);
        assertThat(aprobada.getMentor()).isSameAs(mentorActual);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMentoria.APROBADA);
        assertThat(solicitud.getMentorAsignado()).isSameAs(mentorNuevo);
        assertThat(solicitud.getRevisadaPor()).isSameAs(admin);
        assertThat(solicitud.getFechaRevision()).isNotNull();

        // cada acuerdo afectado deja constancia en su historial
        verify(actividadService).registrar(eq(porAprobar), eq(admin), eq(TipoActividad.CAMBIO_MENTOR), anyString());
        verify(actividadService).registrar(eq(enCurso), eq(admin), eq(TipoActividad.CAMBIO_MENTOR), anyString());

        verify(notificacionService).notificar(eq(practicante), anyString(), eq("/practicante/mentor"));
        verify(notificacionService).notificar(eq(mentorNuevo), anyString(), eq("/contador/practicantes/3"));
        verify(notificacionService).notificar(eq(mentorActual), anyString(), eq("/contador/inicio"));
    }

    @Test
    void rechazarUnCambioConservaAlMentorActual() {
        practicante.setMentor(mentorActual);
        SolicitudMentoria solicitud = solicitudPendiente(TipoSolicitudMentoria.CAMBIO);

        mentoria.rechazar(50L, admin, "No hay otro mentor con esa especialidad", true);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMentoria.RECHAZADA);
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("No hay otro mentor con esa especialidad");
        assertThat(practicante.getMentor()).isSameAs(mentorActual);
        verify(asignacionRepository, never()).save(any());
        verify(notificacionService).notificar(eq(practicante), anyString(), eq("/practicante/mentor"));
    }

    @Test
    void rechazarSinMotivoLanzaError() {
        solicitudPendiente(TipoSolicitudMentoria.NUEVA);

        assertThatThrownBy(() -> mentoria.rechazar(50L, admin, "  ", true)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unPracticanteNoPuedeCancelarLaSolicitudDeOtro() {
        solicitudPendiente(TipoSolicitudMentoria.NUEVA);
        Usuario otro = usuario(7, Rol.CONTADOR_PRACTICANTE);

        assertThatThrownBy(() -> mentoria.cancelar(50L, otro, true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
