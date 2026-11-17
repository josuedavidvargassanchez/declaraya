package com.example.declaraya.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.enums.EstadoDisputa;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Disputa;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    List<Disputa> findByEstado(EstadoDisputa estado);

    List<Disputa> findByEstadoInOrderByFechaCreacionDesc(Collection<EstadoDisputa> estados);

    boolean existsByAcuerdoAndEstado(Acuerdo acuerdo, EstadoDisputa estado);

    boolean existsByAcuerdoAndEstadoIn(Acuerdo acuerdo, Collection<EstadoDisputa> estados);

}
