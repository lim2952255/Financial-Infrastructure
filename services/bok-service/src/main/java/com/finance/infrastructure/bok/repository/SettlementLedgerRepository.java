package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.ledger.SettlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementLedgerRepository extends JpaRepository<SettlementLedger, Long> {

    @Query("""
            select ledger
            from SettlementLedger ledger
            join fetch ledger.settlementBatch
            join fetch ledger.settlementAccount account
            join fetch account.participant
            order by ledger.id
            """)
    List<SettlementLedger> findAllForMonitoring();
}
