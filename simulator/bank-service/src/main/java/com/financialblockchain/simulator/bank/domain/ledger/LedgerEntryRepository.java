package com.financialblockchain.simulator.bank.domain.ledger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByTransactionIdAndType(String transactionId, LedgerEntryType type);

    List<LedgerEntry> findByTransactionIdOrderById(String transactionId);

    List<LedgerEntry> findAllByOrderById();
}
