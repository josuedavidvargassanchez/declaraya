package com.example.declaraya.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.ActividadAcuerdo;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.ActividadAcuerdoRepository;

/** Historial del acuerdo. Solo registra y lista: no hay forma de editar ni borrar actividades. */
@Service
public class ActividadAcuerdoService {

    private final ActividadAcuerdoRepository actividadRepository;

    public ActividadAcuerdoService(ActividadAcuerdoRepository actividadRepository) {
        this.actividadRepository = actividadRepository;
    }

    public ActividadAcuerdo registrar(Acuerdo acuerdo, Usuario actor, TipoActividad tipo, String descripcion) {
        return registrar(acuerdo, actor, tipo, descripcion, LocalDateTime.now());
    }

    public ActividadAcuerdo registrar(Acuerdo acuerdo, Usuario actor, TipoActividad tipo, String descripcion,
                                     LocalDateTime fecha) {
        String texto = descripcion != null && descripcion.length() > 500 ? descripcion.substring(0, 497) + "..." : descripcion;
        ActividadAcuerdo actividad = new ActividadAcuerdo(acuerdo, actor, tipo, texto);
        actividad.setFecha(fecha == null ? LocalDateTime.now() : fecha);
        return actividadRepository.save(actividad);
    }

    public List<ActividadAcuerdo> listar(Acuerdo acuerdo) {
        return actividadRepository.findByAcuerdoOrderByFechaAscIdAsc(acuerdo);
    }

    /** Datos de ejemplo: alinea la actividad de creacion con la fecha (pasada) del acuerdo. */
    public void alinearFechaCreacion(Acuerdo acuerdo) {
        for (ActividadAcuerdo a : actividadRepository.findByAcuerdoAndTipo(acuerdo, TipoActividad.ACUERDO_CREADO)) {
            a.setFecha(acuerdo.getFechaCreacion());
            actividadRepository.save(a);
        }
    }
}
