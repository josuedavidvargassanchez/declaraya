package com.example.declaraya.enums;

public enum Rol {
    PERSONA("Persona natural"),
    CONTADOR_PUBLICO("Contador público"),
    CONTADOR_PRACTICANTE("Contador practicante"),
    ADMIN("Administrador");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
