package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chatbot_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotSession {

    @Id
    @Column(length = 50)
    String id; // Chuỗi ID phiên chat (Ví dụ: UUID từ Frontend)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nullable cho khách vãng lai
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", length = 20)
    @Builder.Default
    SessionStatus sessionStatus = SessionStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum SessionStatus {
        ACTIVE, CLOSED
    }

    // Thêm đoạn này vào dưới cùng của ChatbotSession
    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") // Sắp xếp tin nhắn cũ trước, mới sau
            List<ChatbotMessage> messages = new ArrayList<>();
}