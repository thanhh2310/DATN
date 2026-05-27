package com.example.demo.controller;

import com.example.demo.dto.request.OrderReturnDecisionRequest;
import com.example.demo.dto.request.OrderReturnRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.OrderReturnResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.service.Helper;
import com.example.demo.service.OrderReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-returns")
@RequiredArgsConstructor
public class OrderReturnController {
    private final OrderReturnService orderReturnService;
    private final Helper helper;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderReturnResponse> createReturnRequest(
            @PathVariable Integer orderId,
            @Valid @RequestBody OrderReturnRequest request
    ) {
        return ApiResponse.<OrderReturnResponse>builder()
                .code(200)
                .message("Gửi yêu cầu hoàn hàng thành công")
                .data(orderReturnService.createReturnRequest(helper.getCurrentUserId(), orderId, request))
                .build();
    }

    @PutMapping("/{returnId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderReturnResponse> approveReturn(
            @PathVariable Integer returnId,
            @RequestBody(required = false) OrderReturnDecisionRequest request
    ) {
        return ApiResponse.<OrderReturnResponse>builder()
                .code(200)
                .message("Duyệt hoàn hàng thành công")
                .data(orderReturnService.approveReturn(returnId, helper.getCurrentUserId(), request))
                .build();
    }

    @PutMapping("/{returnId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderReturnResponse> rejectReturn(
            @PathVariable Integer returnId,
            @RequestBody(required = false) OrderReturnDecisionRequest request
    ) {
        return ApiResponse.<OrderReturnResponse>builder()
                .code(200)
                .message("Từ chối hoàn hàng thành công")
                .data(orderReturnService.rejectReturn(returnId, helper.getCurrentUserId(), request))
                .build();
    }

    @GetMapping("/my-returns")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<OrderReturnResponse>> getMyReturns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderReturnResponse>>builder()
                .code(200)
                .message("Lấy danh sách yêu cầu hoàn hàng của tôi thành công")
                .data(orderReturnService.getMyReturns(helper.getCurrentUserId(), page, size))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<OrderReturnResponse>> getAllReturns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderReturnResponse>>builder()
                .code(200)
                .message("Lấy danh sách yêu cầu hoàn hàng thành công")
                .data(orderReturnService.getAllReturns(page, size))
                .build();
    }

    @GetMapping("/{returnId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderReturnResponse> getReturnById(@PathVariable Integer returnId) {
        return ApiResponse.<OrderReturnResponse>builder()
                .code(200)
                .message("Lấy chi tiết yêu cầu hoàn hàng thành công")
                .data(orderReturnService.getReturnById(returnId))
                .build();
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OrderReturnResponse> getReturnByOrderId(@PathVariable Integer orderId) {
        boolean isManager = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        return ApiResponse.<OrderReturnResponse>builder()
                .code(200)
                .message("Lấy yêu cầu hoàn hàng theo đơn thành công")
                .data(orderReturnService.getReturnByOrderId(orderId, helper.getCurrentUserId(), isManager))
                .build();
    }
}
