package com.example.declaraya.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** A4: el valor de una oferta se valida antes de llegar a la base de datos. */
class NuevaOfertaDTOTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crear() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<ConstraintViolation<NuevaOfertaDTO>> validar(String valor, String mensaje) {
        NuevaOfertaDTO dto = new NuevaOfertaDTO();
        dto.setValorPropuesto(valor == null ? null : new BigDecimal(valor));
        dto.setMensaje(mensaje);
        return validador.validate(dto);
    }

    private static boolean fallaEnValor(String valor) {
        return validar(valor, "Hola").stream().anyMatch(v -> v.getPropertyPath().toString().equals("valorPropuesto"));
    }

    @Test
    void rechazaValorCeroNegativoOMayorQueElMaximo() {
        assertThat(fallaEnValor("0")).isTrue();
        assertThat(fallaEnValor("-5000")).isTrue();
        assertThat(fallaEnValor("100000000.01")).isTrue();
        assertThat(fallaEnValor("999999999999999")).isTrue();
        assertThat(fallaEnValor("1500.123")).isTrue();
        assertThat(fallaEnValor(null)).isTrue();
    }

    @Test
    void aceptaUnValorValido() {
        assertThat(validar("350000", "Puedo ayudarte").isEmpty()).isTrue();
        assertThat(validar("100000000", "Tope").isEmpty()).isTrue();
    }

    @Test
    void rechazaUnMensajeDeMasDe500Caracteres() {
        assertThat(validar("350000", "x".repeat(501))).isNotEmpty();
    }

    @Test
    void elPresupuestoDeLaSolicitudTieneTope() {
        NuevaSolicitudDTO dto = new NuevaSolicitudDTO();
        dto.setTitulo("Declaración de renta");
        dto.setPresupuestoMaximo(new BigDecimal("100000001"));
        assertThat(validador.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("presupuestoMaximo"));
        dto.setPresupuestoMaximo(new BigDecimal("300000"));
        dto.setDescripcion("x".repeat(1001));
        assertThat(validador.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("descripcion"));
    }
}
