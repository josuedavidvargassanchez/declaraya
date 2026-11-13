package com.example.declaraya.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(length = 200)
    private String enlace;

    private boolean leida = false;

    private LocalDateTime fecha = LocalDateTime.now();

    public Notificacion() {
    }

    public Notificacion(Usuario usuario, String mensaje, String enlace) {
        this.usuario = usuario;
        this.mensaje = mensaje;
        this.enlace = enlace;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getEnlace() {
        return enlace;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getFechaCorta() {
        return fecha == null ? "" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
