package com.example.demo.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String status;
    private String reply;
    private List<ProductResponse> products;
}
