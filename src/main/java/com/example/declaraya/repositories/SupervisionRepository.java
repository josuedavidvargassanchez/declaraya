package com.example.declaraya.repositories;

import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupervisionRepository extends JpaRepository<Supervision, Long> {

    List<Supervision> findByMentor(Usuario mentor);

    List<Supervision> findByPracticante(Usuario practicante);

    Optional<Supervision> findByAcuerdoId(Long acuerdoId);
}
