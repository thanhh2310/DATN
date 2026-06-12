package com.example.demo.controller;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.service.CartService;
import com.example.demo.service.Helper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {
    private final CartService cartService;
    private final Helper helper;

    private void secureRequest(CartCreationRequest request) {

        try {
            Integer currentUserId = helper.getCurrentUserId();

            // Nếu login → override userId
            request.setUserId(currentUserId);

        } catch (Exception e) {
            // Guest thì bỏ qua
            request.setUserId(null);
        }
    }

    @PostMapping("/detail")
    public ApiResponse<CartDetailResponse> getCartDetail(@Valid @RequestBody CartCreationRequest request) {
        secureRequest(request);
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Get cart successfully")
                .data(cartService.getCartDetail(request))
                .build();
    }

    @PostMapping("/add")
    public ApiResponse<CartDetailResponse> addItem(
            @Valid @RequestBody CartCreationRequest cartReq,
            @RequestParam @NotNull(message = "SKU ID không được để trống") Integer skuId,
            @RequestParam @NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity
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
            @Valid @RequestBody CartCreationRequest cartReq,
            @RequestParam @NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity
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
            @Valid @RequestBody CartCreationRequest cartReq
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
            @Valid @RequestBody CartCreationRequest request
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
