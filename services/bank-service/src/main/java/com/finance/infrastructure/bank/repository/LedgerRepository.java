package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.ledger.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findAllByBankTransactionIdOrderByIdAsc(Long bankTransactionId);
}
