package com.example.declaraya.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Calificacion;
import com.example.declaraya.models.Usuario;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    Optional<Calificacion> findByAcuerdo(Acuerdo acuerdo);

    boolean existsByAcuerdo(Acuerdo acuerdo);

    List<Calificacion> findByContadorAndOcultaFalseOrderByFechaDesc(Usuario contador);

    List<Calificacion> findByComentarioIsNotNullOrderByFechaDesc();
}
