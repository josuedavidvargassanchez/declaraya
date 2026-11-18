package com.example.declaraya.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

/**
 * Freno a la fuerza bruta en el login, en memoria (se pierde al reiniciar, sin dependencias nuevas):
 * tras 5 fallos seguidos el correo queda bloqueado 15 minutos; un acceso correcto reinicia la cuenta.
 * CustomUserDetailsService consulta estaBloqueado y devuelve el usuario con accountNonLocked=false.
 */
@Service
public class IntentosLoginService {

    public static final int MAX_FALLOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);
    // por si llegan miles de correos inventados: se limpian los registros viejos
    private static final int LIMPIAR_DESDE = 10_000;

    private record Registro(int fallos, Instant bloqueadoHasta, Instant ultimoFallo) {
    }

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    @Autowired
    public IntentosLoginService() {
        this(Clock.systemUTC());
    }

    IntentosLoginService(Clock reloj) {
        this.reloj = reloj;
    }

    private static String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public void registrarFallo(String email) {
        Instant ahora = reloj.instant();
        registros.compute(clave(email), (k, r) -> {
            if (r != null && r.bloqueadoHasta() != null && ahora.isBefore(r.bloqueadoHasta())) {
                return r; // ya bloqueado: no se alarga el bloqueo
            }
            int fallos = (r == null || r.bloqueadoHasta() != null) ? 1 : r.fallos() + 1;
            Instant hasta = fallos >= MAX_FALLOS ? ahora.plus(BLOQUEO) : null;
            return new Registro(fallos, hasta, ahora);
        });
        if (registros.size() > LIMPIAR_DESDE) {
            registros.entrySet().removeIf(e -> e.getValue().ultimoFallo().isBefore(ahora.minus(BLOQUEO)));
        }
    }

    public void reiniciar(String email) {
        registros.remove(clave(email));
    }

    public boolean estaBloqueado(String email) {
        Registro r = registros.get(clave(email));
        if (r == null || r.bloqueadoHasta() == null) {
            return false;
        }
        if (reloj.instant().isBefore(r.bloqueadoHasta())) {
            return true;
        }
        registros.remove(clave(email), r); // el bloqueo vencio: vuelve a empezar de cero
        return false;
    }

    // Spring Security publica estos eventos desde el AuthenticationManager del formulario de login
    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        registrarFallo(evento.getAuthentication().getName());
    }

    @EventListener
    public void alEntrar(AuthenticationSuccessEvent evento) {
        reiniciar(evento.getAuthentication().getName());
    }
}
