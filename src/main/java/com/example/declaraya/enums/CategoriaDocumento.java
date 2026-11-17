package com.example.declaraya.enums;

public enum CategoriaDocumento {
    PERSONAL("Personal"),
    TRIBUTARIO("Tributario");

    private final String etiqueta;

    CategoriaDocumento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
