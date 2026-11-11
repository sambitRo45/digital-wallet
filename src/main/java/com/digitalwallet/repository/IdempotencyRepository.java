package com.digitalwallet.repository;

import com.digitalwallet.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUser_IdAndOperationAndIdempotencyKey(Long userId, String operation, String idempotencyKey);
}
