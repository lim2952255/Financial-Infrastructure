package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementSubmissionRepository extends JpaRepository<SettlementSubmission, Long> {

    @Query("""
            select submission
            from SettlementSubmission submission
            join fetch submission.clearingCycle
            order by submission.id
            """)
    List<SettlementSubmission> findAllForMonitoring();

    Optional<SettlementSubmission> findByClearingCycle(ClearingCycle clearingCycle);
}
