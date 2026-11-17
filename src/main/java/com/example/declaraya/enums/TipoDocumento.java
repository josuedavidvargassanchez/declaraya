package com.example.declaraya.enums;

// tipo de documento de identidad del usuario (dato opcional del perfil)
public enum TipoDocumento {
    CC("Cédula de ciudadanía"),
    CE("Cédula de extranjería"),
    PASAPORTE("Pasaporte"),
    PPT("Permiso por protección temporal");

    private final String etiqueta;

    TipoDocumento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
