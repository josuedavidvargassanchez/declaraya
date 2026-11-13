package com.example.declaraya.models;

import com.example.declaraya.enums.EstadoSupervision;
import jakarta.persistence.*;

@Entity
@Table(name = "supervisiones")
public class Supervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "practicante_id", nullable = false)
    private Usuario practicante;

    @ManyToOne
    @JoinColumn(name = "mentor_id", nullable = false)
    private Usuario mentor;

    @OneToOne
    @JoinColumn(name = "acuerdo_id", nullable = false, unique = true)
    private Acuerdo acuerdo;

    @Enumerated(EnumType.STRING)
    private EstadoSupervision estado = EstadoSupervision.PENDIENTE_APROBACION;

    public Supervision() {
    }

    public Supervision(Usuario practicante, Usuario mentor, Acuerdo acuerdo) {
        this.practicante = practicante;
        this.mentor = mentor;
        this.acuerdo = acuerdo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getPracticante() {
        return practicante;
    }

    public void setPracticante(Usuario practicante) {
        this.practicante = practicante;
    }

    public Usuario getMentor() {
        return mentor;
    }

    public void setMentor(Usuario mentor) {
        this.mentor = mentor;
    }

    public Acuerdo getAcuerdo() {
        return acuerdo;
    }

    public void setAcuerdo(Acuerdo acuerdo) {
        this.acuerdo = acuerdo;
    }

    public EstadoSupervision getEstado() {
        return estado;
    }

    public void setEstado(EstadoSupervision estado) {
        this.estado = estado;
    }
}
