package com.example.declaraya.enums;

public enum AccionDisputa {
    APERTURA("Apertura"),
    SOLICITUD_INFO_CLIENTE("Solicitud de información al cliente"),
    SOLICITUD_INFO_CONTADOR("Solicitud de información al contador"),
    MENSAJE_CLIENTE("Mensaje al cliente"),
    MENSAJE_CONTADOR("Mensaje al contador"),
    RESOLUCION("Resolución");

    private final String etiqueta;

    AccionDisputa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
