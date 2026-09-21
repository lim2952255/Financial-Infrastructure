package com.financialblockchain.simulator.bank.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financialblockchain.simulator.bank.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 외부 인프라 없이 계좌의 입금·출금 규칙만 검증한다. */
class AccountTest {

    @Test
    void withdrawAndDepositChangeBalance() {
        Account account = Account.open("BANK_A", "A-1001", "Alice", new BigDecimal("1000.00"));

        account.withdraw(new BigDecimal("300.00"));
        account.deposit(new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("750.00");
    }

    @Test
    void cannotWithdrawMoreThanBalance() {
        Account account = Account.open("BANK_A", "A-1001", "Alice", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("101.00")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
