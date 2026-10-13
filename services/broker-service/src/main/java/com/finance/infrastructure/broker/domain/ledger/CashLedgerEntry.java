package com.finance.infrastructure.broker.domain.ledger;

import com.finance.infrastructure.broker.domain.account.CashBalance;
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

// 예수금의 변화를 기록하는 예수금 원장 엔티티
@Entity
@Table(name = "cash_ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 특정 증권계좌가 보유하고 있는 예수금 현황을 저장하고 있는 엔티티
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_balance_id", nullable = false)
    private CashBalance cashBalance;

    // 예수금 원장에 기록된 기록이 어떤 거래로 인해 발생한 기록인지를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CashLedgerEntryType entryType;

    // 해당 예수금 원장기록을 발생시킨 거래의 Id(예수금 입출금 or 주식매매)
    @Column(nullable = false, updatable = false, length = 64)
    private String referenceId;

    // 거래 금액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal amountValue;

    // 거래 후 결제완료 예수금
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal settledAfterValue;

    // 거래 후 미체결 매수주문 예약금액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal orderLockedAfterValue;

    // 거래 후 매수체결 결제예정 지급액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal pendingDebitAfterValue;

    // 거래 후 매도체결 결제예정 수령액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal pendingCreditAfterValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CashLedgerEntry(
            CashBalance cashBalance,
            CashLedgerEntryType entryType,
            String referenceId,
            Money amount
    ) {
        this.cashBalance = Objects.requireNonNull(cashBalance, "cash balance must not be null");
        this.entryType = Objects.requireNonNull(entryType, "cash ledger entry type must not be null");
        this.referenceId = requireText(referenceId, "reference id");
        this.amountValue = requirePositive(amount).amount();
        this.settledAfterValue = cashBalance.settled().amount();
        this.orderLockedAfterValue = cashBalance.orderLocked().amount();
        this.pendingDebitAfterValue = cashBalance.pendingDebit().amount();
        this.pendingCreditAfterValue = cashBalance.pendingCredit().amount();
        this.createdAt = LocalDateTime.now();
    }

    public Money amount() {
        return new Money(amountValue);
    }

    public Money settledAfter() {
        return new Money(settledAfterValue);
    }

    public Money orderLockedAfter() {
        return new Money(orderLockedAfterValue);
    }

    public Money pendingDebitAfter() {
        return new Money(pendingDebitAfterValue);
    }

    public Money pendingCreditAfter() {
        return new Money(pendingCreditAfterValue);
    }

    private static Money requirePositive(Money amount) {
        Objects.requireNonNull(amount, "ledger amount must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("ledger amount must be positive");
        }
        return amount;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
