package com.example.declaraya.enums;

public enum TipoActividad {
    ACUERDO_CREADO("Acuerdo creado"),
    MENTOR_APROBO("Aprobación del mentor"),
    CAMBIO_ETAPA("Cambio de etapa"),
    DOCUMENTO_SUBIDO("Documento subido"),
    DOCUMENTO_SOLICITADO("Documento solicitado"),
    DOCUMENTO_ENTREGADO("Documento solicitado entregado"),
    ENTREGA_ENVIADA("Trabajo entregado"),
    ENTREGA_APROBADA("Entrega aprobada por el mentor"),
    CORRECCION_MENTOR("Corrección pedida por el mentor"),
    RECEPCION_CONFIRMADA("Recepción confirmada"),
    INCONVENIENTE_REPORTADO("Inconveniente reportado"),
    DISPUTA_ABIERTA("Disputa abierta"),
    DISPUTA_RESUELTA("Disputa resuelta"),
    RECORDATORIO("Recordatorio de entrega"),
    CALIFICACION("Calificación");

    private final String etiqueta;

    TipoActividad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
