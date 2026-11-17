package com.example.declaraya.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.declaraya.enums.TipoDocumento;
import com.example.declaraya.models.Usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que el usuario puede editar en /perfil. A proposito NO incluye rol, correo, tarjeta
 * profesional ni nada de la verificacion: aunque se envien esos parametros, no hay donde enlazarlos.
 * UsuarioService.actualizarPerfil aplica ademas solo los campos permitidos para cada rol.
 */
public class PerfilDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    private TipoDocumento tipoDocumento;

    @Pattern(regexp = "^$|^[A-Za-z0-9-]{4,20}$",
            message = "El número de documento debe tener entre 4 y 20 letras, números o guiones")
    private String numeroDocumento;

    @Pattern(regexp = "^$|^\\+?[0-9 ]{7,20}$",
            message = "El teléfono debe tener entre 7 y 20 dígitos (puede empezar con +)")
    private String telefono;

    @Pattern(regexp = "^$|^[\\p{L} .,'-]{2,60}$",
            message = "La ciudad solo puede tener letras, espacios y signos básicos (máximo 60)")
    private String ciudad;

    @Size(max = 500, message = "La descripción profesional no puede superar 500 caracteres")
    private String descripcionProfesional;

    @DecimalMin(value = "0", message = "La tarifa base no puede ser negativa")
    @DecimalMax(value = "999999999999", message = "La tarifa base es demasiado alta")
    private BigDecimal tarifaBase;

    // tramites del catalogo; UsuarioService descarta los que no existan
    private List<String> especialidades = new ArrayList<>();

    @Min(value = 0, message = "Los años de experiencia van de 0 a 60")
    @Max(value = 60, message = "Los años de experiencia van de 0 a 60")
    private Integer aniosExperiencia;

    public List<String> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(List<String> especialidades) {
        this.especialidades = especialidades;
    }

    public Integer getAniosExperiencia() {
        return aniosExperiencia;
    }

    public void setAniosExperiencia(Integer aniosExperiencia) {
        this.aniosExperiencia = aniosExperiencia;
    }

    public static PerfilDTO desde(Usuario u) {
        PerfilDTO dto = new PerfilDTO();
        dto.setNombre(u.getNombre());
        dto.setTipoDocumento(u.getTipoDocumento());
        dto.setNumeroDocumento(u.getNumeroDocumento());
        dto.setTelefono(u.getTelefono());
        dto.setCiudad(u.getCiudad());
        dto.setDescripcionProfesional(u.getDescripcionProfesional());
        dto.setTarifaBase(u.getTarifaBase());
        dto.setEspecialidades(new ArrayList<>(u.getEspecialidadesLista()));
        dto.setAniosExperiencia(u.getAniosExperiencia());
        return dto;
    }

    // tipo y numero de documento van juntos: los dos o ninguno
    @AssertTrue(message = "Indica el tipo y el número de documento juntos")
    public boolean isDocumentoCompleto() {
        boolean hayNumero = numeroDocumento != null && !numeroDocumento.isBlank();
        return hayNumero == (tipoDocumento != null);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public BigDecimal getTarifaBase() {
        return tarifaBase;
    }

    public void setTarifaBase(BigDecimal tarifaBase) {
        this.tarifaBase = tarifaBase;
    }
}
