package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.declaraya.enums.EstadoDisputa;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Disputa;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.DisputaRepository;

@Service 
public class DisputaService {
    @Autowired
    private DisputaRepository disputaRepository;
    
    public Disputa abrir(Acuerdo acuerdo, Usuario usuario, String motivo){
        Disputa disputa = new Disputa();
        disputa.setAcuerdo(acuerdo);
        disputa.setAbiertaPor(usuario);
        disputa.setMotivo(motivo);
        return disputaRepository.save(disputa);
    }

    public boolean hayDisputaAbierta(Acuerdo acuerdo){
        return disputaRepository.existsByAcuerdoAndEstado(acuerdo, EstadoDisputa.ABIERTA);
    }

    public List<Disputa> listarAbiertas(){
        return disputaRepository.findByEstado(EstadoDisputa.ABIERTA);
    }

    public Disputa resolver(Long id){
        Disputa disputa = disputaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Disputa no encontrada"));
        disputa.setEstado(EstadoDisputa.RESUELTA);
        return disputaRepository.save(disputa);
    }


}
