package com.example.declaraya.services;

import com.example.declaraya.dto.PerfilDTO;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        // las personas no requieren verificacion JCC; los contadores si, y arrancan PENDIENTES
        usuario.setVerificado(rol == Rol.PERSONA);
        if (usuario.isContador()) {
            usuario.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
            usuario.setFechaSolicitudVerificacion(LocalDateTime.now());
        }
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public List<Usuario> listarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
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

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id){
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }


    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    private void aplicarDatosAdmin(Usuario u, Rol rol, String tarjeta, Long mentorId, BigDecimal tarifa) {
        boolean eraContador = u.isContador();
        u.setRol(rol);
        boolean esContador = u.isContador();
        u.setTarjetaProfesional(esContador && tarjeta != null && !tarjeta.isBlank() ? tarjeta.trim() : null);
        if (!esContador) {
            // personas y administradores no pasan por verificacion
            u.setEstadoVerificacion(null);
            u.setVerificado(true);
        } else if (!eraContador || u.getEstadoVerificacion() == null) {
            // quien pasa a ser contador arranca una solicitud nueva que el admin debe revisar
            u.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
            u.setFechaSolicitudVerificacion(LocalDateTime.now());
            u.setVerificado(false);
        }
        // si ya era contador, su estado de verificacion solo cambia desde la pantalla de revision
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

    // el administrador crea cuentas completas (incluso contadores ya verificados: en ese caso
    // queda registrado como quien aprobo la verificacion)
    public Usuario crearPorAdmin(String nombre, String email, String passwordPlano, Rol rol,
                                  String tarjeta, boolean verificado, Long mentorId, BigDecimal tarifa,
                                  Usuario admin) {
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
        aplicarDatosAdmin(u, rol, tarjeta, mentorId, tarifa);
        if (u.isContador() && verificado) {
            LocalDateTime ahora = LocalDateTime.now();
            u.setEstadoVerificacion(EstadoVerificacion.APROBADO);
            u.setVerificado(true);
            u.setFechaRevisionVerificacion(ahora);
            u.setRevisadoPor(admin);
            u.setObservacionVerificacion("Cuenta creada y verificada directamente por el administrador");
        }
        return usuarioRepository.save(u);
    }

    public Usuario actualizarPorAdmin(Long id, String nombre, String email, String passwordPlano, Rol rol,
                                       String tarjeta, Long mentorId, BigDecimal tarifa) {
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
        aplicarDatosAdmin(u, rol, tarjeta, mentorId, tarifa);
        return usuarioRepository.save(u);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    // ------------------------------------------------------------ Mi perfil

    public static final int CONTRASENA_MIN = 8;

    /**
     * Guarda solo los campos que cada rol puede editar. Rol, correo, tarjeta profesional,
     * mentor y verificacion nunca se tocan aqui (no estan en el DTO y no se copian).
     */
    public Usuario actualizarPerfil(Usuario usuario, PerfilDTO datos) {
        usuario.setNombre(datos.getNombre().trim());
        if (usuario.getRol() == Rol.ADMIN) {
            return usuarioRepository.save(usuario);
        }
        boolean hayNumero = datos.getNumeroDocumento() != null && !datos.getNumeroDocumento().isBlank();
        usuario.setTipoDocumento(hayNumero ? datos.getTipoDocumento() : null);
        usuario.setNumeroDocumento(hayNumero ? datos.getNumeroDocumento().trim().toUpperCase() : null);
        usuario.setTelefono(textoOpcional(datos.getTelefono()));
        usuario.setCiudad(textoOpcional(datos.getCiudad()));
        if (usuario.isContador()) {
            usuario.setDescripcionProfesional(textoOpcional(datos.getDescripcionProfesional()));
            if (datos.getTarifaBase() != null && datos.getTarifaBase().signum() >= 0) {
                usuario.setTarifaBase(datos.getTarifaBase());
            }
            // solo tramites del catalogo, sin repetir, en el orden del catalogo
            List<String> elegidas = datos.getEspecialidades() == null ? List.of() : datos.getEspecialidades();
            String especialidades = CatalogoRequisitos.TRAMITES.stream().filter(elegidas::contains)
                    .collect(java.util.stream.Collectors.joining("|"));
            usuario.setEspecialidades(especialidades.isEmpty() ? null : especialidades);
            Integer anios = datos.getAniosExperiencia();
            usuario.setAniosExperiencia(anios != null && anios >= 0 && anios <= 60 ? anios : null);
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario cambiarContrasena(Usuario usuario, String actual, String nueva, String confirmacion) {
        if (actual == null || !passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (nueva == null || nueva.length() < CONTRASENA_MIN) {
            throw new ReglaNegocioException("La nueva contraseña debe tener mínimo " + CONTRASENA_MIN + " caracteres");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ReglaNegocioException("La confirmación no coincide con la nueva contraseña");
        }
        if (passwordEncoder.matches(nueva, usuario.getPassword())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
        return usuarioRepository.save(usuario);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
