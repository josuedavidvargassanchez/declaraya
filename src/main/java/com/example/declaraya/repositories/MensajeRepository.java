package com.example.declaraya.repositories;

import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByChatOrderByFechaEnvioAsc(Chat chat);
}
