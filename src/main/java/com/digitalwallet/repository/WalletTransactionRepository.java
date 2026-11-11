package com.digitalwallet.repository;

import com.digitalwallet.entity.WalletTransaction;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @EntityGraph(attributePaths = "wallet")
    @Query("""
            SELECT t
            FROM WalletTransaction t
            WHERE t.wallet.owner.email = :email
                        
            AND (
                :type IS NULL
                OR t.transactionType = :type
            )
                        
            AND (
                :status IS NULL
                OR t.status = :status
            )
                        
            AND (
                 :fromDate IS NULL
                 OR t.createdAt >= :fromDate
                    )
                        
            AND (
                 :toDate IS NULL
                 OR t.createdAt <= :toDate
            )
            """)
    Page<WalletTransaction> findTransactionHistory(
            @Param("email") String email,
            @Param("type")TransactionType type,
            @Param("status")TransactionStatus status,
            @Param("fromDate")LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageble
            );

    @EntityGraph(attributePaths = "wallet")
    Optional<WalletTransaction> findByTransactionReferenceAndWallet_Owner_Email(
            String transactionReference,
            String email
    );

    @EntityGraph(attributePaths = "wallet")
    @Query("SELECT t FROM WalletTransaction t")
    Page<WalletTransaction> findAllTransactions(Pageable pageable);
}
