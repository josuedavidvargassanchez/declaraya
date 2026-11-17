package com.example.declaraya.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.ActividadAcuerdo;

public interface ActividadAcuerdoRepository extends JpaRepository<ActividadAcuerdo, Long> {

    List<ActividadAcuerdo> findByAcuerdoOrderByFechaAscIdAsc(Acuerdo acuerdo);

    List<ActividadAcuerdo> findByAcuerdoAndTipo(Acuerdo acuerdo, TipoActividad tipo);

    List<ActividadAcuerdo> findByAcuerdoInAndTipo(Collection<Acuerdo> acuerdos, TipoActividad tipo);
}
