package com.example.declaraya.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Convierte las excepciones de negocio en la pagina error.html,
 * con el codigo HTTP que corresponde.
 */
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarReglaNegocio(ReglaNegocioException ex, Model model) {
        return paginaError(model, HttpStatus.BAD_REQUEST, "Solicitud no valida", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, Model model) {
        return paginaError(model, HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    private String paginaError(Model model, HttpStatus estado, String titulo, String detalle) {
        model.addAttribute("status", estado.value());
        model.addAttribute("error", titulo);
        model.addAttribute("message", detalle);
        return "error";
    }
}
