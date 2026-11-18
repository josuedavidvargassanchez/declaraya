package com.example.declaraya.repositories;

import com.example.declaraya.enums.EstadoSolicitud;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudDeclaracionRepository extends JpaRepository<SolicitudDeclaracion, Long> {

    List<SolicitudDeclaracion> findByPersona(Usuario persona);

    List<SolicitudDeclaracion> findByEstado(EstadoSolicitud estado);

    /**
     * SELECT ... FOR UPDATE de la solicitud de una oferta: dos aceptaciones simultaneas de la misma
     * solicitud se ejecutan una detras de otra. Es la primera lectura de la transaccion, asi que lo que
     * se lea despues ya ve lo que haya confirmado la otra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudDeclaracion s where s.id = (select o.solicitud.id from Oferta o where o.id = :ofertaId)")
    Optional<SolicitudDeclaracion> bloquearPorOferta(@Param("ofertaId") Long ofertaId);
}
