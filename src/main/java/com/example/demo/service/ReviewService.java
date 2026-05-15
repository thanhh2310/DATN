package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.ReviewRequest;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ReviewResponse;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.model.Review;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ReviewResponse createReview(Integer userId, ReviewRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.REVIEW_NOT_FOUND));

        if (!orderItem.getOrder().getUser().getId().equals(userId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (orderItem.getOrder().getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (!orderItem.getProductSku().getProduct().getId().equals(request.getProductId())) {
            throw new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (reviewRepository.existsByUserIdAndProductIdAndOrderItemId(
                userId, request.getProductId(), request.getOrderItemId())) {
            throw new WebErrorConfig(ErrorCode.REVIEW_ALREADY_EXISTED);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        Review review = Review.builder()
                .user(orderItem.getOrder().getUser())
                .product(product)
                .orderItem(orderItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .isApproved(true)
                .build();

        review = reviewRepository.save(review);

        return mapToReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProductId(Integer productId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Review> reviewPage = reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable);

        return PageResponse.<ReviewResponse>builder()
                .currentPage(page)
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPage(reviewPage.getTotalPages())
                .items(reviewPage.getContent().stream()
                        .map(this::mapToReviewResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Review> reviewPage = reviewRepository.findByUserId(userId, pageable);

        return PageResponse.<ReviewResponse>builder()
                .currentPage(page)
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPage(reviewPage.getTotalPages())
                .items(reviewPage.getContent().stream()
                        .map(this::mapToReviewResponse)
                        .toList())
                .build();
    }

    @Transactional
    public ReviewResponse approveReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.REVIEW_NOT_FOUND));

        review.setIsApproved(true);

        return mapToReviewResponse(review);
    }

    @Transactional
    public void deleteReview(Integer userId, Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId) && !hasRoleAdmin()) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        reviewRepository.delete(review);
    }

    private boolean hasRoleAdmin() {
        // Lấy thông tin đăng nhập hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nếu chưa đăng nhập hoặc không có thông tin
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Duyệt qua danh sách quyền xem có ROLE_ADMIN không
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @Transactional(readOnly = true)
    public Double getProductAverageRating(Integer productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    @Transactional(readOnly = true)
    public Long getProductReviewCount(Integer productId) {
        return reviewRepository.countByProductIdAndIsApprovedTrue(productId);
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .productId(review.getProduct().getId())
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
