package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.settlement.CashSettlementBatch;
import com.finance.infrastructure.krx.domain.settlement.SettlementBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashSettlementBatchRepository extends JpaRepository<CashSettlementBatch, Long> {

    Optional<CashSettlementBatch> findByBatchId(String batchId);

    Optional<CashSettlementBatch> findByClearingCycle_Id(Long clearingCycleId);

    List<CashSettlementBatch> findAllByStatusOrderByCreatedAtAsc(SettlementBatchStatus status);
}
