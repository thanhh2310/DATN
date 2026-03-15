package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id; // Dùng Long vì log message sẽ rất nhiều

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    ChatbotSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    SenderType senderType;

    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    String messageText;

    // Mảng ID sản phẩm mà VectorDB trả về, lưu dạng chuỗi (VD: "12, 15, 20")
    @Column(name = "retrieved_product_ids")
    String retrievedProductIds;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    public enum SenderType {
        USER, BOT, SYSTEM
    }
}