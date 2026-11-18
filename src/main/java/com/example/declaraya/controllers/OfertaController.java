package com.example.declaraya.controllers;

import com.example.declaraya.dto.NuevaOfertaDTO;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.FlujoAcuerdoService;
import com.example.declaraya.services.MentoriaService;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.OfertaService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class OfertaController {

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private SolicitudDeclaracionService solicitudService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private FlujoAcuerdoService flujoService;

    @Autowired
    private MentoriaService mentoriaService;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private Oferta ofertaDeLaPersona(Long id, Usuario persona){
        Oferta oferta = ofertaService.buscarPorId(id);
        if (!oferta.getSolicitud().getPersona().getId().equals(persona.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta oferta no es para ti");
        }
        return oferta;
    }

    private Oferta ofertaDelContador(Long id, Usuario contador) {
        Oferta oferta = ofertaService.buscarPorId(id);
        if (!oferta.getContador().getId().equals(contador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta oferta no es tuya");
        }
        return oferta;
    }

    private String inicioDe(Usuario contador) {
        return contador.getRol() == Rol.CONTADOR_PRACTICANTE ? "/practicante/inicio" : "/contador/inicio";
    }

    private boolean ofertaEditable(Oferta oferta) {
        return oferta.getEstado() == EstadoOferta.PENDIENTE
                && oferta.getSolicitud().getEstado() == EstadoSolicitud.ABIERTA;
    }

    // numeros para las tarjetas de resumen del panel del contador
    private void resumenOfertas(Model model, List<Oferta> ofertas) {
        model.addAttribute("ofertasPendientes", ofertas.stream().filter(o -> o.getEstado() == EstadoOferta.PENDIENTE).count());
        model.addAttribute("ofertasAceptadas", ofertas.stream().filter(o -> o.getEstado() == EstadoOferta.ACEPTADA).count());
        model.addAttribute("ofertasTotal", ofertas.size());
    }

    @GetMapping("/contador/inicio")
    public String inicioContador(Model model, Authentication authentication) {
        Usuario contador = usuarioActual(authentication);
        model.addAttribute("usuario", contador);
        model.addAttribute("solicitudesDisponibles", solicitudService.listarAbiertas());
        List<Oferta> ofertasContador = ofertaService.listarPorContador(contador);
        model.addAttribute("misOfertas", ofertasContador);
        resumenOfertas(model, ofertasContador);
        model.addAttribute("practicantes", mentoriaService.resumenPracticantes(contador));
        model.addAttribute("practicantesSupervisados", supervisionService.listarPorMentor(contador));
        model.addAttribute("entregasPorRevisar", flujoService.entregasPorRevisar(contador));
        return "inicio-contador";
    }

    @GetMapping("/practicante/inicio")
    public String inicioPracticante(Model model, Authentication authentication) {
        Usuario practicante = usuarioActual(authentication);
        model.addAttribute("usuario", practicante);
        model.addAttribute("solicitudesDisponibles", solicitudService.listarAbiertas());
        List<Oferta> ofertasPracticante = ofertaService.listarPorContador(practicante);
        model.addAttribute("misOfertas", ofertasPracticante);
        resumenOfertas(model, ofertasPracticante);
        model.addAttribute("supervisiones", supervisionService.listarPorPracticante(practicante));
        model.addAttribute("solicitudMentoriaPendiente", mentoriaService.solicitudPendiente(practicante).isPresent());
        model.addAttribute("casosActivos", acuerdoService.contarActivosDe(practicante));
        return "inicio-practicante";
    }

    // el practicante ya no elige mentor: lo solicita en /practicante/mentor y lo asigna el administrador

    // el practicante tambien usa este endpoint; su oferta queda igual, pero el
    // acuerdo que resulte debera pasar por Supervision antes de darse por valido
    @PostMapping("/solicitudes/{id}/ofertas")
    public String enviarOferta(@PathVariable Long id,
                                @Valid @ModelAttribute("oferta") NuevaOfertaDTO dto,
                                BindingResult resultado,
                                Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudService.buscarPorId(id);
        Usuario contador = usuarioActual(authentication);
        boolean practicante = contador.getRol() == Rol.CONTADOR_PRACTICANTE;
        String inicio = practicante ? "/practicante/inicio" : "/contador/inicio";

        // misma regla que decide quien ve las solicitudes abiertas (SolicitudDeclaracionService)
        String motivo = solicitudService.motivoNoPuedeOfertar(contador);
        if (motivo != null) {
            return "redirect:" + inicio + "?" + motivo;
        }
        if (solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            return "redirect:" + inicio + "?errorSolicitud";
        }
        // los errores se muestran sobre el formulario de oferta del mismo panel
        if (resultado.hasFieldErrors("valorPropuesto")) {
            return "redirect:" + inicio + "?errorValor";
        }
        if (resultado.hasFieldErrors("fechaEntregaEstimada") || !OfertaService.fechaEntregaValida(dto.getFechaEntregaEstimada())) {
            return "redirect:" + inicio + "?errorFecha";
        }
        if (resultado.hasErrors()) {
            return "redirect:" + inicio + "?errorMensaje";
        }
        boolean yaOferto = ofertaService.listarPorSolicitud(solicitud).stream()
                .anyMatch(o -> o.getContador().getId().equals(contador.getId()) && o.getEstado() != EstadoOferta.RETIRADA);
        if (yaOferto) {
            return "redirect:" + inicio + "?errorDuplicada";
        }

        ofertaService.crear(solicitud, contador, dto.getMensaje(), dto.getValorPropuesto(), dto.getFechaEntregaEstimada());
        notificacionService.notificar(solicitud.getPersona(),
                "Nueva oferta de " + contador.getNombre() + " para \"" + solicitud.getTitulo() + "\"",
                "/solicitudes/" + solicitud.getId());
        return "redirect:" + inicio + "?ofertaEnviada";
    }

    // la logica (bloqueo, validacion, acuerdo, supervision y notificaciones) vive en OfertaService.aceptar
    @PostMapping("/ofertas/{id}/aceptar")
    public String aceptarOferta(@PathVariable Long id, Authentication authentication) {
        Usuario persona = usuarioActual(authentication);
        try {
            Acuerdo acuerdo = ofertaService.aceptar(id, persona);
            return "redirect:/solicitudes/" + acuerdo.getOferta().getSolicitud().getId();
        } catch (ReglaNegocioException e) {
            return "redirect:/solicitudes/" + ofertaDeLaPersona(id, persona).getSolicitud().getId() + "?ofertaNoDisponible";
        }
    }

    @PostMapping("/ofertas/{id}/rechazar")
    public String rechazarOferta(@PathVariable Long id, Authentication authentication) {
        Oferta oferta = ofertaDeLaPersona(id, usuarioActual(authentication));
        if (oferta.getEstado() == EstadoOferta.PENDIENTE) {
            ofertaService.actualizarEstado(id, EstadoOferta.RECHAZADA);
            notificacionService.notificar(oferta.getContador(),
                    "Tu oferta para \"" + oferta.getSolicitud().getTitulo() + "\" fue rechazada",
                    "/solicitudes/" + oferta.getSolicitud().getId());
        }
        return "redirect:/solicitudes/" + oferta.getSolicitud().getId();
    }

    @GetMapping("/ofertas/{id}/editar")
    public String formularioEditarOferta(@PathVariable Long id, Model model, Authentication authentication) {
        Usuario contador = usuarioActual(authentication);
        Oferta oferta = ofertaDelContador(id, contador);
        if (!ofertaEditable(oferta)) {
            return "redirect:" + inicioDe(contador);
        }
        model.addAttribute("oferta", oferta);
        model.addAttribute("volver", inicioDe(contador));
        return "oferta-editar";
    }

    @PostMapping("/ofertas/{id}/editar")
    public String editarOferta(@PathVariable Long id,
                                @Valid @ModelAttribute("ofertaForm") NuevaOfertaDTO dto,
                                BindingResult resultado,
                                Model model,
                                Authentication authentication) {
        Usuario contador = usuarioActual(authentication);
        Oferta oferta = ofertaDelContador(id, contador);
        if (!ofertaEditable(oferta)) {
            return "redirect:" + inicioDe(contador);
        }
        String mensaje = dto.getMensaje() == null ? "" : dto.getMensaje().trim();
        if (mensaje.isEmpty() && !resultado.hasFieldErrors("mensaje")) {
            resultado.rejectValue("mensaje", "mensaje.vacio", "Escribe un mensaje para el cliente");
        }
        if (!resultado.hasFieldErrors("fechaEntregaEstimada") && !OfertaService.fechaEntregaValida(dto.getFechaEntregaEstimada())) {
            resultado.rejectValue("fechaEntregaEstimada", "fecha.invalida", "Indica una fecha estimada de entrega futura y de máximo 120 días");
        }
        if (resultado.hasErrors()) {
            // se vuelve al mismo formulario con los errores de validacion
            model.addAttribute("oferta", oferta);
            model.addAttribute("volver", inicioDe(contador));
            model.addAttribute("erroresOferta", resultado.getFieldErrors().stream()
                    .map(e -> "valorPropuesto".equals(e.getField()) && e.isBindingFailure()
                            ? NuevaOfertaDTO.MENSAJE_VALOR : e.getDefaultMessage())
                    .distinct().toList());
            return "oferta-editar";
        }
        ofertaService.editar(id, mensaje, dto.getValorPropuesto(), dto.getFechaEntregaEstimada());
        notificacionService.notificar(oferta.getSolicitud().getPersona(),
                contador.getNombre() + " actualizó su oferta para \"" + oferta.getSolicitud().getTitulo() + "\"",
                "/solicitudes/" + oferta.getSolicitud().getId());
        return "redirect:" + inicioDe(contador) + "?ofertaEditada";
    }

    @PostMapping("/ofertas/{id}/retirar")
    public String retirarOferta(@PathVariable Long id, Authentication authentication) {
        Usuario contador = usuarioActual(authentication);
        Oferta oferta = ofertaDelContador(id, contador);
        if (ofertaEditable(oferta)) {
            ofertaService.actualizarEstado(id, EstadoOferta.RETIRADA);
            notificacionService.notificar(oferta.getSolicitud().getPersona(),
                    contador.getNombre() + " retiró su oferta para \"" + oferta.getSolicitud().getTitulo() + "\"",
                    "/solicitudes/" + oferta.getSolicitud().getId());
        }
        return "redirect:" + inicioDe(contador) + "?ofertaRetirada";
    }
}
