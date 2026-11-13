package com.example.declaraya.services;

import com.example.declaraya.dto.NuevaSolicitudDTO;
import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.SolicitudDeclaracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitudDeclaracionService {

    @Autowired
    private SolicitudDeclaracionRepository solicitudRepository;

    public SolicitudDeclaracion crear(Usuario persona, String titulo, String descripcion) {
        SolicitudDeclaracion solicitud = new SolicitudDeclaracion(persona, titulo, descripcion);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion crear(Usuario persona, NuevaSolicitudDTO dto) {
        SolicitudDeclaracion solicitud = new SolicitudDeclaracion(persona, dto.getTitulo(), dto.getDescripcion());
        copiarDetalles(solicitud, dto);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion editar(Long id, NuevaSolicitudDTO dto) {
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setTitulo(dto.getTitulo());
        solicitud.setDescripcion(dto.getDescripcion());
        copiarDetalles(solicitud, dto);
        return solicitudRepository.save(solicitud);
    }

    private void copiarDetalles(SolicitudDeclaracion solicitud, NuevaSolicitudDTO dto) {
        solicitud.setAnioGravable(dto.getAnioGravable());
        solicitud.setUrgencia(dto.getUrgencia());
        solicitud.setPresupuestoMaximo(dto.getPresupuestoMaximo());
        List<String> situaciones = dto.getSituaciones();
        solicitud.setSituaciones(situaciones == null || situaciones.isEmpty() ? null : String.join(",", situaciones));
    }

    public List<SolicitudDeclaracion> listarAbiertas() {
        return solicitudRepository.findByEstado(EstadoSolicitud.ABIERTA);
    }

    public List<SolicitudDeclaracion> listarPorPersona(Usuario persona) {
        return solicitudRepository.findByPersona(persona);
    }

    public SolicitudDeclaracion buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }

    public SolicitudDeclaracion actualizarEstado(Long id, EstadoSolicitud estado) {
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setEstado(estado);
        return solicitudRepository.save(solicitud);
    }

    public SolicitudDeclaracion editar(Long id, String titulo, String descripcion){
        SolicitudDeclaracion solicitud = buscarPorId(id);
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion(descripcion);
        return solicitudRepository.save(solicitud);
    }

    public void eliminar(Long id){
        solicitudRepository.deleteById(id);
    }

    public List<SolicitudDeclaracion> listarTodas() {
        return solicitudRepository.findAll();
    }
}
