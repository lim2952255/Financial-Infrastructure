package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.account.SecuritiesAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecuritiesAccountRepository extends JpaRepository<SecuritiesAccount, Long> {

    Optional<SecuritiesAccount> findByAccountNumber(String accountNumber);
}
