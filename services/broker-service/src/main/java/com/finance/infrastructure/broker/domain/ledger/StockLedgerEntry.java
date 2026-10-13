package com.finance.infrastructure.broker.domain.ledger;

import com.finance.infrastructure.broker.domain.account.StockHolding;
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

import java.time.LocalDateTime;
import java.util.Objects;

// 보유 주식의 수량 변화를 기록하는 종목 원장 엔티티
@Entity
@Table(name = "stock_ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 증권 계좌가 보유중인 종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_holding_id", nullable = false)
    private StockHolding stockHolding;

    // 종목 원장에 기록된 기록이 어떤 거래로 인해 발생한 기록인지를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StockLedgerEntryType entryType;

    // 해당 종목 원장기록을 발생시킨 거래의 Id(주식 매매)
    @Column(nullable = false, updatable = false, length = 64)
    private String referenceId;

    // 거래 종목 수량
    @Column(nullable = false)
    private long quantity;

    // 거래 후 결제완료 보유수량
    @Column(nullable = false)
    private long settledAfter;

    // 거래 후 미체결 매도주문 예약수량
    @Column(nullable = false)
    private long orderLockedAfter;

    // 거래 후 매수체결 결제예정 수령수량
    @Column(nullable = false)
    private long pendingReceiptAfter;

    // 거래 후 매도체결 결제예정 인도수량
    @Column(nullable = false)
    private long pendingDeliveryAfter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StockLedgerEntry(
            StockHolding stockHolding,
            StockLedgerEntryType entryType,
            String referenceId,
            long quantity
    ) {
        this.stockHolding = Objects.requireNonNull(stockHolding, "stock holding must not be null");
        this.entryType = Objects.requireNonNull(entryType, "stock ledger entry type must not be null");
        this.referenceId = requireText(referenceId, "reference id");
        this.quantity = requirePositive(quantity);
        this.settledAfter = stockHolding.getSettledQuantity();
        this.orderLockedAfter = stockHolding.getOrderLockedQuantity();
        this.pendingReceiptAfter = stockHolding.getPendingReceiptQuantity();
        this.pendingDeliveryAfter = stockHolding.getPendingDeliveryQuantity();
        this.createdAt = LocalDateTime.now();
    }

    private static long requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("ledger quantity must be positive");
        }
        return quantity;
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
