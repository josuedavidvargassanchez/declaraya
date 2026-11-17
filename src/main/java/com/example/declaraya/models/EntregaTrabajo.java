package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.EstadoEntrega;

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

// trabajo que entrega el contador; el archivo vive en uploads/entregas con nombre UUID
@Entity
@Table(name = "entregas_trabajo")
public class EntregaTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "acuerdo_id", nullable = false)
    private Acuerdo acuerdo;

    @ManyToOne
    @JoinColumn(name = "contador_id", nullable = false)
    private Usuario contador;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    // nombre UUID en disco; nunca se muestra en las vistas
    @Column(nullable = false)
    private String archivo;

    @Column(nullable = false)
    private String nombreOriginal;

    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private EstadoEntrega estado;

    @Column(length = 500)
    private String observacionMentor;

    @Column(length = 500)
    private String observacionCliente;

    private LocalDateTime fechaRespuesta;

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

    public Usuario getContador() {
        return contador;
    }

    public void setContador(Usuario contador) {
        this.contador = contador;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public void setEstado(EstadoEntrega estado) {
        this.estado = estado;
    }

    public String getObservacionMentor() {
        return observacionMentor;
    }

    public void setObservacionMentor(String observacionMentor) {
        this.observacionMentor = observacionMentor;
    }

    public String getObservacionCliente() {
        return observacionCliente;
    }

    public void setObservacionCliente(String observacionCliente) {
        this.observacionCliente = observacionCliente;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }
}
