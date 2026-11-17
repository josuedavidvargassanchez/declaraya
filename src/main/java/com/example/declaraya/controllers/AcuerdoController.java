package com.example.declaraya.controllers;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoSupervision;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Archivo;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.Mensaje;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.ArchivoService;
import com.example.declaraya.services.ChatService;
import com.example.declaraya.services.DisputaService;
import com.example.declaraya.services.FlujoAcuerdoService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AcuerdoController {

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DisputaService disputaService;

    @Autowired
    private ArchivoService archivoService;

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private FlujoAcuerdoService flujoService;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private Acuerdo acuerdoVisible(Long id, Usuario usuario) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(id);
        if (!acuerdoService.puedeVer(acuerdo, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este acuerdo");
        }
        return acuerdo;
    }

    private Acuerdo acuerdoDeParticipante(Long id, Usuario usuario) {
        Acuerdo acuerdo = acuerdoService.buscarPorId(id);
        if (!acuerdoService.esParticipante(acuerdo, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los participantes pueden hacer esto");
        }
        return acuerdo;
    }

    private boolean bloqueadoPorMentor(Acuerdo acuerdo) {
        return supervisionService.buscarPorAcuerdo(acuerdo)
                .map(s -> s.getEstado() != EstadoSupervision.APROBADO)
                .orElse(false);
    }

    private String irAlChat(Long id, String parametro) {
        return "redirect:/acuerdos/" + id + "/chat" + (parametro == null ? "" : "?" + parametro);
    }

    // atajo comodo desde la vista de la solicitud: ir directo al chat de una oferta aceptada
    @GetMapping("/ofertas/{ofertaId}/chat")
    public String irAlChatDeOferta(@PathVariable Long ofertaId) {
        Acuerdo acuerdo = acuerdoService.buscarPorOfertaId(ofertaId);
        return irAlChat(acuerdo.getId(), null);
    }

    @GetMapping("/acuerdos/{id}/chat")
    public String verChat(@PathVariable Long id, Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Acuerdo acuerdo = acuerdoVisible(id, usuario);
        Chat chat = chatService.buscarPorAcuerdoId(acuerdo.getId());
        List<Mensaje> mensajes = chatService.listarMensajes(chat);

        Map<Long, List<Archivo>> archivosPorMensaje = new HashMap<>();
        for (Mensaje m : mensajes) {
            archivosPorMensaje.put(m.getId(), archivoService.listarPorMensaje(m));
        }

        boolean esParticipante = acuerdoService.esParticipante(acuerdo, usuario);
        boolean bloqueado = bloqueadoPorMentor(acuerdo);
        boolean disputaActiva = disputaService.hayDisputaAbierta(acuerdo);
        Usuario persona = acuerdo.getOferta().getSolicitud().getPersona();

        model.addAttribute("acuerdo", acuerdo);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("archivosPorMensaje", archivosPorMensaje);
        model.addAttribute("esParticipante", esParticipante);
        model.addAttribute("esPersona", usuario.getId().equals(persona.getId()));
        model.addAttribute("bloqueadoPorMentor", bloqueado);
        model.addAttribute("chatHabilitado", esParticipante && puedeEscribir(acuerdo, bloqueado, disputaActiva));
        model.addAttribute("disputaAbierta", disputaActiva);
        // "Finalizar" solo aparece cuando hay una entrega enviada al cliente (equivale a confirmar la recepcion)
        model.addAttribute("finalizarHabilitado", esParticipante && acuerdo.getEstado() == EstadoAcuerdo.ACTIVO
                && flujoService.entregaEnviada(acuerdo) != null);
        return "acuerdo-chat";
    }

    // el chat normal exige el acuerdo activo; mientras haya una disputa activa las partes
    // tambien pueden escribir para responder a la administracion (aunque el acuerdo este finalizado)
    private boolean puedeEscribir(Acuerdo acuerdo, boolean bloqueado, boolean disputaActiva) {
        return disputaActiva || (!bloqueado && acuerdo.getEstado() == EstadoAcuerdo.ACTIVO);
    }

    @PostMapping("/acuerdos/{id}/chat/mensajes")
    public String enviarMensaje(@PathVariable Long id,
                                 @RequestParam(required = false) String contenido,
                                 @RequestParam(required = false) MultipartFile archivo,
                                 Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Acuerdo acuerdo = acuerdoDeParticipante(id, usuario);

        if (!puedeEscribir(acuerdo, bloqueadoPorMentor(acuerdo), disputaService.hayDisputaAbierta(acuerdo))) {
            return irAlChat(id, "errorChat");
        }

        boolean hayArchivo = archivo != null && !archivo.isEmpty();
        boolean hayTexto = contenido != null && !contenido.isBlank();
        if (!hayArchivo && !hayTexto) {
            return irAlChat(id, null);
        }
        if (hayTexto && contenido.length() > 1000) {
            return irAlChat(id, "errorChat");
        }

        if (hayArchivo) {
            try {
                archivoService.validar(archivo);
            } catch (ArchivoService.ArchivoNoPermitidoException e) {
                return irAlChat(id, "errorArchivo");
            }
        }

        Chat chat = chatService.buscarPorAcuerdoId(acuerdo.getId());
        Mensaje mensaje = chatService.enviarMensaje(chat, usuario, hayTexto ? contenido.trim() : "Archivo adjunto");

        if (hayArchivo) {
            try {
                archivoService.guardar(mensaje, archivo);
            } catch (IOException e) {
                return irAlChat(id, "errorArchivo");
            }
        }
        return irAlChat(id, null);
    }

    // solo la persona finaliza, y solo si hay una entrega ENVIADA: hace lo mismo que "Confirmar recepcion"
    // (los acuerdos ya finalizados no cambian)
    @PostMapping("/acuerdos/{id}/finalizar")
    public String finalizar(@PathVariable Long id,
                            @RequestParam(defaultValue = "false") boolean confirmado,
                            Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Acuerdo acuerdo = acuerdoDeParticipante(id, usuario);
        Usuario persona = acuerdo.getOferta().getSolicitud().getPersona();

        if (!usuario.getId().equals(persona.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo la persona puede finalizar el acuerdo");
        }
        EntregaTrabajo enviada = flujoService.entregaEnviada(acuerdo);
        if (acuerdo.getEstado() != EstadoAcuerdo.ACTIVO || enviada == null) {
            return irAlChat(id, "errorFinalizar");
        }
        try {
            flujoService.confirmarRecepcion(id, enviada.getId(), usuario, confirmado);
        } catch (ReglaNegocioException e) {
            return irAlChat(id, "errorFinalizar");
        }
        return "redirect:/acuerdos/" + id + "?recepcionConfirmada";
    }

    @PostMapping("/acuerdos/{id}/disputa")
    public String abrirDisputa(@PathVariable Long id,
                                @RequestParam String motivo,
                                Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Acuerdo acuerdo = acuerdoDeParticipante(id, usuario);

        if (motivo != null && !motivo.isBlank() && !disputaService.hayDisputaAbierta(acuerdo)) {
            String limpio = motivo.trim();
            disputaService.abrir(acuerdo, usuario, limpio.length() > 500 ? limpio.substring(0, 500) : limpio);
        }
        return irAlChat(id, null);
    }

    @GetMapping("/acuerdos/{id}/documento")
    public String documento(@PathVariable Long id, Model model, Authentication authentication) {
        Acuerdo acuerdo = acuerdoVisible(id, usuarioActual(authentication));
        model.addAttribute("acuerdo", acuerdo);
        return "acuerdo-documento";
    }
}
