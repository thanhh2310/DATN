package com.example.demo.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String query;
    private String message;
    private String sessionId;
    private Integer userId;
}
