package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.settlement.SecuritiesSettlementBatch;
import com.finance.infrastructure.krx.domain.settlement.SettlementBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecuritiesSettlementBatchRepository
        extends JpaRepository<SecuritiesSettlementBatch, Long> {

    Optional<SecuritiesSettlementBatch> findByBatchId(String batchId);

    Optional<SecuritiesSettlementBatch> findByClearingCycle_Id(Long clearingCycleId);

    List<SecuritiesSettlementBatch> findAllByStatusOrderByCreatedAtAsc(SettlementBatchStatus status);
}
