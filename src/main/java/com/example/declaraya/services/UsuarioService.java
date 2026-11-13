package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario registrar(String nombre, String email, String passwordPlano, Rol rol, String tarjetaProfesional) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(passwordPlano));
        usuario.setRol(rol);
        usuario.setTarjetaProfesional(tarjetaProfesional);
        // las personas no requieren verificacion JCC; los contadores si, y arrancan sin verificar
        usuario.setVerificado(rol == Rol.PERSONA);
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public List<Usuario> listarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    public List<Usuario> listarNoVerificados() {
        return usuarioRepository.findAll().stream()
        .filter(u -> (u.getRol() == Rol.CONTADOR_PUBLICO || u.getRol() == Rol.CONTADOR_PRACTICANTE) && !u.isVerificado())
        .toList(); 
    }

    public List<Usuario> listarMentoresDisponibles(){
        return usuarioRepository.findByRol(Rol.CONTADOR_PUBLICO).stream().filter(Usuario::isVerificado)
        .toList();
    }

    public List<Usuario> listarPracticantesDe(Usuario mentor){
        return usuarioRepository.findByMentor(mentor);
    }

    public Usuario asignarMentor (Usuario practicante, Long mentorId){
        Usuario mentor = usuarioRepository.findById(mentorId)
        .orElseThrow(() -> new RecursoNoEncontradoException("Mentor no encontrado"));
        if (mentor.getRol() != Rol.CONTADOR_PUBLICO || !mentor.isVerificado()) {
            throw new IllegalStateException("El mentor debe ser un contador publico verificadp");
        }
        practicante.setMentor(mentor);
        return usuarioRepository.save(practicante);
    }

    public Usuario verificar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setVerificado(true);
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id){
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }


    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    private void aplicarDatosAdmin(Usuario u, Rol rol, String tarjeta, boolean verificado, Long mentorId, BigDecimal tarifa) {
        u.setRol(rol);
        boolean esContador = rol == Rol.CONTADOR_PUBLICO || rol == Rol.CONTADOR_PRACTICANTE;
        u.setTarjetaProfesional(esContador && tarjeta != null && !tarjeta.isBlank() ? tarjeta.trim() : null);
        u.setVerificado(!esContador || verificado);
        if (rol == Rol.CONTADOR_PRACTICANTE && mentorId != null) {
            Usuario mentor = buscarPorId(mentorId);
            if (mentor.getRol() != Rol.CONTADOR_PUBLICO) {
                throw new IllegalArgumentException("El mentor debe ser un contador público");
            }
            u.setMentor(mentor);
        } else {
            u.setMentor(null);
        }
        if (tarifa != null && tarifa.signum() >= 0) {
            u.setTarifaBase(tarifa);
        }
    }

    // el administrador crea cuentas completas (incluso contadores ya verificados)
    public Usuario crearPorAdmin(String nombre, String email, String passwordPlano, Rol rol,
                                  String tarjeta, boolean verificado, Long mentorId, BigDecimal tarifa) {
        if (nombre == null || nombre.isBlank() || email == null || email.isBlank() || rol == null
                || passwordPlano == null || passwordPlano.length() < 6) {
            throw new IllegalArgumentException("Completa nombre, correo, rol y una contraseña de mínimo 6 caracteres");
        }
        if (usuarioRepository.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Usuario u = new Usuario();
        u.setNombre(nombre.trim());
        u.setEmail(email.trim());
        u.setPassword(passwordEncoder.encode(passwordPlano));
        aplicarDatosAdmin(u, rol, tarjeta, verificado, mentorId, tarifa);
        return usuarioRepository.save(u);
    }

    public Usuario actualizarPorAdmin(Long id, String nombre, String email, String passwordPlano, Rol rol,
                                       String tarjeta, boolean verificado, Long mentorId, BigDecimal tarifa) {
        Usuario u = buscarPorId(id);
        if (nombre == null || nombre.isBlank() || email == null || email.isBlank() || rol == null) {
            throw new IllegalArgumentException("Nombre, correo y rol son obligatorios");
        }
        Optional<Usuario> otro = usuarioRepository.findByEmail(email.trim());
        if (otro.isPresent() && !otro.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ese correo ya está en uso");
        }
        u.setNombre(nombre.trim());
        u.setEmail(email.trim());
        if (passwordPlano != null && !passwordPlano.isBlank()) {
            if (passwordPlano.length() < 6) {
                throw new IllegalArgumentException("La contraseña debe tener mínimo 6 caracteres");
            }
            u.setPassword(passwordEncoder.encode(passwordPlano));
        }
        aplicarDatosAdmin(u, rol, tarjeta, verificado, mentorId, tarifa);
        return usuarioRepository.save(u);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
