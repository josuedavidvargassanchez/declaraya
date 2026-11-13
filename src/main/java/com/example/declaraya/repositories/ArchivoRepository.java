package com.example.declaraya.repositories;

import com.example.declaraya.models.Archivo;
import com.example.declaraya.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    List<Archivo> findByMensaje(Mensaje mensaje);
}
