package com.example.declaraya.models;

import java.time.LocalDateTime;

import com.example.declaraya.enums.CategoriaDocumento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.TipoDocumentoCliente;

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

/**
 * Documento que guarda un cliente (PERSONA). La relacion con una solicitud vive solo aqui,
 * asi "Mis documentos" y el detalle de la solicitud siempre muestran lo mismo.
 */
@Entity
@Table(name = "documentos_cliente")
public class DocumentoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    // descripcion que escribe el cliente
    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private TipoDocumentoCliente tipo;

    // se calcula a partir del tipo (ver setTipo)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private CategoriaDocumento categoria;

    @Column(nullable = false)
    private String nombreOriginal;

    // nombre UUID en uploads/documentos; nunca se muestra en las vistas
    @Column(nullable = false)
    private String archivo;

    private LocalDateTime fechaCarga = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudDeclaracion solicitud;

    // requisito del catalogo que cumple dentro de su solicitud (null si no aplica)
    @Column(length = 150)
    private String requisito;

    public String getRequisito() {
        return requisito;
    }

    public void setRequisito(String requisito) {
        this.requisito = requisito;
    }

    /**
     * Se puede eliminar, reemplazar o (des)vincular solo si no tiene solicitud o si la solicitud
     * sigue ABIERTA. Si ya esta EN_PROCESO o CERRADA, el documento queda en uso por el acuerdo.
     */
    public boolean isEditable() {
        return solicitud == null || solicitud.getEstado() == EstadoSolicitud.ABIERTA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getPropietario() {
        return propietario;
    }

    public void setPropietario(Usuario propietario) {
        this.propietario = propietario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoDocumentoCliente getTipo() {
        return tipo;
    }

    public void setTipo(TipoDocumentoCliente tipo) {
        this.tipo = tipo;
        this.categoria = tipo == null ? null : tipo.getCategoria();
    }

    public CategoriaDocumento getCategoria() {
        return categoria;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public SolicitudDeclaracion getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudDeclaracion solicitud) {
        this.solicitud = solicitud;
    }
}
