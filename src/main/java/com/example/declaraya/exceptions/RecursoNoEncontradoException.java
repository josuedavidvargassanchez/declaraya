package com.example.declaraya.exceptions;

/**
 * Se lanza cuando un recurso (usuario, solicitud, oferta, acuerdo...) no existe.
 * Es manejada por {@link WebExceptionHandler}, que responde con un 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
