package com.example.declaraya.repositories;

import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findBySolicitud(SolicitudDeclaracion solicitud);

    List<Oferta> findByContador(Usuario contador);
}
