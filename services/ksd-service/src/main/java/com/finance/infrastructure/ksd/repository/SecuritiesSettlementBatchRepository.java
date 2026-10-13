package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.ksd.domain.settlement.SecuritiesSettlementBatch;
import com.finance.infrastructure.ksd.domain.settlement.SecuritiesSettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SecuritiesSettlementBatchRepository
        extends JpaRepository<SecuritiesSettlementBatch, Long> {

    Optional<SecuritiesSettlementBatch> findByBatchId(String batchId);

    Optional<SecuritiesSettlementBatch> findByClearingReferenceId(String clearingReferenceId);

    List<SecuritiesSettlementBatch> findAllBySettlementDateAndStatusOrderByReceivedAtAsc(
            LocalDate settlementDate,
            SecuritiesSettlementStatus status
    );
}
