package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

    @EntityGraph(attributePaths = "positions")
    List<SettlementBatch> findAllByOrderByIdAsc();

    Optional<SettlementBatch> findByBatchId(String batchId);
}
