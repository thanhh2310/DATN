package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInteractionRequest {

    String sessionId;

    Integer productId;

    @NotNull(message = "Interaction type không được để trống")
    String interactionType;

    String searchKeyword;
}
