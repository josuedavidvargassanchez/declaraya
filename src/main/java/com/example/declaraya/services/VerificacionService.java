package com.example.declaraya.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;

/**
 * Flujo de verificacion de contadores: el contador envia sus soportes (PENDIENTE),
 * el administrador los revisa y decide (APROBADO, RECHAZADO o CORRECCION_SOLICITADA).
 * "verificado" se mantiene sincronizado: solo es true cuando el estado es APROBADO.
 */
@Service
public class VerificacionService {

    public static final String DOC_TARJETA = "tarjeta";
    public static final String DOC_IDENTIDAD = "identidad";

    public static final int MOTIVO_MIN = 10;
    public static final int TEXTO_MAX = 500;

    private static final List<Rol> ROLES_CONTADOR = List.of(Rol.CONTADOR_PUBLICO, Rol.CONTADOR_PRACTICANTE);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ArchivoService archivoService;

    @Autowired
    private NotificacionService notificacionService;

    // ------------------------------------------------------------ consultas

    public Usuario buscarContador(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!usuario.isContador()) {
            throw new RecursoNoEncontradoException("Ese usuario no es un contador");
        }
        return usuario;
    }

    // lo que el admin tiene por revisar: primero los pendientes, luego los que esperan al contador
    public List<Usuario> listarPorRevisar() {
        return usuarioRepository.findByRolInAndEstadoVerificacionIn(ROLES_CONTADOR,
                        List.of(EstadoVerificacion.PENDIENTE, EstadoVerificacion.CORRECCION_SOLICITADA, EstadoVerificacion.RECHAZADO))
                .stream()
                .sorted(Comparator.comparing((Usuario u) -> u.getEstadoVerificacion().ordinal())
                        .thenComparing(Usuario::getFechaSolicitudVerificacion, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public long contarPendientes() {
        return usuarioRepository.countByRolInAndEstadoVerificacion(ROLES_CONTADOR, EstadoVerificacion.PENDIENTE);
    }

    // ------------------------------------------------------------ soportes del contador

    /** Devuelve el mensaje de error del archivo, o null si es valido. */
    public String validarSoporte(MultipartFile archivo, boolean obligatorio) {
        if (archivo == null || archivo.isEmpty()) {
            return obligatorio ? "Adjunta el soporte de la tarjeta profesional (PDF, JPG o PNG)" : null;
        }
        try {
            archivoService.validarSoporte(archivo);
            return null;
        } catch (ArchivoService.ArchivoNoPermitidoException e) {
            return e.getMessage();
        }
    }

    /** Al registrarse: guarda los soportes, deja la solicitud PENDIENTE y avisa a los administradores. */
    public Usuario registrarSoportesIniciales(Usuario contador, MultipartFile tarjeta, MultipartFile identidad) {
        try {
            if (tarjeta != null && !tarjeta.isEmpty()) {
                contador.setSoporteTarjetaProfesional(archivoService.guardarSoporteVerificacion(tarjeta));
            }
            if (identidad != null && !identidad.isEmpty()) {
                contador.setSoporteDocumentoIdentidad(archivoService.guardarSoporteVerificacion(identidad));
            }
        } catch (IOException e) {
            // la cuenta queda creada; el contador puede volver a subir los soportes desde su perfil
        }
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        contador.setVerificado(false);
        contador.setFechaSolicitudVerificacion(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(contador);
        notificarAdministradores("Nuevo contador por verificar: " + guardado.getNombre(), guardado);
        return guardado;
    }

    /** Desde /perfil: el contador envia un soporte nuevo y la solicitud vuelve a PENDIENTE. */
    public Usuario reenviarSoportes(Usuario contador, MultipartFile tarjeta, MultipartFile identidad) throws IOException {
        if (!contador.isContador()) {
            throw new ReglaNegocioException("Solo los contadores tienen verificación");
        }
        if (contador.getEstadoVerificacion() == EstadoVerificacion.APROBADO) {
            throw new ReglaNegocioException("Tu cuenta ya está verificada");
        }
        String error = validarSoporte(tarjeta, true);
        if (error == null) {
            error = validarSoporte(identidad, false);
        }
        if (error != null) {
            throw new ReglaNegocioException(error);
        }

        String anteriorTarjeta = contador.getSoporteTarjetaProfesional();
        contador.setSoporteTarjetaProfesional(archivoService.guardarSoporteVerificacion(tarjeta));
        archivoService.eliminarSoporteVerificacion(anteriorTarjeta);
        if (identidad != null && !identidad.isEmpty()) {
            String anteriorIdentidad = contador.getSoporteDocumentoIdentidad();
            contador.setSoporteDocumentoIdentidad(archivoService.guardarSoporteVerificacion(identidad));
            archivoService.eliminarSoporteVerificacion(anteriorIdentidad);
        }

        // la observacion y el motivo anteriores se conservan para que el admin vea el contexto
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        contador.setVerificado(false);
        contador.setFechaSolicitudVerificacion(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(contador);
        notificarAdministradores(guardado.getNombre() + " envió un nuevo soporte para su verificación", guardado);
        return guardado;
    }

    /**
     * Desde /perfil, un contador APROBADO pide que lo verifiquen de nuevo (por ejemplo porque
     * cambio su tarjeta profesional). Deja de estar verificado hasta que el admin lo revise.
     * Los soportes actuales se conservan; con el estado PENDIENTE puede reemplazarlos.
     */
    public Usuario solicitarNuevaVerificacion(Usuario contador, String nuevaTarjeta, boolean confirmado) {
        if (!contador.isContador()) {
            throw new ReglaNegocioException("Solo los contadores tienen verificación");
        }
        if (contador.getEstadoVerificacion() != EstadoVerificacion.APROBADO) {
            throw new ReglaNegocioException("Tu verificación ya está en curso; puedes subir un nuevo soporte");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Confirma que entiendes que no podrás ofertar hasta una nueva aprobación");
        }
        String tarjeta = nuevaTarjeta == null ? "" : nuevaTarjeta.trim();
        if (tarjeta.length() > 50) {
            throw new ReglaNegocioException("El número de tarjeta profesional no puede superar 50 caracteres");
        }
        if (!tarjeta.isEmpty()) {
            contador.setTarjetaProfesional(tarjeta);
        }
        contador.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        contador.setVerificado(false);
        contador.setFechaSolicitudVerificacion(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(contador);
        notificarAdministradores(guardado.getNombre() + " solicitó una nueva verificación"
                + (tarjeta.isEmpty() ? "" : " con la tarjeta profesional " + tarjeta), guardado);
        return guardado;
    }

    public Resource cargarDocumento(Usuario contador, String tipo) throws IOException {
        return archivoService.cargarSoporteVerificacion(nombreDocumento(contador, tipo));
    }

    public String nombreDocumento(Usuario contador, String tipo) {
        if (DOC_TARJETA.equals(tipo)) {
            return contador.getSoporteTarjetaProfesional();
        }
        if (DOC_IDENTIDAD.equals(tipo)) {
            return contador.getSoporteDocumentoIdentidad();
        }
        throw new RecursoNoEncontradoException("Tipo de documento no válido");
    }

    // ------------------------------------------------------------ decisiones del administrador

    public Usuario aprobar(Long contadorId, Usuario admin, String observacion, boolean confirmado) {
        Usuario contador = paraDecidir(contadorId, confirmado);
        if (contador.getSoporteTarjetaProfesional() == null) {
            throw new ReglaNegocioException("No se puede aprobar sin el soporte de la tarjeta profesional. Solicita una corrección.");
        }
        String obs = opcional(observacion, "La observación");

        contador.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        contador.setObservacionVerificacion(obs);
        contador.setMotivoRechazo(null);
        registrarRevision(contador, admin);

        Usuario guardado = usuarioRepository.save(contador);
        notificacionService.notificar(guardado,
                "Tu cuenta de contador fue verificada. Ya puedes enviar ofertas.", "/inicio");
        return guardado;
    }

    public Usuario rechazar(Long contadorId, Usuario admin, String motivo, String observacion, boolean confirmado) {
        Usuario contador = paraDecidir(contadorId, confirmado);
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < MOTIVO_MIN || motivoLimpio.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("El motivo del rechazo debe tener entre " + MOTIVO_MIN + " y " + TEXTO_MAX + " caracteres");
        }
        String obs = opcional(observacion, "La observación");

        contador.setEstadoVerificacion(EstadoVerificacion.RECHAZADO);
        contador.setMotivoRechazo(motivoLimpio);
        contador.setObservacionVerificacion(obs);
        registrarRevision(contador, admin);

        Usuario guardado = usuarioRepository.save(contador);
        notificacionService.notificar(guardado,
                "Tu verificación fue rechazada. Motivo: " + motivoLimpio, "/perfil");
        return guardado;
    }

    public Usuario solicitarCorreccion(Long contadorId, Usuario admin, String observacion, boolean confirmado) {
        Usuario contador = paraDecidir(contadorId, confirmado);
        String obs = observacion == null ? "" : observacion.trim();
        if (obs.isEmpty()) {
            throw new ReglaNegocioException("Indica qué debe corregir el contador");
        }
        if (obs.length() > TEXTO_MAX) {
            throw new ReglaNegocioException("La observación no puede superar " + TEXTO_MAX + " caracteres");
        }

        contador.setEstadoVerificacion(EstadoVerificacion.CORRECCION_SOLICITADA);
        contador.setObservacionVerificacion(obs);
        registrarRevision(contador, admin);

        Usuario guardado = usuarioRepository.save(contador);
        notificacionService.notificar(guardado,
                "El administrador pidió una corrección en tu verificación: " + obs, "/perfil");
        return guardado;
    }

    // ------------------------------------------------------------ auxiliares

    // toda decision exige la confirmacion explicita y que la solicitud siga PENDIENTE
    private Usuario paraDecidir(Long contadorId, boolean confirmado) {
        Usuario contador = buscarContador(contadorId);
        if (contador.getEstadoVerificacion() != EstadoVerificacion.PENDIENTE) {
            String actual = contador.getEstadoVerificacion() == null ? "sin estado" : contador.getEstadoVerificacion().getEtiqueta();
            throw new ReglaNegocioException("Solo se puede decidir sobre solicitudes pendientes (estado actual: " + actual + ")");
        }
        if (!confirmado) {
            throw new ReglaNegocioException("Debes confirmar que revisaste la tarjeta profesional y los documentos");
        }
        return contador;
    }

    private void registrarRevision(Usuario contador, Usuario admin) {
        contador.setVerificado(contador.getEstadoVerificacion() == EstadoVerificacion.APROBADO);
        contador.setFechaRevisionVerificacion(LocalDateTime.now());
        contador.setRevisadoPor(admin);
    }

    private String opcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > TEXTO_MAX) {
            throw new ReglaNegocioException(campo + " no puede superar " + TEXTO_MAX + " caracteres");
        }
        return limpio;
    }

    private void notificarAdministradores(String mensaje, Usuario contador) {
        for (Usuario admin : usuarioRepository.findByRol(Rol.ADMIN)) {
            notificacionService.notificar(admin, mensaje, "/admin/verificaciones/" + contador.getId());
        }
    }
}
