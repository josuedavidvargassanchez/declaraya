package com.example.declaraya.config;

import java.io.IOException;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.declaraya.services.IntentosLoginService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login fallido: /login?bloqueado si la cuenta esta bloqueada (o lo quedo con este intento, el quinto);
 * en los demas casos /login?error, como antes.
 */
@Component
public class LoginFallidoHandler extends SimpleUrlAuthenticationFailureHandler {

    private final IntentosLoginService intentosLoginService;

    public LoginFallidoHandler(IntentosLoginService intentosLoginService) {
        super("/login?error");
        this.intentosLoginService = intentosLoginService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof LockedException || intentosLoginService.estaBloqueado(request.getParameter("username"))) {
            getRedirectStrategy().sendRedirect(request, response, "/login?bloqueado");
            return;
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
