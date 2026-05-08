package com.example.demo.controller;

import com.example.demo.dto.request.UserInteractionRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.UserInteractionResponse;
import com.example.demo.service.UserInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class UserInteractionController {

    private final UserInteractionService userInteractionService;

    @PostMapping("/track")
    public ApiResponse<UserInteractionResponse> trackInteraction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserInteractionRequest request
    ) {
        Integer userId = userDetails != null ? ((com.example.demo.model.User) userDetails).getId() : null;
        UserInteractionResponse response = userInteractionService.trackInteraction(request, userId);

        return ApiResponse.<UserInteractionResponse>builder()
                .code(200)
                .message("Track interaction successfully")
                .data(response)
                .build();
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserInteractionResponse>> getUserHistory(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer userId = ((com.example.demo.model.User) userDetails).getId();
        List<UserInteractionResponse> responses = userInteractionService.getUserHistory(userId);

        return ApiResponse.<List<UserInteractionResponse>>builder()
                .code(200)
                .message("Lấy lịch sử tương tác thành công")
                .data(responses)
                .build();
    }

    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<UserInteractionResponse>> getSessionHistory(
            @PathVariable String sessionId
    ) {
        List<UserInteractionResponse> responses = userInteractionService.getSessionHistory(sessionId);

        return ApiResponse.<List<UserInteractionResponse>>builder()
                .code(200)
                .message("Lấy lịch sử session thành công")
                .data(responses)
                .build();
    }

    @GetMapping("/recommendations/scores")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<Integer, Double>> getRecommendationScores(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer userId = ((com.example.demo.model.User) userDetails).getId();
        Map<Integer, Double> scores = userInteractionService.getRecommendedProductScores(userId);

        return ApiResponse.<Map<Integer, Double>>builder()
                .code(200)
                .message("Lấy điểm gợi ý sản phẩm thành công")
                .data(scores)
                .build();
    }
}
