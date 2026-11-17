package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.EstadoSolicitudMentoria;
import com.example.declaraya.enums.TipoSolicitudMentoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// el practicante pide un mentor (o un cambio) y el administrador lo decide; no se edita ni se borra
@Entity
@Table(name = "solicitudes_mentoria")
public class SolicitudMentoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "practicante_id", nullable = false)
    private Usuario practicante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private TipoSolicitudMentoria tipo;

    // un tramite de CatalogoRequisitos.TRAMITES; opcional solo en los cambios
    private String especialidad;

    @Column(nullable = false, length = 500)
    private String mensaje;

    // obligatorio cuando el tipo es CAMBIO
    @Column(length = 500)
    private String motivoCambio;

    @ManyToOne
    @JoinColumn(name = "mentor_anterior_id")
    private Usuario mentorAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private EstadoSolicitudMentoria estado = EstadoSolicitudMentoria.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "mentor_asignado_id")
    private Usuario mentorAsignado;

    // administrador que aprobo o rechazo (null si sigue pendiente o la cancelo el practicante)
    @ManyToOne
    @JoinColumn(name = "revisada_por_id")
    private Usuario revisadaPor;

    // fecha en que se aprobo, rechazo o cancelo
    private LocalDateTime fechaRevision;

    @Column(length = 500)
    private String motivoRechazo;

    public SolicitudMentoria() {
    }

    public SolicitudMentoria(Usuario practicante, TipoSolicitudMentoria tipo, String especialidad, String mensaje) {
        this.practicante = practicante;
        this.tipo = tipo;
        this.especialidad = especialidad;
        this.mensaje = mensaje;
    }

    public boolean isPendiente() {
        return estado == EstadoSolicitudMentoria.PENDIENTE;
    }

    public boolean isCambio() {
        return tipo == TipoSolicitudMentoria.CAMBIO;
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

    public TipoSolicitudMentoria getTipo() {
        return tipo;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getMotivoCambio() {
        return motivoCambio;
    }

    public void setMotivoCambio(String motivoCambio) {
        this.motivoCambio = motivoCambio;
    }

    public Usuario getMentorAnterior() {
        return mentorAnterior;
    }

    public void setMentorAnterior(Usuario mentorAnterior) {
        this.mentorAnterior = mentorAnterior;
    }

    public EstadoSolicitudMentoria getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitudMentoria estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public Usuario getMentorAsignado() {
        return mentorAsignado;
    }

    public void setMentorAsignado(Usuario mentorAsignado) {
        this.mentorAsignado = mentorAsignado;
    }

    public Usuario getRevisadaPor() {
        return revisadaPor;
    }

    public void setRevisadaPor(Usuario revisadaPor) {
        this.revisadaPor = revisadaPor;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
