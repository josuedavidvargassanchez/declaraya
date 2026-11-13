package com.example.declaraya.models;

import jakarta.persistence.*;

// dato publico de la DIAN (recaudo, UVT, declarantes) para el analisis en Power BI
@Entity
@Table(name = "indicadores_dian")
public class IndicadorDian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer anio;

    @Column(length = 40)
    private String periodo;

    @Column(length = 60)
    private String categoria;

    @Column(length = 160)
    private String concepto;

    private Long valor;

    @Column(length = 30)
    private String unidad;

    @Column(length = 200)
    private String fuente;

    @Column(length = 300)
    private String nota;

    public IndicadorDian() {
    }

    public IndicadorDian(Integer anio, String periodo, String categoria, String concepto,
                         Long valor, String unidad, String fuente, String nota) {
        this.anio = anio;
        this.periodo = periodo;
        this.categoria = categoria;
        this.concepto = concepto;
        this.valor = valor;
        this.unidad = unidad;
        this.fuente = fuente;
        this.nota = nota;
    }

    public Long getId() {
        return id;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getPeriodo() {
        return periodo;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getConcepto() {
        return concepto;
    }

    public Long getValor() {
        return valor;
    }

    public String getUnidad() {
        return unidad;
    }

    public String getFuente() {
        return fuente;
    }

    public String getNota() {
        return nota;
    }
}
