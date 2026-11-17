package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.enums.EstadoSolicitudMentoria;
import com.example.declaraya.models.SolicitudMentoria;
import com.example.declaraya.models.Usuario;

public interface SolicitudMentoriaRepository extends JpaRepository<SolicitudMentoria, Long> {

    List<SolicitudMentoria> findByPracticanteOrderByFechaSolicitudDescIdDesc(Usuario practicante);

    List<SolicitudMentoria> findByPracticanteAndEstado(Usuario practicante, EstadoSolicitudMentoria estado);

    boolean existsByPracticanteAndEstado(Usuario practicante, EstadoSolicitudMentoria estado);

    List<SolicitudMentoria> findByEstadoOrderByFechaSolicitudAsc(EstadoSolicitudMentoria estado);
}
