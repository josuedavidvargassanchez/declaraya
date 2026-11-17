package com.example.declaraya.enums;

// tipo de un documento que sube el cliente; la categoria (personal o tributario) se deriva del tipo
public enum TipoDocumentoCliente {
    IDENTIDAD("Documento de identidad", CategoriaDocumento.PERSONAL),
    RUT("RUT", CategoriaDocumento.PERSONAL),
    OTRO_PERSONAL("Otro documento personal", CategoriaDocumento.PERSONAL),
    CERTIFICADO("Certificado (ingresos, retenciones, etc.)", CategoriaDocumento.TRIBUTARIO),
    EXTRACTO("Extracto bancario", CategoriaDocumento.TRIBUTARIO),
    DECLARACION_ANTERIOR("Declaración anterior", CategoriaDocumento.TRIBUTARIO),
    OTRO_TRIBUTARIO("Otro documento tributario", CategoriaDocumento.TRIBUTARIO);

    private final String etiqueta;
    private final CategoriaDocumento categoria;

    TipoDocumentoCliente(String etiqueta, CategoriaDocumento categoria) {
        this.etiqueta = etiqueta;
        this.categoria = categoria;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public CategoriaDocumento getCategoria() {
        return categoria;
    }
}
