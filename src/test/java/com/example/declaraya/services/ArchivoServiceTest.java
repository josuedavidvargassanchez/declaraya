package com.example.declaraya.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.example.declaraya.repositories.ArchivoRepository;
import com.example.declaraya.repositories.UsuarioRepository;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.enums.Rol;

/** A5: ademas de la extension se revisan los primeros bytes del archivo. */
class ArchivoServiceTest {

    @TempDir
    Path uploads;

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
    private static final byte[] EXE = {0x4D, 0x5A, (byte) 0x90, 0x00};

    @Test
    void unPdfCuyoContenidoNoEmpiezaPorPorcentajePdfSeRechaza() throws IOException {
        ArchivoService servicio = new ArchivoService(mock(ArchivoRepository.class), uploads.toString());
        MockMultipartFile exeRenombrado = new MockMultipartFile("archivo", "factura.pdf", "application/pdf", EXE);

        assertThatThrownBy(() -> servicio.validar(exeRenombrado)).isInstanceOf(ArchivoService.ArchivoNoPermitidoException.class)
                .hasMessageContaining("PDF");
        assertThatThrownBy(() -> servicio.guardarEn(ArchivoService.CARPETA_DOCUMENTOS, exeRenombrado))
                .isInstanceOf(ArchivoService.ArchivoNoPermitidoException.class);
    }

    @Test
    void unPdfRealSeAcepta() throws IOException {
        ArchivoService servicio = new ArchivoService(mock(ArchivoRepository.class), uploads.toString());
        MockMultipartFile pdf = new MockMultipartFile("archivo", "rut.pdf", "application/pdf", "%PDF-1.7\n%...".getBytes());

        assertThatCode(() -> servicio.validar(pdf)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.guardarEn(ArchivoService.CARPETA_ENTREGAS, pdf)).doesNotThrowAnyException();
    }

    @Test
    void lasImagenesSeRevisanPorSuFirma() {
        assertThatCode(() -> ArchivoService.validarContenido(new MockMultipartFile("a", "foto.png", "image/png", PNG)))
                .doesNotThrowAnyException();
        assertThatCode(() -> ArchivoService.validarContenido(new MockMultipartFile("a", "foto.JPG", "image/jpeg", JPG)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ArchivoService.validarContenido(new MockMultipartFile("a", "foto.png", "image/png", JPG)))
                .isInstanceOf(ArchivoService.ArchivoNoPermitidoException.class);
        assertThatThrownBy(() -> ArchivoService.validarContenido(new MockMultipartFile("a", "vacio.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(ArchivoService.ArchivoNoPermitidoException.class);
    }

    @Test
    void laFotoDePerfilUsaLaMismaRevision() throws IOException {
        FotoPerfilService fotos = new FotoPerfilService(mock(UsuarioRepository.class), uploads.toString());
        Usuario u = new Usuario("Ana", "ana@example.com", "x", Rol.PERSONA);

        assertThatThrownBy(() -> fotos.guardar(u, new MockMultipartFile("foto", "yo.jpg", "image/jpeg", EXE)))
                .isInstanceOf(FotoPerfilService.FotoNoPermitidaException.class);
    }
}
