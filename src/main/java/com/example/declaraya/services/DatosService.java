package com.example.declaraya.services;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.repositories.UsuarioRepository;

@Service 
public class DatosService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private AcuerdoService acuerdoService;


    public long personasNaturales(){
        return usuarioRepository.countByRol(Rol.PERSONA);
    }

    public long verificados(Rol rol){
        return usuarioRepository.countByRolAndVerificado(rol, true);
    }

    public long pendientes(Rol rol){
        return usuarioRepository.countByRolAndVerificado(rol, false);
    }

    public BigDecimal ingresos(EstadoAcuerdo estado){
        return acuerdoService.listarTodos().stream().filter(a -> a.getEstado() == estado).map(Acuerdo::getComisionPlataforma).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int porcentaje(long valor, long maximo){
        return maximo <= 0 ? 0 : (int) Math.round(valor * 100.0 / maximo);
    }


}
