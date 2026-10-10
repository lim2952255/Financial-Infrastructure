package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.account.DepositAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepositAccountRepository extends JpaRepository<DepositAccount, Long> {

    @Query("select account.id from DepositAccount account where account.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from DepositAccount account where account.id = :accountId")
    Optional<DepositAccount> findByIdForUpdate(@Param("accountId") Long accountId);
}
