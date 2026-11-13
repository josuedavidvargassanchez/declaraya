package com.example.declaraya.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// MySQL crea los estados como ENUM y "ddl-auto=update" no agrega valores nuevos (como RETIRADA)
// a una tabla que ya existe. Convertimos la columna a texto para que funcione sin borrar la base.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EsquemaReparador implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE ofertas MODIFY COLUMN estado VARCHAR(20)");
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo ajustar la columna ofertas.estado: " + e.getMessage());
        }
    }
}
