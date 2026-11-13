package com.example.declaraya.models;

import jakarta.persistence.*;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "acuerdo_id", nullable = false, unique = true)
    private Acuerdo acuerdo;

    public Chat() {
    }

    public Chat(Acuerdo acuerdo) {
        this.acuerdo = acuerdo;
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

    public void setAcuerdo(Acuerdo acuerdo) {
        this.acuerdo = acuerdo;
    }
}
