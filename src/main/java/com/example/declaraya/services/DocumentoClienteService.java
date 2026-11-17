package com.example.declaraya.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.declaraya.enums.EstadoRequerimiento;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.enums.TipoDocumentoCliente;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.exceptions.ReglaNegocioException;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.RequerimientoDocumento;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import com.example.declaraya.repositories.DocumentoClienteRepository;
import com.example.declaraya.repositories.RequerimientoDocumentoRepository;

/**
 * Documentos personales y tributarios del cliente ("Mis documentos").
 * Reutiliza la validacion y el almacenamiento de ArchivoService (PDF/JPG/PNG, 10 MB, nombres UUID).
 */
@Service
public class DocumentoClienteService {

    public static final int NOMBRE_MAX = 120;

    private final DocumentoClienteRepository documentoRepository;
    private final ArchivoService archivoService;
    private final AcuerdoRepository acuerdoRepository;
    private final SupervisionService supervisionService;
    private final DisputaService disputaService;
    private final SolicitudDeclaracionService solicitudService;
    private final RequerimientoDocumentoRepository requerimientoRepository;
    private final ActividadAcuerdoService actividadService;
    private final NotificacionService notificacionService;
    private final CatalogoRequisitos catalogo;

    public DocumentoClienteService(DocumentoClienteRepository documentoRepository, ArchivoService archivoService,
                                   AcuerdoRepository acuerdoRepository, SupervisionService supervisionService,
                                   DisputaService disputaService, SolicitudDeclaracionService solicitudService,
                                   RequerimientoDocumentoRepository requerimientoRepository,
                                   ActividadAcuerdoService actividadService, NotificacionService notificacionService,
                                   CatalogoRequisitos catalogo) {
        this.documentoRepository = documentoRepository;
        this.archivoService = archivoService;
        this.acuerdoRepository = acuerdoRepository;
        this.supervisionService = supervisionService;
        this.disputaService = disputaService;
        this.solicitudService = solicitudService;
        this.requerimientoRepository = requerimientoRepository;
        this.actividadService = actividadService;
        this.notificacionService = notificacionService;
        this.catalogo = catalogo;
    }

    // ------------------------------------------------------------ permisos

    /**
     * Unica regla de acceso para ver o descargar un documento. True solo si el usuario es el
     * propietario, o el contador o el mentor de un acuerdo de la solicitud vinculada, o un ADMIN
     * con una disputa activa sobre ese acuerdo.
     */
    public boolean puedeVer(DocumentoCliente documento, Usuario usuario) {
        if (documento == null || usuario == null) {
            return false;
        }
        if (documento.getPropietario().getId().equals(usuario.getId())) {
            return true;
        }
        return documento.getSolicitud() != null && accesoPorAcuerdo(documento.getSolicitud(), usuario);
    }

