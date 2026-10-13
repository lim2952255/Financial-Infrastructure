package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.ledger.StockLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockLedgerEntryRepository extends JpaRepository<StockLedgerEntry, Long> {

    List<StockLedgerEntry> findAllByStockHoldingIdOrderByIdAsc(Long stockHoldingId);
}
