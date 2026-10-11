package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementSubmissionRepository extends JpaRepository<SettlementSubmission, Long> {

    Optional<SettlementSubmission> findByClearingCycle(ClearingCycle clearingCycle);
}
