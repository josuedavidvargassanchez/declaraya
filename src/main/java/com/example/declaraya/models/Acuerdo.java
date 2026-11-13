package com.example.declaraya.models;

import com.example.declaraya.enums.EstadoAcuerdo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acuerdos")
public class Acuerdo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "oferta_id", nullable = false, unique = true)
    private Oferta oferta;

    // 10% del valor propuesto, calculado al crear el acuerdo
    private BigDecimal comisionPlataforma;

    @Enumerated(EnumType.STRING)
    private EstadoAcuerdo estado = EstadoAcuerdo.ACTIVO;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Acuerdo() {
    }

    public Acuerdo(Oferta oferta, BigDecimal comisionPlataforma) {
        this.oferta = oferta;
        this.comisionPlataforma = comisionPlataforma;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Oferta getOferta() {
        return oferta;
    }

    public void setOferta(Oferta oferta) {
        this.oferta = oferta;
    }

    public BigDecimal getComisionPlataforma() {
        return comisionPlataforma;
    }

    public void setComisionPlataforma(BigDecimal comisionPlataforma) {
        this.comisionPlataforma = comisionPlataforma;
    }

    public EstadoAcuerdo getEstado() {
        return estado;
    }

    public void setEstado(EstadoAcuerdo estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
