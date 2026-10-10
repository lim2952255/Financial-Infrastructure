package com.finance.infrastructure.bank.domain.ledger;

import com.finance.infrastructure.bank.domain.account.DepositAccount;
import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_transaction_id", nullable = false)
    private BankTransaction bankTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private DepositAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BalanceChangeType changeType; // 예금이 증가했는지, 감소했는지를 나타내는 ENUM

    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal amountValue; // 거래액

    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal balanceAfterValue; // 거래 후 예금

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Ledger(
            BankTransaction bankTransaction,
            DepositAccount account,
            BalanceChangeType changeType,
            Money amount,
            Money balanceAfter
    ) {
        this.bankTransaction = Objects.requireNonNull(
                bankTransaction,
                "bank transaction must not be null"
        );
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.changeType = Objects.requireNonNull(changeType, "change type must not be null");
        requireValidAmounts(account, amount, balanceAfter);
        this.amountValue = amount.amount();
        this.balanceAfterValue = balanceAfter.amount();
        this.createdAt = LocalDateTime.now();
    }

    public Money amount() {
        return new Money(amountValue);
    }

    public Money balanceAfter() {
        return new Money(balanceAfterValue);
    }

    private static void requireValidAmounts(
            DepositAccount account,
            Money amount,
            Money balanceAfter
    ) {
        Objects.requireNonNull(amount, "ledger amount must not be null");
        Objects.requireNonNull(balanceAfter, "balance after must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("ledger amount must be positive");
        }
        if (!balanceAfter.equals(account.balance())) {
            throw new IllegalArgumentException("ledger balance must match the current account balance");
        }
    }
}
