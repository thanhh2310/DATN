package com.example.demo.controller;

import com.example.demo.dto.request.ReviewRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ReviewResponse;
import com.example.demo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request
    ) {
        Integer userId = getCurrentUserId(userDetails);
        ReviewResponse response = reviewService.createReview(userId, request);

        return ApiResponse.<ReviewResponse>builder()
                .code(200)
                .message("Đánh giá sản phẩm thành công")
                .data(response)
                .build();
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<ReviewResponse> response = reviewService.getReviewsByProductId(productId, page, size);

        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(200)
                .message("Lấy danh sách đánh giá thành công")
                .data(response)
                .build();
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer userId = getCurrentUserId(userDetails);
        PageResponse<ReviewResponse> response = reviewService.getMyReviews(userId, page, size);

        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(200)
                .message("Lấy đánh giá của tôi thành công")
                .data(response)
                .build();
    }

    @PutMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewResponse> approveReview(
            @PathVariable Integer reviewId
    ) {
        ReviewResponse response = reviewService.approveReview(reviewId);

        return ApiResponse.<ReviewResponse>builder()
                .code(200)
                .message("Duyệt đánh giá thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer reviewId
    ) {
        Integer userId = getCurrentUserId(userDetails);
        reviewService.deleteReview(userId, reviewId);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Xóa đánh giá thành công")
                .data(null)
                .build();
    }

    @GetMapping("/product/{productId}/average-rating")
    public ApiResponse<Double> getAverageRating(
            @PathVariable Integer productId
    ) {
        Double avgRating = reviewService.getProductAverageRating(productId);

        return ApiResponse.<Double>builder()
                .code(200)
                .message("Lấy đánh giá trung bình thành công")
                .data(avgRating)
                .build();
    }

    @GetMapping("/product/{productId}/count")
    public ApiResponse<Long> getReviewCount(
            @PathVariable Integer productId
    ) {
        Long count = reviewService.getProductReviewCount(productId);

        return ApiResponse.<Long>builder()
                .code(200)
                .message("Lấy số lượng đánh giá thành công")
                .data(count)
                .build();
    }

    private Integer getCurrentUserId(UserDetails userDetails) {
        return ((com.example.demo.model.User) userDetails).getId();
    }
}
