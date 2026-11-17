package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.AccionDisputa;
import com.example.declaraya.enums.EstadoDisputa;
import com.example.declaraya.enums.ResultadoDisputa;

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

// registro inmutable de cada accion sobre una disputa (quien, cuando, que cambio y por que)
@Entity
@Table(name = "historial_disputas")
public class HistorialDisputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "disputa_id", nullable = false)
    private Disputa disputa;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private AccionDisputa accion;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private EstadoDisputa estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private EstadoDisputa estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private ResultadoDisputa decision;

    @Column(length = 1000)
    private String detalle;

    public HistorialDisputa() {
    }

    public HistorialDisputa(Disputa disputa, Usuario actor, AccionDisputa accion,
                            EstadoDisputa estadoAnterior, EstadoDisputa estadoNuevo,
                            ResultadoDisputa decision, String detalle) {
        this.disputa = disputa;
        this.actor = actor;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.decision = decision;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Disputa getDisputa() {
        return disputa;
    }

    public void setDisputa(Disputa disputa) {
        this.disputa = disputa;
    }

    public Usuario getActor() {
        return actor;
    }

    public void setActor(Usuario actor) {
        this.actor = actor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public AccionDisputa getAccion() {
        return accion;
    }

    public void setAccion(AccionDisputa accion) {
        this.accion = accion;
    }

    public EstadoDisputa getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoDisputa estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoDisputa getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoDisputa estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public ResultadoDisputa getDecision() {
        return decision;
    }

    public void setDecision(ResultadoDisputa decision) {
        this.decision = decision;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
