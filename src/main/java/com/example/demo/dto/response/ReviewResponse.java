package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Integer id;
    Integer userId;
    String userName;
    Integer productId;
    Integer orderItemId;
    Integer rating;
    String comment;
    Boolean isApproved;
    LocalDateTime createdAt;
}
