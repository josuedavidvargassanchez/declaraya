package com.example.declaraya.enums;

// NUEVA: el practicante no tiene mentor; CAMBIO: pide reemplazar al mentor actual
public enum TipoSolicitudMentoria {
    NUEVA("Nuevo mentor"),
    CAMBIO("Cambio de mentor");

    private final String etiqueta;

    TipoSolicitudMentoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
