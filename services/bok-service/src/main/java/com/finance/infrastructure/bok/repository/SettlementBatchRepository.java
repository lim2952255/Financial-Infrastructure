package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

    Optional<SettlementBatch> findByBatchId(String batchId);
}
