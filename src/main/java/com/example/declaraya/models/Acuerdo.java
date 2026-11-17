package com.example.declaraya.models;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EtapaAcuerdo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    // etapa de trabajo (complementa a estado); solo cambia desde FlujoAcuerdoService
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private EtapaAcuerdo etapa;

    // evita repetir el recordatorio de "faltan 2 dias o menos"
    @Column(columnDefinition = "BIT(1) NOT NULL DEFAULT 0")
    private boolean recordatorioEnviado = false;

    public Acuerdo() {
    }

    /** Fecha estimada de entrega pactada en la oferta aceptada (null en acuerdos antiguos). */
    public LocalDate getFechaEntregaEstimada() {
        return oferta == null ? null : oferta.getFechaEntregaEstimada();
    }

    /** Dias que faltan para la fecha estimada (negativo si ya paso); null si no hay fecha. */
    public Long getDiasRestantes() {
        LocalDate fecha = getFechaEntregaEstimada();
        return fecha == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), fecha);
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

    public EtapaAcuerdo getEtapa() {
        return etapa;
    }

    public void setEtapa(EtapaAcuerdo etapa) {
        this.etapa = etapa;
    }

    public boolean isRecordatorioEnviado() {
        return recordatorioEnviado;
    }

    public void setRecordatorioEnviado(boolean recordatorioEnviado) {
        this.recordatorioEnviado = recordatorioEnviado;
    }
}
