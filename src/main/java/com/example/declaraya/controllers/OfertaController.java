package com.example.declaraya.controllers;

import com.example.declaraya.dto.NuevaOfertaDTO;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.OfertaService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        model.addAttribute("practicantes", usuarioService.listarPracticantesDe(contador));
        model.addAttribute("practicantesSupervisados", supervisionService.listarPorMentor(contador));
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
        model.addAttribute("mentoresDisponibles", usuarioService.listarMentoresDisponibles());
        model.addAttribute("casosActivos", acuerdoService.contarActivosDe(practicante));
        return "inicio-practicante";
    }

    @PostMapping("/practicante/mentor")
    public String elegirMentor(@RequestParam Long mentorId, Authentication authentication) {
        Usuario practicante = usuarioActual(authentication);
        if (practicante.getMentor() == null) {
            usuarioService.asignarMentor(practicante, mentorId);
        }
        return "redirect:/practicante/inicio";
    }

    // el practicante tambien usa este endpoint; su oferta queda igual, pero el
    // acuerdo que resulte debera pasar por Supervision antes de darse por valido
    @PostMapping("/solicitudes/{id}/ofertas")
    public String enviarOferta(@PathVariable Long id,
                                @ModelAttribute("oferta") NuevaOfertaDTO dto,
                                Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudService.buscarPorId(id);
        Usuario contador = usuarioActual(authentication);
        boolean practicante = contador.getRol() == Rol.CONTADOR_PRACTICANTE;
        String inicio = practicante ? "/practicante/inicio" : "/contador/inicio";

        if (!contador.isVerificado()) {
            return "redirect:" + inicio + "?errorVerificacion";
        }
        if (solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            return "redirect:" + inicio + "?errorSolicitud";
        }
        if (dto.getValorPropuesto() == null || dto.getValorPropuesto().signum() <= 0) {
            return "redirect:" + inicio + "?errorValor";
        }
        boolean yaOferto = ofertaService.listarPorSolicitud(solicitud).stream()
                .anyMatch(o -> o.getContador().getId().equals(contador.getId()) && o.getEstado() != EstadoOferta.RETIRADA);
        if (yaOferto) {
            return "redirect:" + inicio + "?errorDuplicada";
        }
        if (practicante) {
            if (contador.getMentor() == null) {
                return "redirect:" + inicio + "?errorMentor";
            }
            if (acuerdoService.contarActivosDe(contador) > 0) {
                return "redirect:" + inicio + "?errorCasoActivo";
            }
        }

        ofertaService.crear(solicitud, contador, dto.getMensaje(), dto.getValorPropuesto());
        notificacionService.notificar(solicitud.getPersona(),
                "Nueva oferta de " + contador.getNombre() + " para \"" + solicitud.getTitulo() + "\"",
                "/solicitudes/" + solicitud.getId());
        return "redirect:" + inicio + "?ofertaEnviada";
    }

    @PostMapping("/ofertas/{id}/aceptar")
    public String aceptarOferta(@PathVariable Long id, Authentication authentication) {
        Oferta oferta = ofertaDeLaPersona(id, usuarioActual(authentication));
        SolicitudDeclaracion solicitud = oferta.getSolicitud();

        if (oferta.getEstado() != EstadoOferta.PENDIENTE || solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            return "redirect:/solicitudes/" + solicitud.getId();
        }

        oferta = ofertaService.actualizarEstado(id, EstadoOferta.ACEPTADA);
        solicitudService.actualizarEstado(solicitud.getId(), EstadoSolicitud.EN_PROCESO);

        for (Oferta otra : ofertaService.listarPorSolicitud(solicitud)) {
            if (!otra.getId().equals(oferta.getId()) && otra.getEstado() == EstadoOferta.PENDIENTE) {
                ofertaService.actualizarEstado(otra.getId(), EstadoOferta.RECHAZADA);
                notificacionService.notificar(otra.getContador(),
                        "Tu oferta para \"" + solicitud.getTitulo() + "\" no fue seleccionada",
                        "/solicitudes/" + solicitud.getId());
            }
        }

        Acuerdo acuerdo = acuerdoService.crearDesdeOferta(oferta);
        Usuario contador = oferta.getContador();
        notificacionService.notificar(contador,
                "¡Tu oferta para \"" + solicitud.getTitulo() + "\" fue aceptada! Ya puedes chatear con el cliente",
                "/acuerdos/" + acuerdo.getId() + "/chat");
        if (contador.getRol() == Rol.CONTADOR_PRACTICANTE && contador.getMentor() != null) {
            supervisionService.asignar(contador, contador.getMentor(), acuerdo);
        }
        return "redirect:/solicitudes/" + solicitud.getId();
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
                                @ModelAttribute("ofertaForm") NuevaOfertaDTO dto,
                                Authentication authentication) {
        Usuario contador = usuarioActual(authentication);
        Oferta oferta = ofertaDelContador(id, contador);
        if (!ofertaEditable(oferta)) {
            return "redirect:" + inicioDe(contador);
        }
        if (dto.getValorPropuesto() == null || dto.getValorPropuesto().signum() <= 0) {
            return "redirect:/ofertas/" + id + "/editar?errorValor";
        }
        String mensaje = dto.getMensaje() == null ? "" : dto.getMensaje().trim();
        if (mensaje.isEmpty() || mensaje.length() > 500) {
            return "redirect:/ofertas/" + id + "/editar?errorMensaje";
        }
        ofertaService.editar(id, mensaje, dto.getValorPropuesto());
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
