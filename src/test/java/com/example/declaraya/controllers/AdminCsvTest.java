package com.example.declaraya.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A3: las celdas que Excel o Power BI interpretarian como formula se neutralizan con un apostrofo. */
class AdminCsvTest {

    @Test
    void unaFormulaSaleConApostrofo() {
        assertThat(AdminController.csv("=SUMA(A1)")).isEqualTo("'=SUMA(A1)");
        assertThat(AdminController.csv("+57 300")).isEqualTo("'+57 300");
        assertThat(AdminController.csv("-1")).isEqualTo("'-1");
        assertThat(AdminController.csv("@usuario")).isEqualTo("'@usuario");
        assertThat(AdminController.csv("\tTab")).isEqualTo("'\tTab");
    }

    @Test
    void elApostrofoVaAntesDeEscaparLasComillas() {
        assertThat(AdminController.csv("=HYPERLINK(\"x\",\"y\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"");
        assertThat(AdminController.csv("\rx")).isEqualTo("\"'\rx\"");
    }

    @Test
    void lasCeldasNormalesNoCambian() {
        assertThat(AdminController.csv("Declaración de renta")).isEqualTo("Declaración de renta");
        assertThat(AdminController.csv("500000")).isEqualTo("500000");
        assertThat(AdminController.csv(null)).isEmpty();
    }
}
