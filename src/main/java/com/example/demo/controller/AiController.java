package com.example.demo.controller;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.request.RecommendRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.dto.response.ChatbotMessageResponse;
import com.example.demo.dto.response.ChatbotSessionResponse;
import com.example.demo.dto.response.ChatbotStatsResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.service.AiService;
import com.example.demo.service.ChatbotManagementService;
import com.example.demo.service.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ChatbotManagementService chatbotManagementService;
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

    @GetMapping("/admin/chatbot/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<ChatbotSessionResponse>> getChatbotSessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ChatbotSessionResponse>>builder()
                .code(200)
                .message("Lấy danh sách phiên chatbot thành công")
                .data(chatbotManagementService.getSessions(status, userId, page, size))
                .build();
    }

    @GetMapping("/admin/chatbot/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ChatbotSessionResponse> getChatbotSession(@PathVariable String sessionId) {
        return ApiResponse.<ChatbotSessionResponse>builder()
                .code(200)
                .message("Lấy chi tiết phiên chatbot thành công")
                .data(chatbotManagementService.getSession(sessionId))
                .build();
    }

    @GetMapping("/admin/chatbot/sessions/{sessionId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<ChatbotMessageResponse>> getChatbotMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ChatbotMessageResponse>>builder()
                .code(200)
                .message("Lấy tin nhắn phiên chatbot thành công")
                .data(chatbotManagementService.getMessages(sessionId, page, size))
                .build();
    }

    @PutMapping("/admin/chatbot/sessions/{sessionId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ChatbotSessionResponse> closeChatbotSession(@PathVariable String sessionId) {
        return ApiResponse.<ChatbotSessionResponse>builder()
                .code(200)
                .message("Đóng phiên chatbot thành công")
                .data(chatbotManagementService.closeSession(sessionId))
                .build();
    }

    @PutMapping("/admin/chatbot/sessions/{sessionId}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ChatbotSessionResponse> reopenChatbotSession(@PathVariable String sessionId) {
        return ApiResponse.<ChatbotSessionResponse>builder()
                .code(200)
                .message("Mở lại phiên chatbot thành công")
                .data(chatbotManagementService.reopenSession(sessionId))
                .build();
    }

    @GetMapping("/admin/chatbot/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ChatbotStatsResponse> getChatbotStats() {
        return ApiResponse.<ChatbotStatsResponse>builder()
                .code(200)
                .message("Lấy thống kê chatbot thành công")
                .data(chatbotManagementService.getStats())
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
