package com.example.declaraya.controllers;

import com.example.declaraya.dto.NuevaSolicitudDTO;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.OfertaService;
import com.example.declaraya.services.SolicitudDeclaracionService;
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
public class SolicitudController {

    private static final List<String> TIPOS_DECLARACION = List.of(
            "Declaración de renta",
            "Declaración de IVA",
            "Retención en la fuente",
            "Impuesto de industria y comercio (ICA)",
            "Régimen Simple de Tributación",
            "Información exógena",
            "Actualización o inscripción del RUT",
            "Corrección de una declaración",
            "Solicitud de devolución de saldo a favor",
            "Respuesta a requerimiento de la DIAN",
            "Asesoría tributaria");

    private static final List<Integer> ANIOS_GRAVABLES = List.of(2026, 2025, 2024, 2023, 2022);

    private static final List<String> URGENCIAS = List.of("Sin afán", "Normal", "Urgente");

    private static final List<String> SITUACIONES = List.of(
            "Empleado con contrato laboral",
            "Trabajador independiente",
            "Dueño de empresa o negocio",
            "Ingresos por arriendos",
            "Inversiones, CDT o acciones",
            "Crédito hipotecario o de vivienda",
            "Dependientes económicos",
            "Bienes o cuentas en el exterior",
            "Primera vez que declaro");

