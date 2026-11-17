package com.example.declaraya.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.declaraya.enums.EstadoSolicitudMentoria;
import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.SolicitudMentoria;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.CatalogoRequisitos;
import com.example.declaraya.services.MentoriaService;
import com.example.declaraya.services.UsuarioService;

/**
 * Mentoria de practicantes. SecurityConfig restringe cada prefijo a su rol:
 * /practicante/** (practicante), /admin/mentorias/** (admin) y /contador/practicantes/** (contador publico);
 * MentoriaService ademas comprueba que el contador sea el mentor vigente y que cada practicante
 * solo toque sus propias solicitudes.
 */
@Controller
public class MentoriaController {

    private final MentoriaService mentoriaService;
    private final UsuarioService usuarioService;

    public MentoriaController(MentoriaService mentoriaService, UsuarioService usuarioService) {
        this.mentoriaService = mentoriaService;
        this.usuarioService = usuarioService;
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private static String conError(String ruta, String mensaje) {
        return "redirect:" + ruta + "?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------ practicante: "Mi mentor"

    @GetMapping("/practicante/mentor")
    public String miMentor(Model model, Authentication authentication) {
        Usuario practicante = usuarioActual(authentication);
        SolicitudMentoria ultima = mentoriaService.ultimaSolicitud(practicante).orElse(null);
        model.addAttribute("usuario", practicante);
        model.addAttribute("mentor", practicante.getMentor());
        model.addAttribute("asignacion", mentoriaService.asignacionVigente(practicante).orElse(null));
        model.addAttribute("pendiente", mentoriaService.solicitudPendiente(practicante).orElse(null));
        model.addAttribute("rechazada", ultima != null && ultima.getEstado() == EstadoSolicitudMentoria.RECHAZADA ? ultima : null);
        model.addAttribute("verificado", practicante.getEstadoVerificacion() == EstadoVerificacion.APROBADO);
        model.addAttribute("avisoVerificacion", MentoriaService.VERIFICACION_REQUERIDA);
        model.addAttribute("tramites", CatalogoRequisitos.TRAMITES);
        model.addAttribute("historial", mentoriaService.historial(practicante));
        model.addAttribute("textoMax", MentoriaService.TEXTO_MAX);
        return "practicante-mentor";
    }

    @PostMapping("/practicante/mentor/solicitudes")
    public String solicitarMentor(@RequestParam(required = false) String especialidad,
                                  @RequestParam(required = false) String mensaje,
                                  Authentication authentication) {
        try {
            mentoriaService.solicitar(usuarioActual(authentication), especialidad, mensaje);
            return "redirect:/practicante/mentor?solicitudEnviada";
        } catch (ReglaNegocioException e) {
            return conError("/practicante/mentor", e.getMessage());
        }
    }

    @PostMapping("/practicante/mentor/cambio")
    public String solicitarCambio(@RequestParam(required = false) String motivo,
                                  @RequestParam(required = false) String especialidad,
                                  @RequestParam(required = false) String mensaje,
                                  Authentication authentication) {
        try {
            mentoriaService.solicitarCambio(usuarioActual(authentication), motivo, especialidad, mensaje);
            return "redirect:/practicante/mentor?cambioSolicitado";
        } catch (ReglaNegocioException e) {
            return conError("/practicante/mentor", e.getMessage());
        }
    }

    @PostMapping("/practicante/mentor/solicitudes/{id}/cancelar")
    public String cancelarSolicitud(@PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean confirmado,
                                    Authentication authentication) {
        try {
            mentoriaService.cancelar(id, usuarioActual(authentication), confirmado);
            return "redirect:/practicante/mentor?solicitudCancelada";
        } catch (ReglaNegocioException e) {
            return conError("/practicante/mentor", e.getMessage());
        }
    }

    // ------------------------------------------------------------ admin: "Gestión de mentorías"

    @GetMapping("/admin/mentorias")
    public String gestion(@RequestParam(required = false) Long practicante, Model model) {
        model.addAttribute("pendientes", mentoriaService.solicitudesPendientes());
        model.addAttribute("sinMentor", mentoriaService.practicantesSinMentor());
        model.addAttribute("mentores", mentoriaService.resumenMentores());
        model.addAttribute("practicantes", mentoriaService.practicantes());
        model.addAttribute("filtroPracticante", practicante);
        model.addAttribute("asignaciones", mentoriaService.historialAsignaciones(practicante));
        return "admin-mentorias";
    }

    @GetMapping("/admin/mentorias/{id}")
    public String detalleSolicitud(@PathVariable Long id, Model model) {
        SolicitudMentoria solicitud = mentoriaService.buscarSolicitud(id);
        Usuario practicante = solicitud.getPracticante();
        model.addAttribute("s", solicitud);
        model.addAttribute("p", practicante);
        model.addAttribute("asignacion", mentoriaService.asignacionVigente(practicante).orElse(null));
        model.addAttribute("elegibles", solicitud.isPendiente()
                ? mentoriaService.mentoresElegibles(practicante, solicitud.getEspecialidad()) : List.of());
        model.addAttribute("motivoMin", MentoriaService.MOTIVO_MIN);
        model.addAttribute("textoMax", MentoriaService.TEXTO_MAX);
        return "admin-mentoria-detalle";
    }

    @PostMapping("/admin/mentorias/{id}/asignar")
    public String asignar(@PathVariable Long id,
                          @RequestParam(required = false) Long mentorId,
                          @RequestParam(required = false) String observacion,
                          @RequestParam(defaultValue = "false") boolean confirmado,
                          Authentication authentication) {
        try {
            mentoriaService.asignar(id, mentorId, usuarioActual(authentication), observacion, confirmado);
            return "redirect:/admin/mentorias/" + id + "?asignado";
        } catch (ReglaNegocioException e) {
            return conError("/admin/mentorias/" + id, e.getMessage());
        }
    }

    @PostMapping("/admin/mentorias/{id}/rechazar")
    public String rechazar(@PathVariable Long id,
                           @RequestParam(required = false) String motivo,
                           @RequestParam(defaultValue = "false") boolean confirmado,
                           Authentication authentication) {
        try {
            mentoriaService.rechazar(id, usuarioActual(authentication), motivo, confirmado);
            return "redirect:/admin/mentorias/" + id + "?rechazado";
        } catch (ReglaNegocioException e) {
            return conError("/admin/mentorias/" + id, e.getMessage());
        }
    }

    @GetMapping("/admin/mentorias/practicantes/{id}")
    public String fichaPracticante(@PathVariable Long id, Model model) {
        Usuario practicante = usuarioService.buscarPorId(id);
        if (practicante.getRol() != Rol.CONTADOR_PRACTICANTE) {
            throw new RecursoNoEncontradoException("Ese usuario no es un contador practicante");
        }
        SolicitudMentoria pendiente = mentoriaService.solicitudPendiente(practicante).orElse(null);
        boolean puedeCambiar = practicante.getMentor() != null && pendiente == null;
        model.addAttribute("p", practicante);
        model.addAttribute("asignacion", mentoriaService.asignacionVigente(practicante).orElse(null));
        model.addAttribute("pendiente", pendiente);
        model.addAttribute("puedeCambiar", puedeCambiar);
        model.addAttribute("elegibles", puedeCambiar ? mentoriaService.mentoresElegibles(practicante, null) : List.of());
        model.addAttribute("historial", mentoriaService.historial(practicante));
        model.addAttribute("textoMax", MentoriaService.TEXTO_MAX);
        return "admin-mentoria-practicante";
    }

    @PostMapping("/admin/mentorias/practicantes/{id}/cambiar")
    public String cambiarDirecto(@PathVariable Long id,
                                 @RequestParam(required = false) Long mentorId,
                                 @RequestParam(required = false) String motivo,
                                 @RequestParam(defaultValue = "false") boolean confirmado,
                                 Authentication authentication) {
        try {
            mentoriaService.cambiarDirecto(id, mentorId, usuarioActual(authentication), motivo, confirmado);
            return "redirect:/admin/mentorias/practicantes/" + id + "?cambiado";
        } catch (ReglaNegocioException e) {
            return conError("/admin/mentorias/practicantes/" + id, e.getMessage());
        }
    }

    // ------------------------------------------------------------ mentor: supervisiones de un practicante

    @GetMapping("/contador/practicantes/{id}")
    public String supervisionesDePracticante(@PathVariable Long id, Model model, Authentication authentication) {
        Usuario mentor = usuarioActual(authentication);
        Usuario practicante = mentoriaService.exigirPracticanteDe(mentor, id);
        model.addAttribute("p", practicante);
        model.addAttribute("asignacion", mentoriaService.asignacionVigente(practicante).orElse(null));
        model.addAttribute("acuerdos", mentoriaService.acuerdosDe(practicante));
        model.addAttribute("entregas", mentoriaService.entregasDe(practicante));
        model.addAttribute("supervisiones", mentoriaService.supervisionesDe(practicante));
        model.addAttribute("historial", mentoriaService.historial(practicante));
        return "contador-practicante";
    }
}
