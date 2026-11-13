package com.example.declaraya.controllers;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.IndicadorDian;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.IndicadorDianRepository;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.DatosService;
import com.example.declaraya.services.DisputaService;
import com.example.declaraya.services.NotificacionService;
import com.example.declaraya.services.OfertaService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DisputaService disputaService;

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private DatosService datosService;

    @Autowired
    private SolicitudDeclaracionService solicitudService;

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private IndicadorDianRepository indicadorRepository;

    @Value("${declaraya.powerbi.url:}")
    private String powerBiUrl;

    private String codificar(String mensaje) {
        return URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- paneles

    @GetMapping("/datos")
    public String datos(Model model) {
        long personas = datosService.personasNaturales();
        long publicos = datosService.verificados(Rol.CONTADOR_PUBLICO);
        long practicantes = datosService.verificados(Rol.CONTADOR_PRACTICANTE);
        long maximo = Math.max(personas, Math.max(publicos, practicantes));

        model.addAttribute("personas", personas);
        model.addAttribute("publicos", publicos);
        model.addAttribute("practicantes", practicantes);
        model.addAttribute("publicosPendientes", datosService.pendientes(Rol.CONTADOR_PUBLICO));
        model.addAttribute("practicantesPendientes", datosService.pendientes(Rol.CONTADOR_PRACTICANTE));
        model.addAttribute("porcPersonas", datosService.porcentaje(personas, maximo));
        model.addAttribute("porcPublicos", datosService.porcentaje(publicos, maximo));
        model.addAttribute("porcPracticantes", datosService.porcentaje(practicantes, maximo));

        model.addAttribute("ingresosTotales", acuerdoService.totalComisiones());
        model.addAttribute("ingresosFinalizados", datosService.ingresos(EstadoAcuerdo.FINALIZADO));
        model.addAttribute("ingresosActivos", datosService.ingresos(EstadoAcuerdo.ACTIVO));
        model.addAttribute("totalAcuerdos", acuerdoService.listarTodos().size());

        model.addAttribute("indicadores", indicadorRepository.findAll().stream()
                .sorted(Comparator.comparing(IndicadorDian::getAnio, Comparator.reverseOrder())
                        .thenComparing(IndicadorDian::getCategoria)
                        .thenComparing(IndicadorDian::getConcepto))
                .collect(Collectors.toList()));

        model.addAttribute("powerBiUrl", powerBiUrl);
        return "admin-datos";
    }

    @GetMapping("/inicio")
    public String inicioAdmin(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("contadoresPendientes", usuarioService.listarNoVerificados());
        model.addAttribute("disputas", disputaService.listarAbiertas());
        model.addAttribute("acuerdos", acuerdoService.listarTodos());
        model.addAttribute("totalComisiones", acuerdoService.totalComisiones());
        return "inicio-admin";
    }

    @PostMapping("/usuarios/{id}/verificar")
    public String verificarContador(@PathVariable Long id, @RequestParam(required = false) String volver) {
        usuarioService.verificar(id);
        return "usuarios".equals(volver) ? "redirect:/admin/usuarios?verificado" : "redirect:/admin/inicio";
    }

    @PostMapping("/disputas/{id}/resolver")
    public String resolverDisputa(@PathVariable Long id) {
        disputaService.resolver(id);
        return "redirect:/admin/inicio";
    }

    // ---------------------------------------------------------------- usuarios

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("roles", Rol.values());
        model.addAttribute("mentores", usuarioService.listarPorRol(Rol.CONTADOR_PUBLICO));
        return "admin-usuarios";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String nombre,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam Rol rol,
                               @RequestParam(required = false) String tarjetaProfesional,
                               @RequestParam(defaultValue = "false") boolean verificado,
                               @RequestParam(required = false) Long mentorId,
                               @RequestParam(required = false) BigDecimal tarifaBase) {
        try {
            usuarioService.crearPorAdmin(nombre, email, password, rol, tarjetaProfesional, verificado, mentorId, tarifaBase);
            return "redirect:/admin/usuarios?creado";
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/usuarios?error=" + codificar(e.getMessage());
        }
    }

    @GetMapping("/usuarios/{id}/editar")
    public String formularioUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("u", usuarioService.buscarPorId(id));
        model.addAttribute("roles", Rol.values());
        model.addAttribute("mentores", usuarioService.listarPorRol(Rol.CONTADOR_PUBLICO));
        return "admin-usuario-editar";
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Long id,
                                @RequestParam String nombre,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                @RequestParam Rol rol,
                                @RequestParam(required = false) String tarjetaProfesional,
                                @RequestParam(defaultValue = "false") boolean verificado,
                                @RequestParam(required = false) Long mentorId,
                                @RequestParam(required = false) BigDecimal tarifaBase) {
        try {
            usuarioService.actualizarPorAdmin(id, nombre, email, password, rol, tarjetaProfesional, verificado, mentorId, tarifaBase);
            return "redirect:/admin/usuarios?actualizado";
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/usuarios/" + id + "/editar?error=" + codificar(e.getMessage());
        }
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario.getEmail().equals(authentication.getName())) {
            return "redirect:/admin/usuarios?error=" + codificar("No puedes eliminar tu propia cuenta");
        }
        try {
            notificacionService.eliminarDe(usuario);
            usuarioService.eliminar(id);
            return "redirect:/admin/usuarios?eliminado";
        } catch (DataIntegrityViolationException e) {
            return "redirect:/admin/usuarios?error="
                    + codificar("No se puede eliminar: tiene solicitudes, ofertas o acuerdos asociados");
        }
    }

    // ------------------------------------------------------------- solicitudes

    @GetMapping("/solicitudes")
    public String solicitudes(Model model) {
        model.addAttribute("solicitudes", solicitudService.listarTodas());
        model.addAttribute("estados", EstadoSolicitud.values());
        return "admin-solicitudes";
    }

    @PostMapping("/solicitudes/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoSolicitud estado) {
        solicitudService.actualizarEstado(id, estado);
        return "redirect:/admin/solicitudes?actualizada";
    }

    @PostMapping("/solicitudes/{id}/eliminar")
    public String eliminarSolicitud(@PathVariable Long id) {
        try {
            SolicitudDeclaracion solicitud = solicitudService.buscarPorId(id);
            ofertaService.eliminarPorSolicitud(solicitud);
            solicitudService.eliminar(id);
            return "redirect:/admin/solicitudes?eliminada";
        } catch (DataIntegrityViolationException e) {
            return "redirect:/admin/solicitudes?error="
                    + codificar("No se puede eliminar: la solicitud ya tiene un acuerdo asociado");
        }
    }

    // ------------------------------------------------------- exportar a Power BI

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor.replace("\"", "\"\"");
        boolean comillas = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        return comillas ? "\"" + v + "\"" : v;
    }

    private static String txt(Object o) {
        return o == null ? "" : o.toString();
    }

    // numeros sin decimales ni separadores para que Power BI los lea igual en cualquier idioma
    private static String num(BigDecimal n) {
        return n == null ? "" : n.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String fecha(LocalDateTime f) {
        return f == null ? "" : f.toLocalDate().toString();
    }

    private static String anio(LocalDateTime f) {
        return f == null ? "" : String.valueOf(f.getYear());
    }

    private static String mes(LocalDateTime f) {
        return f == null ? "" : String.valueOf(f.getMonthValue());
    }

    private List<String[]> filasUsuarios() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "nombre", "email", "rol", "tarjeta_profesional", "verificado", "mentor", "tarifa_base"});
        for (Usuario u : usuarioService.listarTodos()) {
            filas.add(new String[]{txt(u.getId()), u.getNombre(), u.getEmail(), u.getRol().getEtiqueta(),
                    txt(u.getTarjetaProfesional()), u.isVerificado() ? "Sí" : "No",
                    u.getMentor() == null ? "" : u.getMentor().getNombre(), num(u.getTarifaBase())});
        }
        return filas;
    }

    private List<String[]> filasSolicitudes() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "tipo", "anio_gravable", "urgencia", "presupuesto_maximo", "estado", "cliente",
                "fecha", "anio", "mes", "situaciones"});
        for (SolicitudDeclaracion s : solicitudService.listarTodas()) {
            filas.add(new String[]{txt(s.getId()), s.getTitulo(), txt(s.getAnioGravable()), txt(s.getUrgencia()),
                    num(s.getPresupuestoMaximo()), s.getEstado().name(), s.getPersona().getNombre(),
                    fecha(s.getFechaCreacion()), anio(s.getFechaCreacion()), mes(s.getFechaCreacion()),
                    String.join(" | ", s.getSituacionesLista())});
        }
        return filas;
    }

    private List<String[]> filasOfertas() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "solicitud_id", "tipo", "contador", "rol_contador", "valor_propuesto", "estado", "fecha"});
        for (Oferta o : ofertaService.listarTodas()) {
            filas.add(new String[]{txt(o.getId()), txt(o.getSolicitud().getId()), o.getSolicitud().getTitulo(),
                    o.getContador().getNombre(), o.getContador().getRol().getEtiqueta(), num(o.getValorPropuesto()),
                    o.getEstado().name(), fecha(o.getFechaCreacion())});
        }
        return filas;
    }

    private List<String[]> filasAcuerdos() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "tipo", "anio_gravable", "urgencia", "cliente", "contador", "rol_contador",
                "valor_acuerdo", "comision_plataforma", "estado", "fecha", "anio", "mes"});
        for (Acuerdo a : acuerdoService.listarTodos()) {
            Oferta o = a.getOferta();
            SolicitudDeclaracion s = o.getSolicitud();
            filas.add(new String[]{txt(a.getId()), s.getTitulo(), txt(s.getAnioGravable()), txt(s.getUrgencia()),
                    s.getPersona().getNombre(), o.getContador().getNombre(), o.getContador().getRol().getEtiqueta(),
                    num(o.getValorPropuesto()), num(a.getComisionPlataforma()), a.getEstado().name(),
                    fecha(a.getFechaCreacion()), anio(a.getFechaCreacion()), mes(a.getFechaCreacion())});
        }
        return filas;
    }

    private List<String[]> filasIndicadores() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "anio", "periodo", "categoria", "concepto", "valor", "unidad", "fuente", "nota"});
        for (IndicadorDian i : indicadorRepository.findAll()) {
            filas.add(new String[]{txt(i.getId()), txt(i.getAnio()), i.getPeriodo(), i.getCategoria(),
                    i.getConcepto(), txt(i.getValor()), i.getUnidad(), i.getFuente(), i.getNota()});
        }
        return filas;
    }

    @GetMapping("/exportar/{tabla}")
    public ResponseEntity<byte[]> exportar(@PathVariable String tabla) {
        List<String[]> filas;
        if ("usuarios".equals(tabla)) {
            filas = filasUsuarios();
        } else if ("solicitudes".equals(tabla)) {
            filas = filasSolicitudes();
        } else if ("ofertas".equals(tabla)) {
            filas = filasOfertas();
        } else if ("acuerdos".equals(tabla)) {
            filas = filasAcuerdos();
        } else if ("indicadores_dian".equals(tabla)) {
            filas = filasIndicadores();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabla no disponible");
        }
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (String[] fila : filas) {
            List<String> celdas = new ArrayList<>();
            for (String celda : fila) {
                celdas.add(csv(celda));
            }
            sb.append(String.join(",", celdas)).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"declaraya_" + tabla + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