    /** Lanza 403 si el usuario no puede ver el documento. */
    public void exigirAcceso(DocumentoCliente documento, Usuario usuario) {
        if (!puedeVer(documento, usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este documento");
        }
    }

    /** Quien puede ver la seccion "Documentos de esta solicitud" (misma regla que puedeVer). */
    public boolean puedeVerDocumentosDe(SolicitudDeclaracion solicitud, Usuario usuario) {
        return solicitud.getPersona().getId().equals(usuario.getId()) || accesoPorAcuerdo(solicitud, usuario);
    }

    // el acuerdo existe solo si hubo una oferta aceptada; los contadores que solo ofertaron no entran
    private boolean accesoPorAcuerdo(SolicitudDeclaracion solicitud, Usuario usuario) {
        for (Acuerdo acuerdo : acuerdoRepository.findByOfertaSolicitud(solicitud)) {
            if (acuerdo.getOferta().getContador().getId().equals(usuario.getId())) {
                return true;
            }
            if (usuario.getRol() == Rol.CONTADOR_PUBLICO && supervisionService.esMentorDe(usuario, acuerdo)) {
                return true;
            }
            if (usuario.getRol() == Rol.ADMIN && disputaService.hayDisputaAbierta(acuerdo)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ consultas

    public DocumentoCliente buscarPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
    }

    public List<DocumentoCliente> listarDe(Usuario propietario) {
        return documentoRepository.findByPropietarioOrderByFechaCargaDesc(propietario);
    }

    public List<DocumentoCliente> listarPorSolicitud(SolicitudDeclaracion solicitud) {
        return documentoRepository.findBySolicitudOrderByFechaCargaDesc(solicitud);
    }

    public Resource cargar(DocumentoCliente documento) throws IOException {
        return archivoService.cargarDe(ArchivoService.CARPETA_DOCUMENTOS, documento.getArchivo());
    }

    // ------------------------------------------------------------ acciones del propietario

    public DocumentoCliente subir(Usuario propietario, TipoDocumentoCliente tipo, String nombre,
                                  MultipartFile archivo, Long solicitudId) throws IOException {
        return subir(propietario, tipo, nombre, archivo, solicitudId, null, null);
    }

    /**
     * Sube un documento. Si se indica un requisito, debe pertenecer al tramite de la solicitud;
     * si se indica un requerimiento del contador, el documento se vincula a su solicitud y lo cumple.
     */
    public DocumentoCliente subir(Usuario propietario, TipoDocumentoCliente tipo, String nombre,
                                  MultipartFile archivo, Long solicitudId, String requisito,
                                  Long requerimientoId) throws IOException {
        if (propietario.getRol() != Rol.PERSONA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los clientes tienen documentos");
        }
        if (tipo == null) {
            throw new ReglaNegocioException("Elige el tipo de documento");
        }
        String nombreLimpio = validarNombre(nombre);

        RequerimientoDocumento requerimiento = null;
        if (requerimientoId != null) {
            requerimiento = requerimientoRepository.findById(requerimientoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Documento solicitado no encontrado"));
            SolicitudDeclaracion delRequerimiento = requerimiento.getAcuerdo().getOferta().getSolicitud();
            if (!delRequerimiento.getPersona().getId().equals(propietario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese documento no te lo pidieron a ti");
            }
            if (!requerimiento.isPendiente()) {
                throw new ReglaNegocioException("Ese documento ya fue entregado");
            }
            if (solicitudId != null && !solicitudId.equals(delRequerimiento.getId())) {
                throw new ReglaNegocioException("El documento solicitado pertenece a otra solicitud");
            }
            solicitudId = delRequerimiento.getId();
        }
        SolicitudDeclaracion solicitud = solicitudId == null ? null : solicitudVinculable(solicitudId, propietario);
        String requisitoLimpio = validarRequisito(solicitud, requisito);
        String guardado = guardarArchivo(archivo);

        DocumentoCliente documento = new DocumentoCliente();
        documento.setPropietario(propietario);
        documento.setTipo(tipo);
        documento.setNombre(nombreLimpio);
        documento.setNombreOriginal(nombreOriginal(archivo));
        documento.setArchivo(guardado);
        documento.setSolicitud(solicitud);
        documento.setRequisito(requisitoLimpio);
        DocumentoCliente nuevo = documentoRepository.save(documento);

        if (requerimiento != null) {
            requerimiento.setEstado(EstadoRequerimiento.ENTREGADO);
            requerimiento.setDocumento(nuevo);
            requerimiento.setFechaEntrega(LocalDateTime.now());
            requerimientoRepository.save(requerimiento);
        }
        avisarAcuerdo(nuevo, propietario, requerimiento);
        return nuevo;
    }

    // si la solicitud ya tiene acuerdo: historial y aviso al contador
    private void avisarAcuerdo(DocumentoCliente documento, Usuario cliente, RequerimientoDocumento requerimiento) {
        if (documento.getSolicitud() == null) {
            return;
        }
        for (Acuerdo acuerdo : acuerdoRepository.findByOfertaSolicitud(documento.getSolicitud())) {
            String detalle = documento.getNombre()
                    + (documento.getRequisito() != null ? " (requisito: " + documento.getRequisito() + ")" : "");
            if (requerimiento != null) {
                actividadService.registrar(acuerdo, cliente, TipoActividad.DOCUMENTO_ENTREGADO,
                        "Entregó lo solicitado (" + requerimiento.getNombre() + "): " + detalle);
            } else {
                actividadService.registrar(acuerdo, cliente, TipoActividad.DOCUMENTO_SUBIDO, detalle);
            }
            notificacionService.notificar(acuerdo.getOferta().getContador(),
                    cliente.getNombre() + " subió un documento para el acuerdo #" + acuerdo.getId() + ": " + documento.getNombre(),
                    "/acuerdos/" + acuerdo.getId());
        }
    }

    private String validarRequisito(SolicitudDeclaracion solicitud, String requisito) {
        if (requisito == null || requisito.isBlank()) {
            return null;
        }
        if (solicitud == null || !catalogo.esRequisitoDe(solicitud.getTitulo(), requisito.trim())) {
            throw new ReglaNegocioException("Ese requisito no corresponde al trámite de la solicitud");
        }
        return requisito.trim();
    }

    public DocumentoCliente reemplazar(Long id, Usuario propietario, MultipartFile archivo) throws IOException {
        DocumentoCliente documento = editable(id, propietario);
        String anterior = documento.getArchivo();
        documento.setArchivo(guardarArchivo(archivo));
        documento.setNombreOriginal(nombreOriginal(archivo));
        documento.setFechaCarga(LocalDateTime.now());
        DocumentoCliente guardado = documentoRepository.save(documento);
        archivoService.eliminarDe(ArchivoService.CARPETA_DOCUMENTOS, anterior);
        return guardado;
    }

    public void eliminar(Long id, Usuario propietario) {
        DocumentoCliente documento = editable(id, propietario);
        documentoRepository.delete(documento);
        archivoService.eliminarDe(ArchivoService.CARPETA_DOCUMENTOS, documento.getArchivo());
    }

    /** Asocia el documento a una solicitud propia, o lo desasocia si solicitudId es null. */
    public DocumentoCliente vincular(Long id, Usuario propietario, Long solicitudId) {
        DocumentoCliente documento = editable(id, propietario);
        SolicitudDeclaracion anterior = documento.getSolicitud();
        SolicitudDeclaracion nueva = solicitudId == null ? null : solicitudVinculable(solicitudId, propietario);
        documento.setSolicitud(nueva);
        boolean cambio = anterior == null ? nueva != null : nueva == null || !anterior.getId().equals(nueva.getId());
        if (cambio) {
            // el requisito depende del tramite de la solicitud, asi que no se conserva al cambiarla
            documento.setRequisito(null);
        }
        DocumentoCliente guardado = documentoRepository.save(documento);
        if (cambio) {
            avisarAcuerdo(guardado, propietario, null);
        }
        return guardado;
    }

    /** Antes de borrar una solicitud ABIERTA, sus documentos vuelven a "Mis documentos" sin solicitud. */
    public void desvincularDeSolicitud(SolicitudDeclaracion solicitud) {
        List<DocumentoCliente> documentos = documentoRepository.findBySolicitudOrderByFechaCargaDesc(solicitud);
        for (DocumentoCliente d : documentos) {
            d.setSolicitud(null);
        }
        documentoRepository.saveAll(documentos);
    }

    // ------------------------------------------------------------ auxiliares

    private DocumentoCliente propio(Long id, Usuario propietario) {
        DocumentoCliente documento = buscarPorId(id);
        if (!documento.getPropietario().getId().equals(propietario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este documento no es tuyo");
        }
        return documento;
    }

    private DocumentoCliente editable(Long id, Usuario propietario) {
        DocumentoCliente documento = propio(id, propietario);
        if (!documento.isEditable()) {
            throw new ReglaNegocioException("Este documento está bloqueado porque la solicitud ya tiene un acuerdo. Puedes subir uno nuevo.");
        }
        return documento;
    }

    // solo solicitudes propias que no esten cerradas
    private SolicitudDeclaracion solicitudVinculable(Long solicitudId, Usuario propietario) {
        SolicitudDeclaracion solicitud = solicitudService.buscarPorId(solicitudId);
        if (!solicitud.getPersona().getId().equals(propietario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esa solicitud no es tuya");
        }
        if (solicitud.getEstado() == EstadoSolicitud.CERRADA) {
            throw new ReglaNegocioException("No puedes vincular documentos a una solicitud cerrada");
        }
        return solicitud;
    }

    private String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("Escribe un nombre o descripción para el documento");
        }
        if (limpio.length() > NOMBRE_MAX) {
            throw new ReglaNegocioException("El nombre no puede superar " + NOMBRE_MAX + " caracteres");
        }
        return limpio;
    }

    private String guardarArchivo(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Selecciona un archivo");
        }
        try {
            return archivoService.guardarEn(ArchivoService.CARPETA_DOCUMENTOS, archivo);
        } catch (ArchivoService.ArchivoNoPermitidoException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
    }

    private static String nombreOriginal(MultipartFile archivo) {
        String original = StringUtils.getFilename(StringUtils.cleanPath(archivo.getOriginalFilename()));
        if (original == null || original.isBlank()) {
            return "documento";
        }
        return original.length() > 200 ? original.substring(original.length() - 200) : original;
    }
}
