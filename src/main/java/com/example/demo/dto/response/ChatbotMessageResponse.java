package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponse {
    private Long id;
    private String sessionId;
    private String senderType;
    private String messageText;
    private String retrievedProductIds;
    private List<Integer> retrievedProductIdList;
    private LocalDateTime createdAt;
}
