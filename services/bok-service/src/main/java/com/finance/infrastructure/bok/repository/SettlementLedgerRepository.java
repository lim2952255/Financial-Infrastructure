package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.ledger.SettlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementLedgerRepository extends JpaRepository<SettlementLedger, Long> {
}
