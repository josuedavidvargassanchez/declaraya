package com.example.declaraya.config;

import com.example.declaraya.dto.NuevaSolicitudDTO;
import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.OfertaRepository;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;
import com.example.declaraya.services.AcuerdoService;
import com.example.declaraya.services.DisputaService;
import com.example.declaraya.services.OfertaService;
import com.example.declaraya.services.SolicitudDeclaracionService;
import com.example.declaraya.services.SupervisionService;
import com.example.declaraya.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Datos de ejemplo FICTICIOS para probar y para el dashboard de Power BI:
 * contadores, practicantes, clientes, solicitudes, ofertas y acuerdos repartidos en 2026.
 * Se cargan una sola vez. Para desactivarlos: declaraya.demo-data=false
 * Todas las cuentas de ejemplo usan la clave "demo1234".
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final String MARCADOR = "mauricio.salazar@example.com";
    private static final String CLAVE_DEMO = "demo1234";

    // titulo, valor minimo, valor maximo (COP), peso de frecuencia
    private static final Object[][] TRAMITES = {
            {"Declaración de renta", 150000L, 600000L, 4},
            {"Declaración de IVA", 300000L, 900000L, 3},
            {"Retención en la fuente", 200000L, 500000L, 2},
            {"Impuesto de industria y comercio (ICA)", 250000L, 700000L, 1},
            {"Régimen Simple de Tributación", 300000L, 800000L, 2},
            {"Información exógena", 600000L, 1500000L, 1},
            {"Actualización o inscripción del RUT", 60000L, 150000L, 2},
            {"Corrección de una declaración", 200000L, 500000L, 1},
            {"Solicitud de devolución de saldo a favor", 300000L, 800000L, 1},
            {"Respuesta a requerimiento de la DIAN", 400000L, 1200000L, 1},
            {"Asesoría tributaria", 150000L, 400000L, 2}
    };

    private static final String[] URGENCIAS = {"Normal", "Normal", "Normal", "Urgente", "Sin afán"};

    private static final String[] SITUACIONES = {
            "Empleado con contrato laboral", "Trabajador independiente", "Dueño de empresa o negocio",
            "Ingresos por arriendos", "Inversiones, CDT o acciones", "Crédito hipotecario o de vivienda",
            "Dependientes económicos", "Bienes o cuentas en el exterior", "Primera vez que declaro"
    };

    private static final String[] PUBLICOS = {
            "Mauricio Salazar Pineda", "Claudia Marcela Ortiz", "Andrés Felipe Cárdenas", "Luz Marina Giraldo",
            "Jorge Eduardo Valencia", "Paola Andrea Rincón", "Héctor Fabio Mejía", "Diana Carolina Torres",
            "Ricardo Alonso Peña", "Natalia Quintero Rojas"
    };

    private static final String[] PRACTICANTES = {
            "Camila Herrera", "Sebastián Ruiz", "Valentina Gómez", "Daniel Ospina",
            "Laura Sofía Medina", "Juan Pablo Cortés", "Mariana Londoño", "Esteban Arias"
    };

    private static final String[] PERSONAS = {
            "Carolina Pardo", "Felipe Montoya", "Ana María Duque", "Santiago Bedoya", "Lina Marcela Cruz",
            "Camilo Restrepo", "Daniela Franco", "Iván Darío Suárez", "Paula Andrea Nieto", "Óscar Fabián Lozano",
            "Juliana Cardona", "Brayan Steven Mora", "Tatiana Beltrán", "Nicolás Espinosa", "Viviana Cuesta",
            "Gustavo Adolfo Ríos"
    };

    private static final String[] MENSAJES_OFERTA = {
            "Tengo experiencia con este trámite y puedo entregarlo en pocos días.",
            "Reviso tus soportes, preparo el borrador y te acompaño hasta la presentación.",
            "Trabajo con este tipo de casos todas las semanas. Quedo atento a tus documentos.",
            "Puedo ayudarte de inmediato. Incluye una revisión final antes de presentar.",
            "Te explico cada paso para que entiendas tu situación tributaria.",
            "Ofrezco un precio cerrado y atención por chat durante todo el proceso."
    };

    private static final String[] EXTRAS_DESCRIPCION = {
            "Tengo mis certificados de ingresos y retenciones listos.",
            "Es mi primera vez y no sé por dónde empezar.",
            "El plazo se acerca y necesito ayuda pronto.",
            "Prefiero que me expliquen el proceso por chat.",
            "Tengo dudas sobre qué gastos puedo deducir.",
            "Ya intenté hacerlo yo mismo pero me confundí con los formularios."
    };

    @Value("${declaraya.demo-data:true}")
    private boolean activo;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SolicitudDeclaracionService solicitudService;

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private AcuerdoService acuerdoService;

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private DisputaService disputaService;

    @Autowired
    private SolicitudDeclaracionRepository solicitudRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private AcuerdoRepository acuerdoRepository;

    @Override
    public void run(String... args) {
        if (!activo || usuarioService.buscarPorEmail(MARCADOR).isPresent()) {
            return;
        }
        Random rnd = new Random(2026);

        List<Usuario> publicos = new ArrayList<>();
        for (int i = 0; i < PUBLICOS.length; i++) {
            // el ultimo queda sin verificar para probar el flujo de verificacion
            publicos.add(crearUsuario(PUBLICOS[i], Rol.CONTADOR_PUBLICO, 100 + i, i < PUBLICOS.length - 1, null, rnd));
        }
        List<Usuario> practicantes = new ArrayList<>();
        for (int i = 0; i < PRACTICANTES.length; i++) {
            boolean ultimo = i == PRACTICANTES.length - 1;
            Usuario mentor = ultimo ? null : publicos.get(i % (PUBLICOS.length - 1));
            practicantes.add(crearUsuario(PRACTICANTES[i], Rol.CONTADOR_PRACTICANTE, 200 + i, !ultimo, mentor, rnd));
        }
        List<Usuario> personas = new ArrayList<>();
        for (int i = 0; i < PERSONAS.length; i++) {
            personas.add(crearUsuario(PERSONAS[i], Rol.PERSONA, 0, true, null, rnd));
        }

        List<Usuario> oferentes = new ArrayList<>();
        for (Usuario u : publicos) {
            if (u.isVerificado()) {
                oferentes.add(u);
            }
        }
        for (Usuario u : practicantes) {
            if (u.isVerificado() && u.getMentor() != null) {
                oferentes.add(u);
            }
        }

        Map<Long, Integer> activos = new HashMap<>();
        List<Acuerdo> acuerdos = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            crearCaso(rnd, personas, oferentes, activos, acuerdos);
        }
        crearDisputas(acuerdos);

        System.out.println("Datos de ejemplo cargados: " + (publicos.size() + practicantes.size() + personas.size())
                + " usuarios, 48 solicitudes y " + acuerdos.size() + " acuerdos. Clave de todas las cuentas demo: " + CLAVE_DEMO);
    }

    private Usuario crearUsuario(String nombre, Rol rol, int numero, boolean verificado, Usuario mentor, Random rnd) {
        String tarjeta = rol == Rol.PERSONA ? null : "TP-DEMO-" + String.format("%04d", numero);
        Usuario u = usuarioService.registrar(nombre, correo(nombre), CLAVE_DEMO, rol, tarjeta);
        u.setVerificado(rol == Rol.PERSONA || verificado);
        u.setMentor(mentor);
        if (rol != Rol.PERSONA) {
            u.setTarifaBase(BigDecimal.valueOf((rnd.nextInt(10) + 3) * 50000L));
        }
        return usuarioService.guardar(u);
    }

    private String correo(String nombre) {
        String limpio = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        String[] partes = limpio.split("\\s+");
        String segundo = partes.length > 1 ? "." + partes[1] : "";
        return partes[0] + segundo + "@example.com";
    }

    private Object[] elegirTramite(Random rnd) {
        int total = 0;
        for (Object[] t : TRAMITES) {
            total += (Integer) t[3];
        }
        int tirada = rnd.nextInt(total);
        for (Object[] t : TRAMITES) {
            tirada -= (Integer) t[3];
            if (tirada < 0) {
                return t;
            }
        }
        return TRAMITES[0];
    }

    private LocalDateTime fechaAleatoria(Random rnd, LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        LocalDate dia = desde.plusDays(rnd.nextInt((int) dias + 1));
        return dia.atTime(8 + rnd.nextInt(10), rnd.nextInt(60));
    }

    private long redondear(long valor) {
        return Math.round(valor / 10000.0) * 10000L;
    }

    private List<String> situacionesAleatorias(Random rnd) {
        List<String> todas = new ArrayList<>();
        Collections.addAll(todas, SITUACIONES);
        Collections.shuffle(todas, rnd);
        return new ArrayList<>(todas.subList(0, rnd.nextInt(4)));
    }

    private void crearCaso(Random rnd, List<Usuario> personas, List<Usuario> oferentes,
                           Map<Long, Integer> activos, List<Acuerdo> acuerdos) {
        Object[] tramite = elegirTramite(rnd);
        String titulo = (String) tramite[0];
        long min = (Long) tramite[1];
        long max = (Long) tramite[2];
        boolean esRenta = titulo.startsWith("Declaración de renta");

        LocalDateTime fecha = esRenta
                ? fechaAleatoria(rnd, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 10, 20))
                : fechaAleatoria(rnd, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 11, 10));

        int anio;
        if (esRenta) {
            anio = rnd.nextInt(10) < 8 ? 2025 : (rnd.nextBoolean() ? 2024 : 2023);
        } else if (titulo.startsWith("Declaración de IVA") || titulo.startsWith("Retención")
                || titulo.startsWith("Impuesto de industria") || titulo.startsWith("Régimen Simple")) {
            anio = 2026;
        } else {
            anio = rnd.nextBoolean() ? 2025 : 2026;
        }

        long base = redondear(min + (long) (rnd.nextDouble() * (max - min)));

        NuevaSolicitudDTO dto = new NuevaSolicitudDTO();
        dto.setTitulo(titulo);
        dto.setAnioGravable(anio);
        dto.setUrgencia(URGENCIAS[rnd.nextInt(URGENCIAS.length)]);
        if (rnd.nextInt(10) < 7) {
            dto.setPresupuestoMaximo(BigDecimal.valueOf(redondear((long) (max * (1.0 + rnd.nextDouble() * 0.4)))));
        }
        dto.setSituaciones(situacionesAleatorias(rnd));
        dto.setDescripcion("Necesito ayuda con " + titulo.toLowerCase() + " del año " + anio + ". "
                + EXTRAS_DESCRIPCION[rnd.nextInt(EXTRAS_DESCRIPCION.length)]);

        Usuario persona = personas.get(rnd.nextInt(personas.size()));
        SolicitudDeclaracion solicitud = solicitudService.crear(persona, dto);

        // 0 abierta, 1 en proceso (acuerdo activo), 2 cerrada (acuerdo finalizado), 3 cancelada
        boolean reciente = fecha.isAfter(LocalDateTime.of(2026, 10, 10, 0, 0));
        double tirada = rnd.nextDouble();
        int estado;
        if (reciente) {
            estado = tirada < 0.60 ? 0 : (tirada < 0.85 ? 1 : 2);
        } else {
            estado = tirada < 0.70 ? 2 : (tirada < 0.82 ? 1 : (tirada < 0.92 ? 0 : 3));
        }

        int cantidad = estado == 3 ? rnd.nextInt(3) : 1 + rnd.nextInt(4);
        List<Usuario> candidatos = new ArrayList<>(oferentes);
        Collections.shuffle(candidatos, rnd);
        List<Oferta> ofertas = new ArrayList<>();
        for (int j = 0; j < Math.min(cantidad, candidatos.size()); j++) {
            Usuario c = candidatos.get(j);
            boolean practicante = c.getRol() == Rol.CONTADOR_PRACTICANTE;
            double factor = 0.85 + rnd.nextDouble() * 0.30;
            if (practicante) {
                factor = factor * 0.65;
            }
            Oferta o = ofertaService.crear(solicitud, c, MENSAJES_OFERTA[rnd.nextInt(MENSAJES_OFERTA.length)],
                    BigDecimal.valueOf(redondear((long) (base * factor))));
            o.setFechaCreacion(fecha.plusHours(1 + rnd.nextInt(47)));
            ofertas.add(o);
        }

        Oferta ganadora = null;
        if (estado == 1 || estado == 2) {
            for (Oferta o : ofertas) {
                boolean practicante = o.getContador().getRol() == Rol.CONTADOR_PRACTICANTE;
                boolean ocupado = activos.getOrDefault(o.getContador().getId(), 0) > 0;
                if (!(practicante && estado == 1 && ocupado)) {
                    ganadora = o;
                    break;
                }
            }
            if (ganadora == null) {
                estado = 0;
            }
        }

        for (Oferta o : ofertas) {
            if (estado == 0) {
                o.setEstado(rnd.nextInt(10) == 0 ? EstadoOferta.RETIRADA : EstadoOferta.PENDIENTE);
            } else if (o == ganadora) {
                o.setEstado(EstadoOferta.ACEPTADA);
            } else {
                o.setEstado(EstadoOferta.RECHAZADA);
            }
            ofertaRepository.save(o);
        }

        if (ganadora != null) {
            Usuario contador = ganadora.getContador();
            Acuerdo acuerdo = acuerdoService.crearDesdeOferta(ganadora);
            acuerdo.setFechaCreacion(ganadora.getFechaCreacion().plusDays(1 + rnd.nextInt(3)));
            acuerdo.setEstado(estado == 2 ? EstadoAcuerdo.FINALIZADO : EstadoAcuerdo.ACTIVO);
            acuerdo = acuerdoRepository.save(acuerdo);
            if (contador.getRol() == Rol.CONTADOR_PRACTICANTE && contador.getMentor() != null) {
                Supervision supervision = supervisionService.asignar(contador, contador.getMentor(), acuerdo);
                supervisionService.aprobar(supervision.getId(), contador.getMentor());
            }
            if (estado == 1) {
                activos.merge(contador.getId(), 1, Integer::sum);
            }
            acuerdos.add(acuerdo);
        }

        solicitud.setFechaCreacion(fecha);
        solicitud.setEstado(estado == 0 ? EstadoSolicitud.ABIERTA
                : (estado == 1 ? EstadoSolicitud.EN_PROCESO : EstadoSolicitud.CERRADA));
        solicitudRepository.save(solicitud);
    }

    private void crearDisputas(List<Acuerdo> acuerdos) {
        if (acuerdos.size() < 4) {
            return;
        }
        Acuerdo reciente = acuerdos.get(acuerdos.size() - 1);
        disputaService.abrir(reciente, reciente.getOferta().getSolicitud().getPersona(),
                "El contador no ha respondido en varios días y se acerca la fecha límite.");
        Acuerdo antiguo = acuerdos.get(0);
        Disputa resuelta = disputaService.abrir(antiguo, antiguo.getOferta().getContador(),
                "El cliente no ha enviado los soportes acordados.");
        disputaService.resolver(resuelta.getId());
    }
}
