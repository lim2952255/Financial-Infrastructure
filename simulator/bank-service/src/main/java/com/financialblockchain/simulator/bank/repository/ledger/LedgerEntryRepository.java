package com.financialblockchain.simulator.bank.repository.ledger;

import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntry;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 원장 항목을 저장하고 거래 ID를 기준으로 대사 대상 기록을 조회한다. */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByTransactionIdAndType(String transactionId, LedgerEntryType type);

    List<LedgerEntry> findByTransactionIdOrderById(String transactionId);

    List<LedgerEntry> findAllByOrderById();
}
