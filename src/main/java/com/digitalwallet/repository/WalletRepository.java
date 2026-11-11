package com.digitalwallet.repository;

import com.digitalwallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByOwner_Email(String email);

    boolean existsByOwner_Id(Long userId);

    boolean existsByWalletNumber(String walletNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM Wallet w
            WHERE w.owner.email = :email
            """)
    Optional<Wallet> findByOwnerEmailForUpdate(@Param("email") String email);

    Optional<Wallet> findByWalletNumber(String walletNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM Wallet w
            WHERE w.id = :walletId
            """)
    Optional<Wallet> findByIdForUpdate(@Param("walletId") Long walletId);
}
