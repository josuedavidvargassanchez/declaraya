package com.example.declaraya.repositories;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcuerdoRepository extends JpaRepository<Acuerdo, Long> {

    Optional<Acuerdo> findByOfertaId(Long ofertaId);

    List<Acuerdo> findByOfertaContador(Usuario contador);

    List<Acuerdo> findByOfertaSolicitudPersona(Usuario persona);

    long countByOfertaContadorAndEstado(Usuario contador, EstadoAcuerdo estado);

    List<Acuerdo> findByEstadoAndRecordatorioEnviadoFalse(EstadoAcuerdo estado);

    // acuerdos nacidos de una solicitud (en la practica uno: el de la oferta aceptada)
    List<Acuerdo> findByOfertaSolicitud(SolicitudDeclaracion solicitud);
}
