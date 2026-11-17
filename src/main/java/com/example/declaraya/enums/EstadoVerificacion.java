package com.example.declaraya.enums;

// estado de la revision de la tarjeta profesional de un contador (null para PERSONA y ADMIN)
public enum EstadoVerificacion {
    PENDIENTE("Pendiente"),
    CORRECCION_SOLICITADA("Corrección solicitada"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String etiqueta;

    EstadoVerificacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
