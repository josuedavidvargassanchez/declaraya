package com.example.declaraya.exceptions;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;

/** Sin base de datos: la pagina de error responde 503 con un mensaje para el usuario, sin detalles tecnicos. */
class WebExceptionHandlerTest {

    @Controller
    static class ControladorSinBase {

        @GetMapping("/sin-base")
        String sinBase() {
            throw new DataAccessResourceFailureException("Communications link failure (detalle técnico)");
        }

        @GetMapping("/sin-transaccion")
        String sinTransaccion() {
            throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorSinBase())
                .setControllerAdvice(new WebExceptionHandler())
                .build();
    }

    @Test
    void unaFallaDeLaBaseDevuelveLaPaginaDeErrorCon503() throws Exception {
        mvc.perform(get("/sin-base"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 503))
                .andExpect(model().attribute("error", "Servicio no disponible"))
                .andExpect(model().attribute("message",
                        "No pudimos conectarnos con la base de datos. Intenta de nuevo en unos minutos."))
                .andExpect(model().attribute("message", not(containsString("Communications link failure"))));
    }

    @Test
    void sinPoderAbrirLaTransaccionTambienDevuelve503() throws Exception {
        mvc.perform(get("/sin-transaccion"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "Servicio no disponible"));
    }
}
