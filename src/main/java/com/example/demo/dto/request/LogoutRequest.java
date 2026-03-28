package com.example.demo.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutRequest {
    private String refreshToken;
}
