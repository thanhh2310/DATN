package com.example.demo.controller;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.request.RecommendRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.service.AiService;
import com.example.demo.service.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final Helper helper;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chatWithBot(@RequestBody ChatRequest request) {
        request.setUserId(resolveCurrentUserId());
        ChatResponse response = aiService.processChat(request);
        return ApiResponse.<ChatResponse>builder()
                .code(200)
                .message("Chat xử lý thành công")
                .data(response)
                .build();
    }

    @PostMapping("/recommend")
    public ApiResponse<List<ProductResponse>> getRecommendations(@RequestBody RecommendRequest request) {
        request.setUserId(resolveCurrentUserId());
        List<ProductResponse> products = aiService.getRecommendations(request);
        return ApiResponse.<List<ProductResponse>>builder()
                .code(200)
                .message("Gợi ý sản phẩm thành công")
                .data(products)
                .build();
    }

    private Integer resolveCurrentUserId() {
        try {
            return helper.getCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
