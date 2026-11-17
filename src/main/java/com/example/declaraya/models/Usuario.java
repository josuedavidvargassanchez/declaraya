package com.example.declaraya.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoDocumento;
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

    // indica si el admin ya valido al contador contra la Junta Central de Contadores.
    // Para contadores es true solo cuando estadoVerificacion == APROBADO (lo sincroniza VerificacionService)
    private boolean verificado = false;

    @ManyToOne 
    @JoinColumn(name = "mentor_id")
    private Usuario mentor; 

    private String fotoPerfil;

    @Column(precision = 14, scale = 2)
    private BigDecimal tarifaBase = TARIFA_BASE_POR_DEFECTO;

    // ---- datos de perfil (todos opcionales; se editan en /perfil)

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private TipoDocumento tipoDocumento;

    @Column(length = 20)
    private String numeroDocumento;

    @Column(length = 20)
    private String telefono;

    @Column(length = 60)
    private String ciudad;

    // solo contadores
    @Column(length = 500)
    private String descripcionProfesional;

    // solo contadores: tramites del catalogo separados por "|"
    @Column(length = 1000)
    private String especialidades;

    // solo contadores: de 0 a 60
    private Integer aniosExperiencia;

    /** Especialidades como lista, para las vistas. */
    public java.util.List<String> getEspecialidadesLista() {
        if (especialidades == null || especialidades.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(especialidades.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(String especialidades) {
        this.especialidades = especialidades;
    }

    public Integer getAniosExperiencia() {
        return aniosExperiencia;
    }

    public void setAniosExperiencia(Integer aniosExperiencia) {
        this.aniosExperiencia = aniosExperiencia;
    }

    // ---- verificacion de contadores (todo queda en null para PERSONA y ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private EstadoVerificacion estadoVerificacion;

    private LocalDateTime fechaSolicitudVerificacion;

    // nombre con el que se guardo el archivo en uploads/verificaciones
    private String soporteTarjetaProfesional;

    private String soporteDocumentoIdentidad;

    @Column(length = 500)
    private String observacionVerificacion;

    @Column(length = 500)
    private String motivoRechazo;

    private LocalDateTime fechaRevisionVerificacion;

    @ManyToOne
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    public boolean isContador() {
        return rol == Rol.CONTADOR_PUBLICO || rol == Rol.CONTADOR_PRACTICANTE;
    }

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

    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public LocalDateTime getFechaSolicitudVerificacion() {
        return fechaSolicitudVerificacion;
    }

    public void setFechaSolicitudVerificacion(LocalDateTime fechaSolicitudVerificacion) {
        this.fechaSolicitudVerificacion = fechaSolicitudVerificacion;
    }

    public String getSoporteTarjetaProfesional() {
        return soporteTarjetaProfesional;
    }

    public void setSoporteTarjetaProfesional(String soporteTarjetaProfesional) {
        this.soporteTarjetaProfesional = soporteTarjetaProfesional;
    }

    public String getSoporteDocumentoIdentidad() {
        return soporteDocumentoIdentidad;
    }

    public void setSoporteDocumentoIdentidad(String soporteDocumentoIdentidad) {
        this.soporteDocumentoIdentidad = soporteDocumentoIdentidad;
    }

    public String getObservacionVerificacion() {
        return observacionVerificacion;
    }

    public void setObservacionVerificacion(String observacionVerificacion) {
        this.observacionVerificacion = observacionVerificacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public LocalDateTime getFechaRevisionVerificacion() {
        return fechaRevisionVerificacion;
    }

    public void setFechaRevisionVerificacion(LocalDateTime fechaRevisionVerificacion) {
        this.fechaRevisionVerificacion = fechaRevisionVerificacion;
    }

    public Usuario getRevisadoPor() {
        return revisadoPor;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDescripcionProfesional() {
        return descripcionProfesional;
    }

    public void setDescripcionProfesional(String descripcionProfesional) {
        this.descripcionProfesional = descripcionProfesional;
    }

    public void setRevisadoPor(Usuario revisadoPor) {
        this.revisadoPor = revisadoPor;
    }
}
