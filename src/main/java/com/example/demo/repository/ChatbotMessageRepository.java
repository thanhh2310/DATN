package com.example.demo.repository;

import com.example.demo.model.ChatbotMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    Page<ChatbotMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    long countBySessionId(String sessionId);

    long countBySenderType(ChatbotMessage.SenderType senderType);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    Optional<ChatbotMessage> findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);
}
