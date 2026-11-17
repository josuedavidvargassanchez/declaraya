package com.example.declaraya.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Periodo en que un contador publico fue mentor de un practicante. Sin fechaFin, sigue vigente.
 * Usuario.mentor sigue siendo la fuente de verdad; esta tabla guarda la historia (no se edita ni se borra).
 */
@Entity
@Table(name = "asignaciones_mentoria")
public class AsignacionMentoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "practicante_id", nullable = false)
    private Usuario practicante;

    @ManyToOne
    @JoinColumn(name = "mentor_id", nullable = false)
    private Usuario mentor;

    // null en las asignaciones anteriores al flujo de solicitudes
    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudMentoria solicitud;

    // null solo en las asignaciones migradas, de las que no se sabe quien las hizo
    @ManyToOne
    @JoinColumn(name = "asignada_por_id")
    private Usuario asignadaPor;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    @Column(length = 500)
    private String observacion;

    public AsignacionMentoria() {
    }

    public AsignacionMentoria(Usuario practicante, Usuario mentor, SolicitudMentoria solicitud, Usuario asignadaPor,
                              LocalDateTime fechaInicio, String observacion) {
        this.practicante = practicante;
        this.mentor = mentor;
        this.solicitud = solicitud;
        this.asignadaPor = asignadaPor;
        this.fechaInicio = fechaInicio;
        this.observacion = observacion;
    }

    public boolean isVigente() {
        return fechaFin == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getPracticante() {
        return practicante;
    }

    public Usuario getMentor() {
        return mentor;
    }

    public SolicitudMentoria getSolicitud() {
        return solicitud;
    }

    public Usuario getAsignadaPor() {
        return asignadaPor;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getObservacion() {
        return observacion;
    }
}
