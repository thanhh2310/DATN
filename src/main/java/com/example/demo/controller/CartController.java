package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/detail")
    public ApiResponse<CartDetailResponse> getCartDetail(@RequestBody CartCreationRequest request) {
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
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Clear cart successfully")
                .data(cartService.clearCart(request))
                .build();
    }

    @PostMapping("/merge")
    public ApiResponse<CartDetailResponse> mergeCart(
            @RequestParam String sessionId,
            @RequestParam Integer userId
    ) {
        return ApiResponse.<CartDetailResponse>builder()
                .code(200)
                .message("Merge cart successfully")
                .data(cartService.mergeCart(sessionId, userId))
                .build();
    }
}