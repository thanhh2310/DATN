package com.example.demo.controller;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.service.CartService;
import com.example.demo.service.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final Helper helper;

    private void secureRequest(CartCreationRequest request) {
        // Nếu user đã đăng nhập, helper sẽ trả về ID thật. Nếu là Khách (Guest), helper trả về null.
        Integer currentUserId = helper.getCurrentUserId();

        // Ghi đè luôn! Cho dù Hacker cố tình gửi userId = 5, hệ thống vẫn ép về ID thật của Token.
        request.setUserId(currentUserId);
    }

    @PostMapping("/detail")
    public ApiResponse<CartDetailResponse> getCartDetail(@RequestBody CartCreationRequest request) {
        secureRequest(request);
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Get cart successfully")
                .data(cartService.getCartDetail(request))
                .build();
    }

    @PostMapping("/add")
    public ApiResponse<CartDetailResponse> addItem(
            @RequestBody CartCreationRequest cartReq,
            @RequestParam Integer skuId,
            @RequestParam Integer quantity
    ) {
        secureRequest(cartReq);
        CartItemRequest itemReq = CartItemRequest.builder()
                .skuId(skuId)
                .quantity(quantity)
                .build();

        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Add item successfully")
                .data(cartService.addItem(cartReq, itemReq))
                .build();
    }

    @PutMapping("/item/{itemId}")
    public ApiResponse<CartDetailResponse> updateItem(
            @PathVariable Integer itemId,
            @RequestBody CartCreationRequest cartReq,
            @RequestParam Integer quantity
    ) {
        secureRequest(cartReq);
        CartItemUpdateRequest req = CartItemUpdateRequest.builder()
                .quantity(quantity)
                .build();

        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Update item successfully")
                .data(cartService.updateItem(cartReq, itemId, req))
                .build();
    }

    @DeleteMapping("/item/{itemId}")
    public ApiResponse<CartDetailResponse> removeItem(
            @PathVariable Integer itemId,
            @RequestBody CartCreationRequest cartReq
    ) {
        secureRequest(cartReq);
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Remove item successfully")
                .data(cartService.removeItem(cartReq, itemId))
                .build();
    }

    @DeleteMapping("/clear")
    public ApiResponse<CartDetailResponse> clearCart(
            @RequestBody CartCreationRequest request
    ) {
        secureRequest(request);
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Clear cart successfully")
                .data(cartService.clearCart(request))
                .build();
    }

    @PostMapping("/merge")
    public ApiResponse<CartDetailResponse> mergeCart(
            @RequestParam String sessionId
    ) {
        Integer currentUserId = helper.getCurrentUserId();

        // API Merge bắt buộc người dùng phải đăng nhập mới chạy được
        if (currentUserId == null) {
            throw new WebErrorConfig(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Merge cart successfully")
                .data(cartService.mergeCart(sessionId, currentUserId))
                .build();
    }
}