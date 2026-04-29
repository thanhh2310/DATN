package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.service.Helper;
import com.example.demo.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final Helper helper;

    /**
     * Thêm sản phẩm vào wishlist
     * POST /api/wishlist/{productId}
     */
    @PostMapping("/{productId}")
    public ApiResponse<WishlistResponse> addToWishlist(@PathVariable Integer productId) {
        Integer userId = helper.getCurrentUserId();
        return ApiResponse.<WishlistResponse>builder()
                .code(200)
                .message("Product added to wishlist successfully")
                .data(wishlistService.addToWishlist(userId, productId))
                .build();
    }

    /**
     * Xoá sản phẩm khỏi wishlist
     * DELETE /api/wishlist/{productId}
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeFromWishlist(@PathVariable Integer productId) {
        Integer userId = helper.getCurrentUserId();
        wishlistService.removeFromWishlist(userId, productId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Product removed from wishlist successfully")
                .build();
    }

    /**
     * Lấy danh sách wishlist của user (có phân trang)
     * GET /api/wishlist?page=1&size=10
     */
    @GetMapping
    public ApiResponse<PageResponse<WishlistResponse>> getWishlist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer userId = helper.getCurrentUserId();
        return ApiResponse.<PageResponse<WishlistResponse>>builder()
                .code(200)
                .message("Get wishlist successfully")
                .data(wishlistService.getWishlist(userId, page, size))
                .build();
    }

    /**
     * Kiểm tra sản phẩm có trong wishlist không
     * GET /api/wishlist/check/{productId}
     */
    @GetMapping("/check/{productId}")
    public ApiResponse<Boolean> isInWishlist(@PathVariable Integer productId) {
        Integer userId = helper.getCurrentUserId();
        return ApiResponse.<Boolean>builder()
                .code(200)
                .message("Check wishlist successfully")
                .data(wishlistService.isInWishlist(userId, productId))
                .build();
    }
}
