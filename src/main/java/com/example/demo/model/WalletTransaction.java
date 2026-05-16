package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_transactions_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wallet_transactions_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_wallet_transactions_external_transaction_id", columnList = "external_transaction_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    Wallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    Direction direction;

    @Column(name = "reference_type", length = 30)
    String referenceType;

    @Column(name = "reference_id")
    Integer referenceId;

    @Column(name = "external_transaction_id", length = 100)
    String externalTransactionId;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceAfter;

    @Column(length = 255)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = TransactionStatus.SUCCESS;
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        PAYMENT,
        REFUND
    }

    public enum Direction {
        IN,
        OUT
    }

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}
