package com.example.declaraya.models;

import com.example.declaraya.enums.EstadoOferta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ofertas")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudDeclaracion solicitud;

    @ManyToOne
    @JoinColumn(name = "contador_id", nullable = false)
    private Usuario contador;

    @Column(length = 500)
    private String mensaje;

    private BigDecimal valorPropuesto;

    @Enumerated(EnumType.STRING)
    private EstadoOferta estado = EstadoOferta.PENDIENTE;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Oferta() {
    }

    public Oferta(SolicitudDeclaracion solicitud, Usuario contador, String mensaje, BigDecimal valorPropuesto) {
        this.solicitud = solicitud;
        this.contador = contador;
        this.mensaje = mensaje;
        this.valorPropuesto = valorPropuesto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SolicitudDeclaracion getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudDeclaracion solicitud) {
        this.solicitud = solicitud;
    }

    public Usuario getContador() {
        return contador;
    }

    public void setContador(Usuario contador) {
        this.contador = contador;
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

    public EstadoOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoOferta estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
