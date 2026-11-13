package com.example.declaraya.repositories;

import com.example.declaraya.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByAcuerdoId(Long acuerdoId);
}
