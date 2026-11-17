package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.CategoriaDocumento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoDocumentoCliente;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.ArchivoRepository;
import com.example.declaraya.repositories.DocumentoClienteRepository;
import com.example.declaraya.repositories.RequerimientoDocumentoRepository;

class DocumentoClienteServiceTest {

    @TempDir
    Path uploads;

    private DocumentoClienteRepository documentoRepository;
    private AcuerdoRepository acuerdoRepository;
    private SupervisionService supervisionService;
    private DisputaService disputaService;
    private SolicitudDeclaracionService solicitudService;
    private DocumentoClienteService servicio;

    private Usuario cliente;
    private Usuario otroCliente;
    private Usuario contadorDelAcuerdo;
    private Usuario otroContador;
    private Usuario mentor;
    private Usuario admin;
    private SolicitudDeclaracion solicitud;
    private Acuerdo acuerdo;
    private DocumentoCliente documento;

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@example.com", "x", rol);
        u.setId(id);
        return u;
    }

    @BeforeEach
    void preparar() throws Exception {
        documentoRepository = mock(DocumentoClienteRepository.class);
        acuerdoRepository = mock(AcuerdoRepository.class);
        supervisionService = mock(SupervisionService.class);
        disputaService = mock(DisputaService.class);
        solicitudService = mock(SolicitudDeclaracionService.class);
        // ArchivoService real sobre una carpeta temporal: asi se prueba la validacion de verdad
        ArchivoService archivoService = new ArchivoService(mock(ArchivoRepository.class), uploads.toString());
        servicio = new DocumentoClienteService(documentoRepository, archivoService, acuerdoRepository,
                supervisionService, disputaService, solicitudService, mock(RequerimientoDocumentoRepository.class),
                mock(ActividadAcuerdoService.class), mock(NotificacionService.class), new CatalogoRequisitos());

        cliente = usuario(1, Rol.PERSONA);
        otroCliente = usuario(2, Rol.PERSONA);
        contadorDelAcuerdo = usuario(3, Rol.CONTADOR_PUBLICO);
        otroContador = usuario(4, Rol.CONTADOR_PUBLICO);
        mentor = usuario(5, Rol.CONTADOR_PUBLICO);
        admin = usuario(6, Rol.ADMIN);

        solicitud = new SolicitudDeclaracion(cliente, "Declaración de renta", "Ayuda");
        solicitud.setId(10L);
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        Oferta oferta = new Oferta(solicitud, contadorDelAcuerdo, "Oferta", new BigDecimal("300000"));
        acuerdo = new Acuerdo(oferta, new BigDecimal("30000"));
        acuerdo.setId(20L);

        documento = new DocumentoCliente();
        documento.setId(30L);
        documento.setPropietario(cliente);
        documento.setTipo(TipoDocumentoCliente.RUT);
        documento.setNombre("RUT");
        documento.setNombreOriginal("rut.pdf");
        documento.setArchivo("abc.pdf");
        documento.setSolicitud(solicitud);

        when(acuerdoRepository.findByOfertaSolicitud(solicitud)).thenReturn(List.of(acuerdo));
        when(documentoRepository.findById(30L)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(DocumentoCliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void elPropietarioPuedeVer() {
        assertThat(servicio.puedeVer(documento, cliente)).isTrue();
        // tambien un documento sin solicitud
        documento.setSolicitud(null);
        assertThat(servicio.puedeVer(documento, cliente)).isTrue();
    }

    @Test
    void otroClienteRecibe403() {
        assertThat(servicio.puedeVer(documento, otroCliente)).isFalse();
        assertThatThrownBy(() -> servicio.exigirAcceso(documento, otroCliente))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void contadorConAcuerdoPuedeVerYContadorSinAcuerdoNo() {
        assertThat(servicio.puedeVer(documento, contadorDelAcuerdo)).isTrue();
        assertThat(servicio.puedeVer(documento, otroContador)).isFalse();
        assertThatThrownBy(() -> servicio.exigirAcceso(documento, otroContador))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sinSolicitudVinculadaElContadorNoPuedeVer() {
        documento.setSolicitud(null);
        assertThat(servicio.puedeVer(documento, contadorDelAcuerdo)).isFalse();
    }

    @Test
    void elMentorDelPracticantePuedeVer() {
        when(supervisionService.esMentorDe(mentor, acuerdo)).thenReturn(true);
        assertThat(servicio.puedeVer(documento, mentor)).isTrue();
    }

    @Test
    void elAdminSoloPuedeVerConDisputaActiva() {
        when(disputaService.hayDisputaAbierta(acuerdo)).thenReturn(false);
        assertThat(servicio.puedeVer(documento, admin)).isFalse();
        when(disputaService.hayDisputaAbierta(acuerdo)).thenReturn(true);
        assertThat(servicio.puedeVer(documento, admin)).isTrue();
    }

    @Test
    void noSePuedeEliminarUnDocumentoDeUnaSolicitudEnProceso() {
        assertThat(documento.isEditable()).isFalse();
        assertThatThrownBy(() -> servicio.eliminar(30L, cliente)).isInstanceOf(ReglaNegocioException.class);
        verify(documentoRepository, never()).delete(any());
    }

    @Test
    void siLaSolicitudSigueAbiertaSePuedeEliminar() {
        solicitud.setEstado(EstadoSolicitud.ABIERTA);
        servicio.eliminar(30L, cliente);
        verify(documentoRepository).delete(documento);
    }

    @Test
    void otroClienteNoPuedeEliminar() {
        solicitud.setEstado(EstadoSolicitud.ABIERTA);
        assertThatThrownBy(() -> servicio.eliminar(30L, otroCliente)).isInstanceOf(ResponseStatusException.class);
        verify(documentoRepository, never()).delete(any());
    }

    @Test
    void seRechazaUnExe() throws Exception {
        MockMultipartFile exe = new MockMultipartFile("archivo", "programa.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> servicio.subir(cliente, TipoDocumentoCliente.RUT, "Mi RUT", exe, null))
                .isInstanceOf(ReglaNegocioException.class);
        verify(documentoRepository, never()).save(any());
        Path carpeta = uploads.resolve("documentos");
        assertThat(!Files.exists(carpeta) || Files.list(carpeta).findAny().isEmpty()).isTrue();
    }

    @Test
    void subirUnPdfGuardaElArchivoYCalculaLaCategoria() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "certificado.pdf", "application/pdf", "%PDF-1.4".getBytes());

        DocumentoCliente nuevo = servicio.subir(cliente, TipoDocumentoCliente.CERTIFICADO, "Certificado de ingresos", pdf, null);

        assertThat(nuevo.getCategoria()).isEqualTo(CategoriaDocumento.TRIBUTARIO);
        assertThat(nuevo.getNombreOriginal()).isEqualTo("certificado.pdf");
        assertThat(nuevo.getArchivo()).endsWith(".pdf").isNotEqualTo("certificado.pdf");
        assertThat(Files.exists(uploads.resolve("documentos").resolve(nuevo.getArchivo()))).isTrue();
    }
}
