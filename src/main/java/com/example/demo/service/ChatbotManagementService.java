package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.response.ChatbotMessageResponse;
import com.example.demo.dto.response.ChatbotSessionResponse;
import com.example.demo.dto.response.ChatbotStatsResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.model.ChatbotMessage;
import com.example.demo.model.ChatbotSession;
import com.example.demo.model.User;
import com.example.demo.repository.ChatbotMessageRepository;
import com.example.demo.repository.ChatbotSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotManagementService {
    private final ChatbotSessionRepository sessionRepository;
    private final ChatbotMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public PageResponse<ChatbotSessionResponse> getSessions(String status, Integer userId, int page, int size) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);

        Page<ChatbotSession> sessionPage;
        if (userId != null) {
            sessionPage = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        } else if (status != null && !status.isBlank()) {
            sessionPage = sessionRepository.findBySessionStatusOrderByUpdatedAtDesc(
                    ChatbotSession.SessionStatus.valueOf(status.trim().toUpperCase()),
                    pageable
            );
        } else {
            sessionPage = sessionRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }

        return PageResponse.<ChatbotSessionResponse>builder()
                .currentPage(page)
                .totalPage(sessionPage.getTotalPages())
                .pageSize(sessionPage.getSize())
                .totalElements(sessionPage.getTotalElements())
                .items(sessionPage.getContent().stream().map(this::toSessionResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatbotSessionResponse getSession(String sessionId) {
        return toSessionResponse(getSessionOrThrow(sessionId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatbotMessageResponse> getMessages(String sessionId, int page, int size) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new WebErrorConfig(ErrorCode.CHATBOT_SESSION_NOT_FOUND);
        }

        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<ChatbotMessage> messagePage = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);

        return PageResponse.<ChatbotMessageResponse>builder()
                .currentPage(page)
                .totalPage(messagePage.getTotalPages())
                .pageSize(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .items(messagePage.getContent().stream().map(this::toMessageResponse).toList())
                .build();
    }

    @Transactional
    public ChatbotSessionResponse closeSession(String sessionId) {
        ChatbotSession session = getSessionOrThrow(sessionId);
        session.setSessionStatus(ChatbotSession.SessionStatus.CLOSED);
        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional
    public ChatbotSessionResponse reopenSession(String sessionId) {
        ChatbotSession session = getSessionOrThrow(sessionId);
        session.setSessionStatus(ChatbotSession.SessionStatus.ACTIVE);
        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public ChatbotStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return ChatbotStatsResponse.builder()
                .totalSessions(sessionRepository.count())
                .activeSessions(sessionRepository.countBySessionStatus(ChatbotSession.SessionStatus.ACTIVE))
                .closedSessions(sessionRepository.countBySessionStatus(ChatbotSession.SessionStatus.CLOSED))
                .guestSessions(sessionRepository.countByUserIsNull())
                .authenticatedSessions(sessionRepository.countByUserIsNotNull())
                .todaySessions(sessionRepository.countByCreatedAtAfter(todayStart))
                .totalMessages(messageRepository.count())
                .userMessages(messageRepository.countBySenderType(ChatbotMessage.SenderType.USER))
                .botMessages(messageRepository.countBySenderType(ChatbotMessage.SenderType.BOT))
                .systemMessages(messageRepository.countBySenderType(ChatbotMessage.SenderType.SYSTEM))
                .todayMessages(messageRepository.countByCreatedAtAfter(todayStart))
                .build();
    }

    private ChatbotSession getSessionOrThrow(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CHATBOT_SESSION_NOT_FOUND));
    }

    private ChatbotSessionResponse toSessionResponse(ChatbotSession session) {
        User user = session.getUser();
        ChatbotMessage lastMessage = messageRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId()).orElse(null);

        return ChatbotSessionResponse.builder()
                .id(session.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? buildUserName(user) : null)
                .userEmail(user != null ? user.getEmail() : null)
                .sessionStatus(session.getSessionStatus().name())
                .messageCount(messageRepository.countBySessionId(session.getId()))
                .lastMessage(lastMessage != null ? lastMessage.getMessageText() : null)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private ChatbotMessageResponse toMessageResponse(ChatbotMessage message) {
        return ChatbotMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .senderType(message.getSenderType().name())
                .messageText(message.getMessageText())
                .retrievedProductIds(message.getRetrievedProductIds())
                .retrievedProductIdList(parseProductIds(message.getRetrievedProductIds()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    private List<Integer> parseProductIds(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(value -> value != null)
                .toList();
    }

    private String buildUserName(User user) {
        String fullName = String.join(" ",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : ""
        ).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
