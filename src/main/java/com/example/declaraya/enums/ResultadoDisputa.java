package com.example.declaraya.enums;

public enum ResultadoDisputa {
    A_FAVOR_CLIENTE("A favor del cliente"),
    A_FAVOR_CONTADOR("A favor del contador"),
    SIN_SANCION("Cerrar sin sanción");

    private final String etiqueta;

    ResultadoDisputa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    // las decisiones a favor de una parte dejan la disputa RESUELTA; "sin sancion" la deja CERRADA
    public EstadoDisputa estadoFinal() {
        return this == SIN_SANCION ? EstadoDisputa.CERRADA : EstadoDisputa.RESUELTA;
    }
}
