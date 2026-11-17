package com.example.declaraya.enums;

public enum EstadoEntrega {
    EN_REVISION_MENTOR("En revisión del mentor"),
    CORRECCION_SOLICITADA("Corrección solicitada"),
    ENVIADA("Enviada al cliente"),
    RECIBIDA("Recibida"),
    CON_INCONVENIENTE("Con inconveniente");

    private final String etiqueta;

    EstadoEntrega(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    // el cliente solo ve lo que ya le fue enviado (no los borradores que revisa el mentor)
    public boolean isVisibleParaCliente() {
        return this == ENVIADA || this == RECIBIDA || this == CON_INCONVENIENTE;
    }
}
