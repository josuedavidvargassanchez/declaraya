package com.example.declaraya.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
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

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

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

    // base de datos caida o sin conexiones: se registra el detalle en el log y el usuario solo ve un aviso
    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String manejarBaseNoDisponible(Exception ex, Model model) {
        log.error("Base de datos no disponible: {}", ex.getMessage());
        return paginaError(model, HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
                "No pudimos conectarnos con la base de datos. Intenta de nuevo en unos minutos.");
    }

    private String paginaError(Model model, HttpStatus estado, String titulo, String detalle) {
        model.addAttribute("status", estado.value());
        model.addAttribute("error", titulo);
        model.addAttribute("message", detalle);
        return "error";
    }
}
