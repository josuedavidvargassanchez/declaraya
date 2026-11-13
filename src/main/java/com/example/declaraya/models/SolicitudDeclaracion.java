package com.example.declaraya.models;

import com.example.declaraya.enums.EstadoSolicitud;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "solicitudes_declaracion")
public class SolicitudDeclaracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario persona;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    private Integer anioGravable;

    @Column(length = 20)
    private String urgencia = "Normal";

    @Column(precision = 15, scale = 2)
    private BigDecimal presupuestoMaximo;

    // situaciones del cliente separadas por coma (ej: "Empleado,Arriendos")
    @Column(length = 500)
    private String situaciones;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado = EstadoSolicitud.ABIERTA;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public SolicitudDeclaracion() {
    }

    public SolicitudDeclaracion(Usuario persona, String titulo, String descripcion) {
        this.persona = persona;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getPersona() {
        return persona;
    }

    public void setPersona(Usuario persona) {
        this.persona = persona;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
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

    public String getSituaciones() {
        return situaciones;
    }

    public void setSituaciones(String situaciones) {
        this.situaciones = situaciones;
    }

    /** Situaciones como lista, para mostrarlas en las vistas. */
    public List<String> getSituacionesLista() {
        if (situaciones == null || situaciones.isBlank()) {
            return List.of();
        }
        return Arrays.stream(situaciones.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    /** Fecha de creacion lista para mostrar (dd/MM/yyyy). */
    public String getFechaCorta() {
        return fechaCreacion == null ? "" : fechaCreacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
