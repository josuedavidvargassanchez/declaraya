package com.example.declaraya.models;

import java.math.BigDecimal;

import com.example.declaraya.enums.Rol;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final BigDecimal  TARIFA_BASE_POR_DEFECTO = new BigDecimal("500000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // solo aplica para CONTADOR_PUBLICO y CONTADOR_PRACTICANTE
    private String tarjetaProfesional;

    // indica si el admin ya valido al contador contra la Junta Central de Contadores
    private boolean verificado = false;

    @ManyToOne 
    @JoinColumn(name = "mentor_id")
    private Usuario mentor; 

    private String fotoPerfil;

    @Column(precision = 14, scale = 2)
    private BigDecimal tarifaBase = TARIFA_BASE_POR_DEFECTO;

    public Usuario getMentor() {
        return mentor;
    }

    public void setMentor(Usuario mentor) {
        this.mentor = mentor;
    }

    public Usuario() {
    }

    public Usuario(String nombre, String email, String password, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getTarjetaProfesional() {
        return tarjetaProfesional;
    }

    public void setTarjetaProfesional(String tarjetaProfesional) {
        this.tarjetaProfesional = tarjetaProfesional;
    }

    public boolean isVerificado() {
        return verificado;
    }

    public void setVerificado(boolean verificado) {
        this.verificado = verificado;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public BigDecimal getTarifaBase() {
        return tarifaBase != null ? tarifaBase : TARIFA_BASE_POR_DEFECTO;
    }

    public void setTarifaBase(BigDecimal tarifaBase) {
        this.tarifaBase = tarifaBase;
    }
}
