package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.HistorialDisputa;

public interface HistorialDisputaRepository extends JpaRepository<HistorialDisputa, Long> {

    List<HistorialDisputa> findByDisputaOrderByFechaAscIdAsc(Disputa disputa);
}
