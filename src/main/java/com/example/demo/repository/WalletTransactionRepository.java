package com.example.demo.repository;

import com.example.demo.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Optional<WalletTransaction> findByExternalTransactionId(String externalTransactionId);

    Optional<WalletTransaction> findFirstByReferenceTypeAndReferenceIdAndTransactionTypeAndStatus(
            String referenceType,
            Integer referenceId,
            WalletTransaction.TransactionType transactionType,
            WalletTransaction.TransactionStatus status
    );
}
