package com.example.demo.repository;

import com.example.demo.model.ChatbotSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, String> {
    Page<ChatbotSession> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<ChatbotSession> findBySessionStatusOrderByUpdatedAtDesc(ChatbotSession.SessionStatus status, Pageable pageable);

    Page<ChatbotSession> findByUserIdOrderByUpdatedAtDesc(Integer userId, Pageable pageable);

    long countBySessionStatus(ChatbotSession.SessionStatus status);

    long countByUserIsNull();

    long countByUserIsNotNull();

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
