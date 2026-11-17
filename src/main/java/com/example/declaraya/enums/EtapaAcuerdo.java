package com.example.declaraya.enums;

// etapa de trabajo del acuerdo; complementa a EstadoAcuerdo (ACTIVO/FINALIZADO), no lo reemplaza
public enum EtapaAcuerdo {
    APROBACION_MENTOR("Aprobación del mentor", true),
    DOCUMENTACION("Documentación", false),
    PREPARACION("Preparación", false),
    REVISION_MENTOR("Revisión del mentor", true),
    ENTREGADO("Entregado", false),
    FINALIZADO("Finalizado", false);

    private final String etiqueta;
    private final boolean soloPracticante;

    EtapaAcuerdo(String etiqueta, boolean soloPracticante) {
        this.etiqueta = etiqueta;
        this.soloPracticante = soloPracticante;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    // las etapas del mentor no aplican cuando atiende un contador publico
    public boolean isSoloPracticante() {
        return soloPracticante;
    }
}
