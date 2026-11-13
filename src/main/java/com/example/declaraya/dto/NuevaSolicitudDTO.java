package com.example.declaraya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NuevaSolicitudDTO {

    @NotBlank(message = "Selecciona el tipo de trámite")
    private String titulo;

    @NotNull(message = "Selecciona el año gravable")
    private Integer anioGravable = 2025;

    @NotBlank(message = "Selecciona la urgencia")
    private String urgencia = "Normal";

    @PositiveOrZero(message = "El presupuesto no puede ser negativo")
    private BigDecimal presupuestoMaximo;

    private List<String> situaciones = new ArrayList<>();

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
