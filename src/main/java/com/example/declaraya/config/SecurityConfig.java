package com.example.declaraya.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro", "/politica-datos", "/ayuda", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/solicitudes/*/ofertas").hasAnyRole("CONTADOR_PUBLICO", "CONTADOR_PRACTICANTE")
                        .requestMatchers("/ofertas/*/aceptar", "/ofertas/*/rechazar").hasRole("PERSONA")
                        .requestMatchers("/ofertas/*/editar", "/ofertas/*/retirar").hasAnyRole("CONTADOR_PUBLICO", "CONTADOR_PRACTICANTE")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/contador/**").hasRole("CONTADOR_PUBLICO")
                        .requestMatchers("/practicante/**").hasRole("CONTADOR_PRACTICANTE")
                        .requestMatchers("/cliente/**").hasRole("PERSONA")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/inicio", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
