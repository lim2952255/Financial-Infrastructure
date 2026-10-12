package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.ledger.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    @Query("""
            select ledger
            from Ledger ledger
            join fetch ledger.bankTransaction
            join fetch ledger.account
            order by ledger.id
            """)
    List<Ledger> findAllForMonitoring();

    List<Ledger> findAllByBankTransactionIdOrderByIdAsc(Long bankTransactionId);
}
