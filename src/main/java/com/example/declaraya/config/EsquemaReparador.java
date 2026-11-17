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

        try {
            jdbc.execute("ALTER TABLE disputas MODIFY COLUMN estado VARCHAR(30)");
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo ajustar la columna disputas.estado: " + e.getMessage());
        }

        // los mensajes de la administracion llevan un prefijo ("[Administración → Cliente] ...")
        // ademas de los 1000 caracteres del texto, asi que la columna necesita un poco mas de espacio
        try {
            jdbc.execute("ALTER TABLE mensajes MODIFY COLUMN contenido VARCHAR(1100)");
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo ajustar la columna mensajes.contenido: " + e.getMessage());
        }

        // contadores creados antes de que existiera el estado de verificacion:
        // los ya verificados quedan APROBADO y el resto PENDIENTE (no se inventan fechas ni revisores)
        try {
            int migrados = jdbc.update("UPDATE usuarios SET estado_verificacion = "
                    + "CASE WHEN verificado THEN 'APROBADO' ELSE 'PENDIENTE' END "
                    + "WHERE estado_verificacion IS NULL AND rol IN ('CONTADOR_PUBLICO', 'CONTADOR_PRACTICANTE')");
            if (migrados > 0) {
                System.out.println("Estado de verificacion asignado a " + migrados + " contadores existentes.");
            }
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo migrar el estado de verificacion: " + e.getMessage());
        }

        // acuerdos creados antes de existir la etapa: FINALIZADO si ya terminaron, APROBACION_MENTOR
        // si su supervision sigue pendiente y DOCUMENTACION en los demas casos
        try {
            int migrados = jdbc.update("UPDATE acuerdos SET etapa = 'FINALIZADO' WHERE etapa IS NULL AND estado = 'FINALIZADO'");
            migrados += jdbc.update("UPDATE acuerdos a JOIN supervisiones s ON s.acuerdo_id = a.id "
                    + "SET a.etapa = 'APROBACION_MENTOR' WHERE a.etapa IS NULL AND s.estado = 'PENDIENTE_APROBACION'");
            migrados += jdbc.update("UPDATE acuerdos SET etapa = 'DOCUMENTACION' WHERE etapa IS NULL");
            if (migrados > 0) {
                System.out.println("Etapa asignada a " + migrados + " acuerdos existentes.");
            }
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo migrar la etapa de los acuerdos: " + e.getMessage());
        }
    }
}
