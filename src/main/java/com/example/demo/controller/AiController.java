package com.example.demo.controller;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.request.RecommendRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chatWithBot(@RequestBody ChatRequest request) {
        ChatResponse response = aiService.processChat(request);
        return ApiResponse.<ChatResponse>builder()
                .code(200)
                .message("Chat xử lý thành công")
                .data(response)
                .build();
    }

    @PostMapping("/recommend")
    public ApiResponse<List<ProductResponse>> getRecommendations(@RequestBody RecommendRequest request) {
        List<ProductResponse> products = aiService.getRecommendations(request);
        return ApiResponse.<List<ProductResponse>>builder()
                .code(200)
                .message("Gợi ý sản phẩm thành công")
                .data(products)
                .build();
    }
}
