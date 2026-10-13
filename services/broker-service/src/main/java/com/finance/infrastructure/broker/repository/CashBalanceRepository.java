package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.account.CashBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {

    Optional<CashBalance> findByAccountId(Long accountId);
}
