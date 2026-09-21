package com.financialblockchain.simulator.bank.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financialblockchain.simulator.bank.support.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

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
