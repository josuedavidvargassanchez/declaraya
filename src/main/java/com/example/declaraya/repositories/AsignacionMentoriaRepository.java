package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.models.AsignacionMentoria;
import com.example.declaraya.models.Usuario;

public interface AsignacionMentoriaRepository extends JpaRepository<AsignacionMentoria, Long> {

    // en la practica hay una sola vigente; se devuelve lista para poder cerrar cualquier inconsistencia
    List<AsignacionMentoria> findByPracticanteAndFechaFinIsNull(Usuario practicante);

    List<AsignacionMentoria> findByFechaFinIsNull();

    List<AsignacionMentoria> findByPracticanteOrderByFechaInicioDescIdDesc(Usuario practicante);

    List<AsignacionMentoria> findAllByOrderByFechaInicioDescIdDesc();

    boolean existsByPracticante(Usuario practicante);
}
