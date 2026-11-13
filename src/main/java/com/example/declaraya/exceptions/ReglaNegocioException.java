package com.example.declaraya.exceptions;

/** Se lanza cuando una accion incumple una regla de negocio de la plataforma. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
