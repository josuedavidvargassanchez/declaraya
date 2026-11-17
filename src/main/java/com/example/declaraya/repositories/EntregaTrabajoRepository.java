package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.enums.EstadoEntrega;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.EntregaTrabajo;
import com.example.declaraya.models.Usuario;

public interface EntregaTrabajoRepository extends JpaRepository<EntregaTrabajo, Long> {

    List<EntregaTrabajo> findByAcuerdoOrderByFechaDesc(Acuerdo acuerdo);

    List<EntregaTrabajo> findByContadorOrderByFechaDesc(Usuario contador);

    List<EntregaTrabajo> findByContadorAndEstado(Usuario contador, EstadoEntrega estado);

    List<EntregaTrabajo> findByEstadoOrderByFechaAsc(EstadoEntrega estado);

    boolean existsByAcuerdoAndEstado(Acuerdo acuerdo, EstadoEntrega estado);
}
