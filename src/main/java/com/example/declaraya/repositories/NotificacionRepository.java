package com.example.declaraya.repositories;

import com.example.declaraya.models.Notificacion;
import com.example.declaraya.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioOrderByFechaDesc(Usuario usuario);

    long countByUsuarioAndLeidaFalse(Usuario usuario);
}
