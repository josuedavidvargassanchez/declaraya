package com.example.declaraya.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // registro de sesiones abiertas: SesionesService las expira al cambiar rol, eliminar o cambiar contraseña
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // avisa al registro cuando una sesion se destruye o cambia de id
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginFallidoHandler loginFallido,
                                           SessionRegistry sessionRegistry) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro", "/politica-datos", "/ayuda", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/solicitudes/*/ofertas").hasAnyRole("CONTADOR_PUBLICO", "CONTADOR_PRACTICANTE")
                        .requestMatchers("/ofertas/*/aceptar", "/ofertas/*/rechazar").hasRole("PERSONA")
                        .requestMatchers("/ofertas/*/editar", "/ofertas/*/retirar").hasAnyRole("CONTADOR_PUBLICO", "CONTADOR_PRACTICANTE")
                        // mentorias: ademas de la ruta, MentoriaService exige ser el mentor vigente o el dueño de la solicitud
                        .requestMatchers("/admin/mentorias/**").hasRole("ADMIN")
                        .requestMatchers("/contador/practicantes/**").hasRole("CONTADOR_PUBLICO")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/contador/**").hasRole("CONTADOR_PUBLICO")
                        .requestMatchers("/practicante/**").hasRole("CONTADOR_PRACTICANTE")
                        .requestMatchers("/cliente/**").hasRole("PERSONA")
                        // ver/descargar documentos del cliente: cualquier usuario autenticado, pero el
                        // controlador exige DocumentoClienteService.puedeVer y responde 403 si no aplica
                        .requestMatchers(HttpMethod.GET, "/documentos/*", "/documentos/*/descargar").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/inicio", true)
                        // /login?bloqueado tras 5 fallos seguidos (IntentosLoginService); si no, /login?error
                        .failureHandler(loginFallido)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // sin limite de sesiones por usuario; solo se registran para poder expirarlas
                .sessionManagement(sesiones -> sesiones
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expirada")
                );

        return http.build();
    }
}
