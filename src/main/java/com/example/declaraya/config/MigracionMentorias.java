package com.example.declaraya.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.declaraya.services.MentoriaService;

/**
 * Practicantes que ya tenian mentor antes del flujo de solicitudes: se les crea la asignacion vigente
 * ("Asignación previa al flujo de solicitudes", desde hoy). Corre despues de DemoDataSeeder, que crea
 * las de los datos de ejemplo con fechas coherentes; aqui solo se completa lo que falte.
 */
@Component
@Order(30)
public class MigracionMentorias implements CommandLineRunner {

    private final MentoriaService mentoriaService;

    public MigracionMentorias(MentoriaService mentoriaService) {
        this.mentoriaService = mentoriaService;
    }

    @Override
    public void run(String... args) {
        try {
            int creadas = mentoriaService.migrarAsignacionesPrevias();
            if (creadas > 0) {
                System.out.println("Asignaciones de mentoría creadas para " + creadas + " practicantes con mentor previo.");
            }
        } catch (Exception e) {
            System.out.println("Aviso: no se pudieron migrar las asignaciones de mentoría: " + e.getMessage());
        }
    }
}
