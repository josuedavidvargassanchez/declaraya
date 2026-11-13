package com.example.declaraya.repositories;

import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDeclaracionRepository extends JpaRepository<SolicitudDeclaracion, Long> {

    List<SolicitudDeclaracion> findByPersona(Usuario persona);

    List<SolicitudDeclaracion> findByEstado(EstadoSolicitud estado);
}