    @Autowired
    private SolicitudDeclaracionService solicitudService;

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private NotificacionService notificacionService;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private SolicitudDeclaracion solicitudDeLaPersona(Long id, Usuario persona){
        SolicitudDeclaracion solicitud = solicitudService.buscarPorId(id);
        if (!solicitud.getPersona().getId().equals(persona.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta solicitud no es tuya");
        }
        return solicitud;
    }

    private void datosFormulario(Model model, String accion, String titulo){
        model.addAttribute("accion", accion);
        model.addAttribute("tituloPagina", titulo);
        model.addAttribute("opciones", TIPOS_DECLARACION);
        model.addAttribute("anios", ANIOS_GRAVABLES);
        model.addAttribute("urgencias", URGENCIAS);
        model.addAttribute("situacionesOpciones", SITUACIONES);
    }

    private void validarTipo(NuevaSolicitudDTO dto, BindingResult resultado){
        if (dto.getTitulo() != null && !dto.getTitulo().isBlank() && !TIPOS_DECLARACION.contains(dto.getTitulo())) {
            resultado.rejectValue("titulo", "titulo.invalido", "Selecciona una opción valida");
        }
        if (dto.getAnioGravable() != null && !ANIOS_GRAVABLES.contains(dto.getAnioGravable())) {
            resultado.rejectValue("anioGravable", "anio.invalido", "Selecciona un año valido");
        }
        if (dto.getUrgencia() != null && !dto.getUrgencia().isBlank() && !URGENCIAS.contains(dto.getUrgencia())) {
            resultado.rejectValue("urgencia", "urgencia.invalida", "Selecciona una urgencia valida");
        }
        if (dto.getSituaciones() != null) {
            dto.setSituaciones(dto.getSituaciones().stream().filter(SITUACIONES::contains).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        }
    }

    @GetMapping("/cliente/inicio")
    public String inicioCliente(Model model, Authentication authentication) {
        Usuario persona = usuarioActual(authentication);
        model.addAttribute("usuario", persona);
        List<SolicitudDeclaracion> lista = solicitudService.listarPorPersona(persona);
        model.addAttribute("solicitudes", lista);
        model.addAttribute("totalSolicitudes", lista.size());
        model.addAttribute("solicitudesAbiertas", lista.stream().filter(x -> x.getEstado() == EstadoSolicitud.ABIERTA).count());
        model.addAttribute("solicitudesEnProceso", lista.stream().filter(x -> x.getEstado() == EstadoSolicitud.EN_PROCESO).count());
        long ofertasPendientes = 0;
        for (SolicitudDeclaracion x : lista) {
            if (x.getEstado() == EstadoSolicitud.ABIERTA) {
                ofertasPendientes += ofertaService.listarPorSolicitud(x).stream()
                        .filter(o -> o.getEstado() == EstadoOferta.PENDIENTE).count();
            }
        }
        model.addAttribute("ofertasPorRevisar", ofertasPendientes);
        return "inicio-cliente";
    }

    @GetMapping("/cliente/solicitudes/nueva")
    public String formularioNuevaSolicitud(Model model) {
        model.addAttribute("solicitud", new NuevaSolicitudDTO());
        datosFormulario(model, "/cliente/solicitudes/nueva", "Nueva solicitud de declaracion");
        return "solicitud-nueva";
    }

    @PostMapping("/cliente/solicitudes/nueva")
    public String crearSolicitud(@Valid @ModelAttribute("solicitud") NuevaSolicitudDTO dto,
                                  BindingResult resultado,
                                  Model model,
                                  Authentication authentication) {
        validarTipo(dto, resultado);
        if (resultado.hasErrors()) {
            datosFormulario(model, "/cliente/solicitudes/nueva", "Nueva solicitud de declaracion");
            return "solicitud-nueva";
        }
        solicitudService.crear(usuarioActual(authentication), dto);
        return "redirect:/cliente/inicio";
    }

    @GetMapping("/cliente/solicitudes/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model, Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudDeLaPersona(id, usuarioActual(authentication));
        if (solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            return "redirect:/solicitudes/" + id;
        }
        NuevaSolicitudDTO dto = new NuevaSolicitudDTO();
        dto.setTitulo(solicitud.getTitulo());
        dto.setDescripcion(solicitud.getDescripcion());
        if (solicitud.getAnioGravable() != null) {
            dto.setAnioGravable(solicitud.getAnioGravable());
        }
        if (solicitud.getUrgencia() != null) {
            dto.setUrgencia(solicitud.getUrgencia());
        }
        dto.setPresupuestoMaximo(solicitud.getPresupuestoMaximo());
        dto.setSituaciones(new java.util.ArrayList<>(solicitud.getSituacionesLista()));
        model.addAttribute("solicitud", dto);
        datosFormulario(model, "/cliente/solicitudes/" + id + "/editar", "Editar solicitud");
        return "solicitud-nueva";
    }
    

    @PostMapping("/cliente/solicitudes/{id}/editar")
    public String editarSolicitud(@PathVariable Long id, @Valid @ModelAttribute("solicitud") NuevaSolicitudDTO dto, BindingResult resultado, Model model, Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudDeLaPersona(id, usuarioActual(authentication));
        if (solicitud.getEstado() != EstadoSolicitud.ABIERTA) {
            return "redirect:/solicitudes/" + id;

        }
        validarTipo(dto, resultado);
        if (resultado.hasErrors()) {
            datosFormulario(model, "/cliente/solicitudes/" + id + "/editar", "Editar solicitud");
            return "solicitud-nueva";
        }
        solicitudService.editar(id, dto);
        for (Oferta o : ofertaService.listarPorSolicitud(solicitud)) {
            if (o.getEstado() == EstadoOferta.PENDIENTE) {
                notificacionService.notificar(o.getContador(),
                        "La solicitud \"" + dto.getTitulo() + "\" fue actualizada. Revisa si tu oferta sigue siendo adecuada",
                        "/solicitudes/" + id);
            }
        }
        return "redirect:/cliente/inicio?editada";
    }

    // cierra la solicitud sin borrarla: queda en el historial y las ofertas pendientes se rechazan
    @PostMapping("/cliente/solicitudes/{id}/cancelar")
    public String cancelarSolicitud(@PathVariable Long id, Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudDeLaPersona(id, usuarioActual(authentication));
        if (solicitud.getEstado() == EstadoSolicitud.ABIERTA) {
            for (Oferta o : ofertaService.listarPorSolicitud(solicitud)) {
                if (o.getEstado() == EstadoOferta.PENDIENTE) {
                    ofertaService.actualizarEstado(o.getId(), EstadoOferta.RECHAZADA);
                    notificacionService.notificar(o.getContador(),
                            "La solicitud \"" + solicitud.getTitulo() + "\" fue cancelada por el cliente",
                            "/solicitudes/" + id);
                }
            }
            solicitudService.actualizarEstado(id, EstadoSolicitud.CERRADA);
        }
        return "redirect:/cliente/inicio?cancelada";
    }

    @PostMapping("/cliente/solicitudes/{id}/eliminar")
    public String eliminarSolicitud(@PathVariable Long id, Authentication authentication) {
        SolicitudDeclaracion solicitud = solicitudDeLaPersona(id, usuarioActual(authentication));
        if (solicitud.getEstado() == EstadoSolicitud.ABIERTA) {
            ofertaService.eliminarPorSolicitud(solicitud);
            solicitudService.eliminar(id);
        }
        
        return "redirect:/cliente/inicio";
    }
    
    

    @GetMapping("/solicitudes/{id}")
    public String verSolicitud(@PathVariable Long id, Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        SolicitudDeclaracion solicitud = solicitudService.buscarPorId(id);
        boolean esDueno = solicitud.getPersona().getId().equals(usuario.getId());
        
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("esDueno", esDueno);
        model.addAttribute("ofertas", esDueno ? ofertaService.listarPorSolicitud(solicitud) : List.of());
        return "solicitud-detalle";
    }
}
