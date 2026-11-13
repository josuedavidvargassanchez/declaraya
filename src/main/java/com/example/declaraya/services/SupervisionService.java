package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.EstadoSupervision;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Supervision;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.SupervisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class SupervisionService {

    @Autowired
    private SupervisionRepository supervisionRepository;

    public Supervision asignar(Usuario practicante, Usuario mentor, Acuerdo acuerdo) {
        return supervisionRepository.save(new Supervision(practicante, mentor, acuerdo));
    }

    public List<Supervision> listarPorMentor(Usuario mentor) {
        return supervisionRepository.findByMentor(mentor);
    }

    public List<Supervision> listarPorPracticante(Usuario practicante) {
        return supervisionRepository.findByPracticante(practicante);
    }

    public Optional<Supervision> buscarPorAcuerdo(Acuerdo acuerdo){
        return supervisionRepository.findByAcuerdoId(acuerdo.getId());
    }

    public boolean esMentorDe(Usuario usuario, Acuerdo acuerdo){
        return buscarPorAcuerdo(acuerdo).map(s -> s.getMentor().getId().equals(usuario.getId())).orElse(false);
    }

    public Supervision aprobar(Long id, Usuario mentor) {
        Supervision supervision = supervisionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Supervision no encontrada"));
        if (!supervision.getMentor().getId().equals(mentor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el mentor asignado puede aprobar este caso");
        }        
        supervision.setEstado(EstadoSupervision.APROBADO);
        return supervisionRepository.save(supervision);
    }
}
