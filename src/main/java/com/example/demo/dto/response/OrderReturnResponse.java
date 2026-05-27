package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReturnResponse {
    private Integer id;
    private Integer orderId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private BigDecimal refundAmount;
    private String reason;
    private String adminNote;
    private String status;
    private Integer processedById;
    private String processedByName;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
