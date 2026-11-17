package com.example.declaraya.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// una sola calificacion por acuerdo (restriccion unica en acuerdo_id); el admin puede ocultarla, no borrarla
@Entity
@Table(name = "calificaciones", uniqueConstraints = @UniqueConstraint(name = "uk_calificacion_acuerdo", columnNames = "acuerdo_id"))
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "acuerdo_id", nullable = false, unique = true)
    private Acuerdo acuerdo;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Usuario persona;

    @ManyToOne
    @JoinColumn(name = "contador_id", nullable = false)
    private Usuario contador;

    @Column(nullable = false)
    private int puntaje;

    @Column(length = 500)
    private String comentario;

    private LocalDateTime fecha = LocalDateTime.now();

    @Column(columnDefinition = "BIT(1) NOT NULL DEFAULT 0")
    private boolean oculta = false;

    @Column(length = 500)
    private String motivoOcultamiento;

    public Calificacion() {
    }

    public Calificacion(Acuerdo acuerdo, Usuario persona, Usuario contador, int puntaje, String comentario) {
        this.acuerdo = acuerdo;
        this.persona = persona;
        this.contador = contador;
        this.puntaje = puntaje;
        this.comentario = comentario;
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

    public Usuario getPersona() {
        return persona;
    }

    public Usuario getContador() {
        return contador;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isOculta() {
        return oculta;
    }

    public void setOculta(boolean oculta) {
        this.oculta = oculta;
    }

    public String getMotivoOcultamiento() {
        return motivoOcultamiento;
    }

    public void setMotivoOcultamiento(String motivoOcultamiento) {
        this.motivoOcultamiento = motivoOcultamiento;
    }
}
