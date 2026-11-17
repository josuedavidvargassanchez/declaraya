package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.EstadoRequerimiento;

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

// documento adicional que el contador (o su mentor) le pide al cliente durante el acuerdo
@Entity
@Table(name = "requerimientos_documento")
public class RequerimientoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "acuerdo_id", nullable = false)
    private Acuerdo acuerdo;

    @ManyToOne
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private EstadoRequerimiento estado = EstadoRequerimiento.PENDIENTE;

    // documento con el que el cliente cumplio el requerimiento
    @ManyToOne
    @JoinColumn(name = "documento_id")
    private DocumentoCliente documento;

    private LocalDateTime fechaEntrega;

    public RequerimientoDocumento() {
    }

    public RequerimientoDocumento(Acuerdo acuerdo, Usuario solicitadoPor, String nombre, String descripcion) {
        this.acuerdo = acuerdo;
        this.solicitadoPor = solicitadoPor;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public boolean isPendiente() {
        return estado == EstadoRequerimiento.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Acuerdo getAcuerdo() {
        return acuerdo;
    }

    public Usuario getSolicitadoPor() {
        return solicitadoPor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoRequerimiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoRequerimiento estado) {
        this.estado = estado;
    }

    public DocumentoCliente getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoCliente documento) {
        this.documento = documento;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }
}
