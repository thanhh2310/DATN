package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.WalletDepositResponse;
import com.example.demo.dto.response.WalletResponse;
import com.example.demo.dto.response.WalletTransactionResponse;
import com.example.demo.model.Order;
import com.example.demo.model.User;
import com.example.demo.model.Wallet;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.repository.WalletTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private static final String WALLET_DEPOSIT_PREFIX = "WALLET_";
    private static final String ORDER_REFERENCE = "ORDER";
    private static final String VNPAY_DEPOSIT_REFERENCE = "VNPAY_DEPOSIT";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional
    public Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
                    return walletRepository.save(Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .isActive(true)
                            .build());
                });
    }

    @Transactional
    public WalletResponse getMyWallet(Integer userId) {
        return toWalletResponse(getOrCreateWallet(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> getTransactions(Integer userId, int page, int size) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<WalletTransaction> transactionPage = walletTransactionRepository
                .findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);

        return PageResponse.<WalletTransactionResponse>builder()
                .currentPage(page)
                .totalPage(transactionPage.getTotalPages())
                .pageSize(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .items(transactionPage.getContent().stream().map(this::toTransactionResponse).toList())
                .build();
    }

    @Transactional
    public WalletDepositResponse createDeposit(Integer userId, BigDecimal amount, HttpServletRequest request) {
        validatePositiveAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        validateWalletActive(wallet);

        String txnRef = WALLET_DEPOSIT_PREFIX + UUID.randomUUID().toString().replace("-", "");
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransaction.TransactionType.DEPOSIT)
                .direction(WalletTransaction.Direction.IN)
                .referenceType(VNPAY_DEPOSIT_REFERENCE)
                .externalTransactionId(txnRef)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance())
                .description("Nạp tiền vào ví qua VNPAY")
                .status(WalletTransaction.TransactionStatus.PENDING)
                .build();
        transaction = walletTransactionRepository.save(transaction);

        String paymentUrl = paymentService.createVnPayPayment(
                txnRef,
                amount,
                "Nap tien vao vi " + transaction.getId(),
                request
        );

        return WalletDepositResponse.builder()
                .transactionId(transaction.getId())
                .amount(amount)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getPendingDepositAmount(String txnRef) {
        WalletTransaction transaction = walletTransactionRepository.findByExternalTransactionId(txnRef)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));
        return transaction.getAmount();
    }

    @Transactional
    public void completeDeposit(String txnRef, boolean isSuccess, String vnpTransactionNo) {
        WalletTransaction transaction = walletTransactionRepository.findByExternalTransactionId(txnRef)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != WalletTransaction.TransactionStatus.PENDING) {
            return;
        }

        if (!isSuccess) {
            transaction.setStatus(WalletTransaction.TransactionStatus.FAILED);
            transaction.setDescription("Nạp tiền qua VNPAY thất bại hoặc bị hủy");
            walletTransactionRepository.save(transaction);
            return;
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(transaction.getWallet().getUser().getId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.WALLET_NOT_FOUND));
        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(transaction.getAmount());
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus(WalletTransaction.TransactionStatus.SUCCESS);
        transaction.setDescription("Nạp tiền vào ví qua VNPAY. Mã GD: " + vnpTransactionNo);
        walletTransactionRepository.save(transaction);
    }

    @Transactional
    public void payOrder(Integer userId, Order order) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.WALLET_NOT_FOUND));
        validateWalletActive(wallet);

        BigDecimal amount = order.getTotalAmount();
        validatePositiveAmount(amount);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new WebErrorConfig(ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransaction.TransactionType.PAYMENT)
                .direction(WalletTransaction.Direction.OUT)
                .referenceType(ORDER_REFERENCE)
                .referenceId(order.getId())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Thanh toán đơn hàng #" + order.getId())
                .status(WalletTransaction.TransactionStatus.SUCCESS)
                .build());
    }

    @Transactional
    public void refundOrder(Order order, String description) {
        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new WebErrorConfig(ErrorCode.ORDER_NOT_PAID);
        }

        boolean refunded = walletTransactionRepository
                .findFirstByReferenceTypeAndReferenceIdAndTransactionTypeAndStatus(
                        ORDER_REFERENCE,
                        order.getId(),
                        WalletTransaction.TransactionType.REFUND,
                        WalletTransaction.TransactionStatus.SUCCESS
                )
                .isPresent();
        if (refunded) {
            throw new WebErrorConfig(ErrorCode.ORDER_ALREADY_REFUNDED);
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(order.getUser().getId())
                .orElseGet(() -> getOrCreateWallet(order.getUser().getId()));
        validateWalletActive(wallet);

        BigDecimal amount = order.getTotalAmount();
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransaction.TransactionType.REFUND)
                .direction(WalletTransaction.Direction.IN)
                .referenceType(ORDER_REFERENCE)
                .referenceId(order.getId())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .status(WalletTransaction.TransactionStatus.SUCCESS)
                .build());
    }

    public boolean isWalletDepositRef(String txnRef) {
        return txnRef != null && txnRef.startsWith(WALLET_DEPOSIT_PREFIX);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebErrorConfig(ErrorCode.INVALID_WALLET_AMOUNT);
        }
    }

    private void validateWalletActive(Wallet wallet) {
        if (!Boolean.TRUE.equals(wallet.getIsActive())) {
            throw new WebErrorConfig(ErrorCode.WALLET_INACTIVE);
        }
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .isActive(wallet.getIsActive())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType().name())
                .direction(transaction.getDirection().name())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .externalTransactionId(transaction.getExternalTransactionId())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
