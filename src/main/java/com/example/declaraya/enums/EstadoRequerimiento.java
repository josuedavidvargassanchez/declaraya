package com.example.declaraya.enums;

public enum EstadoRequerimiento {
    PENDIENTE("Pendiente"),
    ENTREGADO("Entregado");

    private final String etiqueta;

    EstadoRequerimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
