package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInteractionResponse {
    Long id;
    Integer userId;
    String sessionId;
    Integer productId;
    String productName;
    String interactionType;
    Float interactionWeight;
    String searchKeyword;
    LocalDateTime createdAt;
}
