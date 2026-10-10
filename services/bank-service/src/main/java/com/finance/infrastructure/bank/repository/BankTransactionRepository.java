package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByRequestId(String requestId);
}
