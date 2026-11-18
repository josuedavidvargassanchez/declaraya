package com.example.declaraya.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// los limites siguen las columnas de solicitudes_declaracion
public class NuevaSolicitudDTO {

    @NotBlank(message = "Selecciona el tipo de trámite")
    @Size(max = 255, message = "El tipo de trámite no puede superar 255 caracteres")
    private String titulo;

    @NotNull(message = "Selecciona el año gravable")
    private Integer anioGravable = 2025;

    @NotBlank(message = "Selecciona la urgencia")
    @Size(max = 20, message = "La urgencia no puede superar 20 caracteres")
    private String urgencia = "Normal";

    // DECIMAL(15,2)
    @PositiveOrZero(message = "El presupuesto no puede ser negativo")
    @DecimalMax(value = "100000000", message = "El presupuesto no puede superar $100.000.000")
    @Digits(integer = 13, fraction = 2, message = "El presupuesto admite máximo 2 decimales")
    private BigDecimal presupuestoMaximo;

    // se guardan unidas por coma en una columna de 500 caracteres (solo opciones del catalogo)
    @Size(max = 20, message = "Selecciona menos situaciones")
    private List<String> situaciones = new ArrayList<>();

    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    private String descripcion;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getAnioGravable() {
        return anioGravable;
    }

    public void setAnioGravable(Integer anioGravable) {
        this.anioGravable = anioGravable;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }

    public BigDecimal getPresupuestoMaximo() {
        return presupuestoMaximo;
    }

    public void setPresupuestoMaximo(BigDecimal presupuestoMaximo) {
        this.presupuestoMaximo = presupuestoMaximo;
    }

    public List<String> getSituaciones() {
        return situaciones;
    }

    public void setSituaciones(List<String> situaciones) {
        this.situaciones = situaciones;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
