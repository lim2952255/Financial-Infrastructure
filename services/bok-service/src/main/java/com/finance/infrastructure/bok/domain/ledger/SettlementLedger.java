package com.finance.infrastructure.bok.domain.ledger;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// 특정 settlement_batch를 기반으로 실제로 당좌예금 계좌가 어떻게 변했는지를 저장하는 원장 엔티티
@Entity
@Table(
        name = "settlement_ledger_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_ledger_batch_account",
                columnNames = {"settlement_batch_id", "settlement_account_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 각 Ledger는 KFTC가 보낸 결제 요청정보 묶음을 기반으로 결제한 뒤 원장을 기록한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_batch_id", nullable = false)
    private SettlementBatch settlementBatch;

    // 결제 계좌
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_account_id", nullable = false)
    private SettlementAccount settlementAccount;

    // 입금(대변), 출금(차변)인지를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementLedgerEntryType entryType;

    // 결제액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal amountValue;

    // 결제 후 잔액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal balanceAfterValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SettlementLedger(
            SettlementBatch settlementBatch,
            SettlementAccount settlementAccount,
            SettlementLedgerEntryType entryType,
            Money amount,
            Money balanceAfter
    ) {
        this.settlementBatch = Objects.requireNonNull(
                settlementBatch,
                "settlement batch must not be null"
        );
        this.settlementAccount = Objects.requireNonNull(
                settlementAccount,
                "settlement account must not be null"
        );
        this.entryType = Objects.requireNonNull(entryType, "entry type must not be null");
        requireValidAmounts(settlementAccount, amount, balanceAfter);
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
            SettlementAccount account,
            Money amount,
            Money balanceAfter
    ) {
        Objects.requireNonNull(amount, "ledger amount must not be null");
        Objects.requireNonNull(balanceAfter, "balance after must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("ledger amount must be positive");
        }
        if (!balanceAfter.equals(account.balance())) {
            throw new IllegalArgumentException(
                    "ledger balance must match the current settlement account balance"
            );
        }
    }
}
