package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.TipoActividad;

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

// historial del acuerdo; solo se crea desde ActividadAcuerdoService y no se edita ni se borra
@Entity
@Table(name = "actividades_acuerdo")
public class ActividadAcuerdo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "acuerdo_id", nullable = false)
    private Acuerdo acuerdo;

    // null cuando la accion la hace el sistema (por ejemplo, un recordatorio)
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private TipoActividad tipo;

    @Column(length = 500)
    private String descripcion;

    public ActividadAcuerdo() {
    }

    public ActividadAcuerdo(Acuerdo acuerdo, Usuario actor, TipoActividad tipo, String descripcion) {
        this.acuerdo = acuerdo;
        this.actor = actor;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public Acuerdo getAcuerdo() {
        return acuerdo;
    }

    public Usuario getActor() {
        return actor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public TipoActividad getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
