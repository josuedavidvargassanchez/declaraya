package com.example.declaraya.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NuevaOfertaDTO {

    public static final String MENSAJE_VALOR = "El valor propuesto debe estar entre $1 y $100.000.000, con máximo 2 decimales";

    // la columna ofertas.mensaje admite 500 caracteres
    @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
    private String mensaje;

    @NotNull(message = MENSAJE_VALOR)
    @DecimalMin(value = "1", message = MENSAJE_VALOR)
    @DecimalMax(value = "100000000", message = MENSAJE_VALOR)
    @Digits(integer = 13, fraction = 2, message = MENSAJE_VALOR)
    private BigDecimal valorPropuesto;

    // obligatoria: futura y como maximo a 120 dias (lo valida OfertaService)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEntregaEstimada;

    public LocalDate getFechaEntregaEstimada() {
        return fechaEntregaEstimada;
    }

    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) {
        this.fechaEntregaEstimada = fechaEntregaEstimada;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public BigDecimal getValorPropuesto() {
        return valorPropuesto;
    }

    public void setValorPropuesto(BigDecimal valorPropuesto) {
        this.valorPropuesto = valorPropuesto;
    }
}
