package com.example.declaraya.services;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Cierra sesiones abiertas usando el SessionRegistry de SecurityConfig. Una sesion expirada se
 * redirige a /login?expirada en su siguiente peticion (ConcurrentSessionFilter).
 */
@Service
public class SesionesService {

    private final SessionRegistry sessionRegistry;

    public SesionesService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** El admin le cambio el rol o elimino la cuenta: todas sus sesiones dejan de valer. */
    public void expirarTodas(String email) {
        expirar(email, null);
    }

    /** Cambio su contraseña: se cierran las demas sesiones y se conserva la actual. */
    public void expirarOtras(String email, String sesionActual) {
        expirar(email, sesionActual);
    }

    private void expirar(String email, String conservar) {
        if (email == null) {
            return;
        }
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String nombre = principal instanceof UserDetails u ? u.getUsername() : String.valueOf(principal);
            if (!email.equalsIgnoreCase(nombre)) {
                continue;
            }
            for (SessionInformation sesion : sessionRegistry.getAllSessions(principal, false)) {
                if (!sesion.getSessionId().equals(conservar)) {
                    sesion.expireNow();
                }
            }
        }
    }
}
