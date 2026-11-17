package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.EstadoOferta;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.OfertaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OfertaService {

    @Autowired
    private OfertaRepository ofertaRepository;

    public static final int MAX_DIAS_ENTREGA = 120;

    /** La fecha estimada debe ser futura y no superar 120 dias. Devuelve true si es valida. */
    public static boolean fechaEntregaValida(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        return fecha != null && fecha.isAfter(hoy) && !fecha.isAfter(hoy.plusDays(MAX_DIAS_ENTREGA));
    }

    public Oferta crear(SolicitudDeclaracion solicitud, Usuario contador, String mensaje, BigDecimal valorPropuesto) {
        return ofertaRepository.save(new Oferta(solicitud, contador, mensaje, valorPropuesto));
    }

    public Oferta crear(SolicitudDeclaracion solicitud, Usuario contador, String mensaje, BigDecimal valorPropuesto,
                        LocalDate fechaEntregaEstimada) {
        Oferta oferta = new Oferta(solicitud, contador, mensaje, valorPropuesto);
        oferta.setFechaEntregaEstimada(fechaEntregaEstimada);
        return ofertaRepository.save(oferta);
    }

    public List<Oferta> listarPorSolicitud(SolicitudDeclaracion solicitud) {
        return ofertaRepository.findBySolicitud(solicitud);
    }

    public List<Oferta> listarPorContador(Usuario contador) {
        return ofertaRepository.findByContador(contador);
    }

    public Oferta buscarPorId(Long id) {
        return ofertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
    }

    public Oferta actualizarEstado(Long id, EstadoOferta estado) {
        Oferta oferta = buscarPorId(id);
        oferta.setEstado(estado);
        return ofertaRepository.save(oferta);
    }

    public Oferta editar(Long id, String mensaje, BigDecimal valorPropuesto, LocalDate fechaEntregaEstimada) {
        Oferta oferta = buscarPorId(id);
        oferta.setMensaje(mensaje);
        oferta.setValorPropuesto(valorPropuesto);
        oferta.setFechaEntregaEstimada(fechaEntregaEstimada);
        return ofertaRepository.save(oferta);
    }

    public void eliminarPorSolicitud(SolicitudDeclaracion solicitud){
        ofertaRepository.deleteAll(ofertaRepository.findBySolicitud(solicitud));
    }

    public List<Oferta> listarTodas() {
        return ofertaRepository.findAll();
    }
}
