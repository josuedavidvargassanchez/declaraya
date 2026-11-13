package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.EstadoDisputa;

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

@Entity 
@Table(name = "disputas") 
public class Disputa {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne 
    @JoinColumn( name = "acuerdo_id", nullable = false) 
    private Acuerdo acuerdo;

    @ManyToOne 
    @JoinColumn( name = "abierta_por_id", nullable = false) 
    private Usuario abiertaPor;

    @Column(length = 500)
    private String motivo;
    
    @Enumerated(EnumType.STRING)
    private EstadoDisputa estado = EstadoDisputa.ABIERTA;
    
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Acuerdo getAcuerdo() {
        return acuerdo;
    }

    public void setAcuerdo(Acuerdo acuerdo) {
        this.acuerdo = acuerdo;
    }

    public Usuario getAbiertaPor() {
        return abiertaPor;
    }

    public void setAbiertaPor(Usuario abiertaPor) {
        this.abiertaPor = abiertaPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public EstadoDisputa getEstado() {
        return estado;
    }

    public void setEstado(EstadoDisputa estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    

}
