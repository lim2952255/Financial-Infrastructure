package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.ledger.CashLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashLedgerEntryRepository extends JpaRepository<CashLedgerEntry, Long> {

    List<CashLedgerEntry> findAllByCashBalanceIdOrderByIdAsc(Long cashBalanceId);
}
