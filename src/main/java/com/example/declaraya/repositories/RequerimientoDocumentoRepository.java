package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.enums.EstadoRequerimiento;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.RequerimientoDocumento;

public interface RequerimientoDocumentoRepository extends JpaRepository<RequerimientoDocumento, Long> {

    List<RequerimientoDocumento> findByAcuerdoOrderByFechaSolicitudAsc(Acuerdo acuerdo);

    long countByAcuerdoAndEstado(Acuerdo acuerdo, EstadoRequerimiento estado);
}
