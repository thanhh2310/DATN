package com.example.demo.controller;

import com.example.demo.dto.request.WalletDepositRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.WalletDepositResponse;
import com.example.demo.dto.response.WalletResponse;
import com.example.demo.dto.response.WalletTransactionResponse;
import com.example.demo.service.Helper;
import com.example.demo.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final Helper helper;

    @GetMapping
    public ApiResponse<WalletResponse> getMyWallet() {
        return ApiResponse.<WalletResponse>builder()
                .code(200)
                .message("Lấy thông tin ví thành công")
                .data(walletService.getMyWallet(helper.getCurrentUserId()))
                .build();
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<WalletTransactionResponse>>builder()
                .code(200)
                .message("Lấy lịch sử giao dịch ví thành công")
                .data(walletService.getTransactions(helper.getCurrentUserId(), page, size))
                .build();
    }

    @PostMapping("/deposit/vnpay")
    public ApiResponse<WalletDepositResponse> deposit(
            @Valid @RequestBody WalletDepositRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.<WalletDepositResponse>builder()
                .code(200)
                .message("Tạo giao dịch nạp ví thành công")
                .data(walletService.createDeposit(helper.getCurrentUserId(), request.getAmount(), servletRequest))
                .build();
    }
}
