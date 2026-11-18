package com.example.declaraya.services;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IntentosLoginService intentosLoginService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // tras 5 fallos seguidos la cuenta queda bloqueada 15 minutos (LockedException al iniciar sesion)
        boolean bloqueado = intentosLoginService.estaBloqueado(usuario.getEmail());
        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                true, true, true, !bloqueado,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );
    }
}
