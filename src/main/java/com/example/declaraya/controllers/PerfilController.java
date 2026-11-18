package com.example.declaraya.controllers;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.declaraya.dto.PerfilDTO;
import com.example.declaraya.enums.TipoDocumento;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.services.CatalogoRequisitos;
import com.example.declaraya.services.FotoPerfilService;
import com.example.declaraya.services.SesionesService;
import com.example.declaraya.services.UsuarioService;
import com.example.declaraya.services.VerificacionService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;



@Controller 
public class PerfilController {
    
    @Autowired 
    private UsuarioService usuarioService;

    @Autowired
    private FotoPerfilService fotoPerfilService;

    @Autowired
    private VerificacionService verificacionService;

    @Autowired
    private SesionesService sesionesService;

    private Usuario usuarioActual(Authentication authentication){
        return usuarioService.buscarPorEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    // atributos que necesita perfil.html (tambien al volver a mostrarlo con errores de validacion)
    private void prepararPerfil(Model model, Usuario usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        model.addAttribute("tramites", CatalogoRequisitos.TRAMITES);
        model.addAttribute("contrasenaMin", UsuarioService.CONTRASENA_MIN);
        if (!model.containsAttribute("perfil")) {
            model.addAttribute("perfil", PerfilDTO.desde(usuario));
        }
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model, Authentication authentication) {
        prepararPerfil(model, usuarioActual(authentication));
        return "perfil";
    }

    // solo se enlazan los campos del DTO; rol, correo, TP y verificacion se ignoran aunque lleguen
    @PostMapping("/perfil/datos")
    public String guardarDatos(@Valid @ModelAttribute("perfil") PerfilDTO perfil,
                               BindingResult resultado,
                               Model model,
                               Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (resultado.hasErrors()) {
            model.addAttribute("errorDatos", true);
            prepararPerfil(model, usuario);
            return "perfil";
        }
        usuarioService.actualizarPerfil(usuario, perfil);
        return "redirect:/perfil?datosActualizados";
    }

    @PostMapping("/perfil/contrasena")
    public String cambiarContrasena(@RequestParam(required = false) String actual,
                                    @RequestParam(required = false) String nueva,
                                    @RequestParam(required = false) String confirmacion,
                                    Authentication authentication,
                                    HttpServletRequest request,
                                    RedirectAttributes redirect) {
        try {
            Usuario usuario = usuarioService.cambiarContrasena(usuarioActual(authentication), actual, nueva, confirmacion);
            // las demas sesiones abiertas con la contraseña anterior se cierran; esta se conserva
            sesionesService.expirarOtras(usuario.getEmail(), request.getSession().getId());
            return "redirect:/perfil?contrasenaActualizada";
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("errorContrasena", e.getMessage());
            return "redirect:/perfil";
        }
    }

    // un contador APROBADO pide volver a ser verificado (confirmado en un modal)
    @PostMapping("/perfil/verificacion/nueva")
    public String solicitarNuevaVerificacion(@RequestParam(required = false) String nuevaTarjeta,
                                             @RequestParam(defaultValue = "false") boolean confirmado,
                                             Authentication authentication,
                                             RedirectAttributes redirect) {
        try {
            verificacionService.solicitarNuevaVerificacion(usuarioActual(authentication), nuevaTarjeta, confirmado);
            return "redirect:/perfil?nuevaVerificacion";
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("errorVerificacion", e.getMessage());
            return "redirect:/perfil";
        }
    }

    @PostMapping("/perfil/foto")
    public String subirFoto(@RequestParam MultipartFile foto, Authentication authentication) {
        if (foto == null || foto.isEmpty()) {
            return "redirect:/perfil?errorFoto";
        }
        try {
            fotoPerfilService.guardar(usuarioActual(authentication), foto);
        } catch (FotoPerfilService.FotoNoPermitidaException | IOException e) {
            return "redirect:/perfil?errorFoto";
        }

        return "redirect:/perfil?fotoActualizada";
    }

    // el contador en PENDIENTE, CORRECCION_SOLICITADA o RECHAZADO envia un soporte nuevo;
    // la solicitud vuelve a PENDIENTE y se avisa a los administradores
    @PostMapping("/perfil/verificacion")
    public String reenviarVerificacion(@RequestParam(required = false) MultipartFile soporteTarjeta,
                                       @RequestParam(required = false) MultipartFile soporteDocumento,
                                       Authentication authentication,
                                       RedirectAttributes redirect) {
        try {
            verificacionService.reenviarSoportes(usuarioActual(authentication), soporteTarjeta, soporteDocumento);
            return "redirect:/perfil?verificacionEnviada";
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("errorVerificacion", e.getMessage());
            return "redirect:/perfil";
        } catch (IOException e) {
            redirect.addFlashAttribute("errorVerificacion", "No se pudo guardar el archivo. Intenta de nuevo.");
            return "redirect:/perfil";
        }
    }

    // solo el propio contador ve aqui sus soportes (el admin los ve desde /admin/verificaciones)
    @GetMapping("/perfil/verificacion/documentos/{tipo}")
    public ResponseEntity<Resource> verMiSoporte(@PathVariable String tipo, Authentication authentication) throws IOException {
        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isContador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los contadores tienen soportes de verificación");
        }
        return AdminVerificacionController.servirDocumento(verificacionService, usuario, tipo);
    }

    @PostMapping("/perfil/foto/eliminar")
    public String eliminarFoto(Authentication authentication) {
        fotoPerfilService.eliminar(usuarioActual(authentication));
        
        return "redirect:/perfil?fotoEliminada";
    }

    //cualquier usuario puede ver la foto del otro
    @GetMapping("/usuarios/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) throws IOException {
        Usuario usuario = usuarioService.buscarPorId(id);
        Resource recurso = fotoPerfilService.cargar(usuario);
        return ResponseEntity.ok().contentType(fotoPerfilService.tipoDe(usuario)).cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate()).body(recurso);
    }
    
    
    

}
