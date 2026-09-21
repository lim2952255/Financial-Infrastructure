package com.financialblockchain.simulator.bank.repository.account;

import com.financialblockchain.simulator.bank.domain.account.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 계좌 영속화와 동시 잔액 변경을 위한 잠금 조회를 제공한다. */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    // 출금과 입금이 동시에 같은 계좌 잔액을 덮어쓰는 상황을 방지한다.
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
