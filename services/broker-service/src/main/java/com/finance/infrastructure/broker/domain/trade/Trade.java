package com.finance.infrastructure.broker.domain.trade;

import com.finance.infrastructure.broker.domain.order.BrokerOrder;
import com.finance.infrastructure.broker.domain.order.OrderSide;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// KRX에서 체결된 거래정보를 저장하는 엔티티
@Entity
@Table(name = "broker_trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 체결된 거래의 Id
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String tradeId;

    // 어떤 주문의 거래가 체결되었는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_order_id", nullable = false)
    private BrokerOrder order;

    // 체결수량
    @Column(nullable = false, updatable = false)
    private long quantity;

    // 체결금액
    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal priceValue;

    // 체결일자
    @Column(nullable = false, updatable = false)
    private LocalDate tradeDate;

    // 결제일자
    @Column(nullable = false, updatable = false)
    private LocalDate settlementDate;

    // 거래의 상계 및 결제상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SettlementStatus settlementStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime settledAt;

    // 상계 및 결제 실패 이유
    @Column(length = 255)
    private String failureReason;

    public Trade(
            String tradeId,
            BrokerOrder order,
            long quantity,
            Money price,
            LocalDate tradeDate,
            LocalDate settlementDate
    ) {
        this.tradeId = requireText(tradeId, "trade id");
        this.order = Objects.requireNonNull(order, "broker order must not be null");
        this.quantity = requirePositiveQuantity(quantity);
        Money executionPrice = requirePositivePrice(price);
        requirePriceWithinLimit(order, executionPrice);
        this.priceValue = executionPrice.amount();
        this.tradeDate = Objects.requireNonNull(tradeDate, "trade date must not be null");
        this.settlementDate = requireSettlementDate(tradeDate, settlementDate);
        this.order.recordFill(quantity);
        this.settlementStatus = SettlementStatus.PENDING_CLEARING;
        this.createdAt = LocalDateTime.now();
    }

    public Money price() {
        return new Money(priceValue);
    }

    // 총 거래액: price * quantity
    public Money amount() {
        return Money.of(priceValue.multiply(BigDecimal.valueOf(quantity)));
    }

    // 거래의 상계 및 결제상태를 update하는 메서드
    public void clear() {
        requireStatus(SettlementStatus.PENDING_CLEARING);
        settlementStatus = SettlementStatus.CLEARED;
    }

    public void submitForSettlement() {
        requireStatus(SettlementStatus.CLEARED);
        settlementStatus = SettlementStatus.SETTLEMENT_PENDING;
    }

    public void settle() {
        requireStatus(SettlementStatus.SETTLEMENT_PENDING);
        settlementStatus = SettlementStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (settlementStatus == SettlementStatus.SETTLED || settlementStatus == SettlementStatus.FAILED) {
            throw new IllegalStateException("terminal trade settlement cannot fail again");
        }
        failureReason = requireText(reason, "failure reason");
        settlementStatus = SettlementStatus.FAILED;
    }

    private void requireStatus(SettlementStatus expected) {
        if (settlementStatus != expected) {
            throw new IllegalStateException(
                    "trade settlement must be " + expected + " but was " + settlementStatus
            );
        }
    }

    private static LocalDate requireSettlementDate(LocalDate tradeDate, LocalDate settlementDate) {
        Objects.requireNonNull(settlementDate, "settlement date must not be null");
        if (settlementDate.isBefore(tradeDate)) {
            throw new IllegalArgumentException("settlement date must not be before trade date");
        }
        return settlementDate;
    }

    private static Money requirePositivePrice(Money price) {
        Objects.requireNonNull(price, "trade price must not be null");
        if (price.amount().signum() <= 0) {
            throw new IllegalArgumentException("trade price must be positive");
        }
        return price;
    }

    private static void requirePriceWithinLimit(BrokerOrder order, Money executionPrice) {
        int comparison = executionPrice.amount().compareTo(order.limitPrice().amount());
        boolean violatesLimit = order.getSide() == OrderSide.BUY
                ? comparison > 0
                : comparison < 0;
        if (violatesLimit) {
            throw new IllegalArgumentException("trade price violates the order limit price");
        }
    }

    private static long requirePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("trade quantity must be positive");
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
