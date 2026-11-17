package com.example.declaraya.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.declaraya.models.DocumentoCliente;
import com.example.declaraya.models.SolicitudDeclaracion;
import com.example.declaraya.models.Usuario;

public interface DocumentoClienteRepository extends JpaRepository<DocumentoCliente, Long> {

    List<DocumentoCliente> findByPropietarioOrderByFechaCargaDesc(Usuario propietario);

    List<DocumentoCliente> findBySolicitudOrderByFechaCargaDesc(SolicitudDeclaracion solicitud);
}
