package com.finance.infrastructure.broker.domain.order;

import com.finance.infrastructure.broker.domain.account.SecuritiesAccount;
import com.finance.infrastructure.broker.domain.security.TradableSecurity;
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

// 고객이 증권사 요청한 주문정보를 담은 엔티티
@Entity
@Table(name = "broker_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrokerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 증권사가 자체적으로 발급한 주문번호
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String orderId;

    // 거래소가 주문 접수 후 발급한 주문번호
    @Column(unique = true, length = 64)
    private String exchangeOrderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "securities_account_id", nullable = false)
    private SecuritiesAccount account;

    // 거래할 종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private TradableSecurity security;

    // 매수포지션 or 매도 포지션
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 8)
    private OrderSide side;

    // 고객이 지정한 주문가격
    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal limitPriceValue;

    // 고객이 처음 주문한 전체 수량
    @Column(nullable = false, updatable = false)
    private long originalQuantity;

    // 현재까지 체결된 누적 수량
    @Column(nullable = false)
    private long filledQuantity;

    // 주문상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    // 주문접수가 거부된 이유
    @Column(length = 255)
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public BrokerOrder(
            String orderId,
            SecuritiesAccount account,
            TradableSecurity security,
            OrderSide side,
            Money limitPrice,
            long quantity
    ) {
        this.orderId = requireText(orderId, "order id");
        this.account = Objects.requireNonNull(account, "securities account must not be null");
        this.security = Objects.requireNonNull(security, "tradable security must not be null");
        this.side = Objects.requireNonNull(side, "order side must not be null");
        this.limitPriceValue = requirePositivePrice(limitPrice).amount();
        this.security.validateOrder(quantity);
        this.originalQuantity = quantity;
        this.status = OrderStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public Money limitPrice() {
        return new Money(limitPriceValue);
    }

    // 총 주문금액: limitPriceValue * originalQuantity
    public Money maximumOrderAmount() {
        return Money.of(limitPriceValue.multiply(BigDecimal.valueOf(originalQuantity)));
    }

    // 현재까지 미체결된 주문수량을 리턴하는 메서드
    public long remainingQuantity() {
        return originalQuantity - filledQuantity;
    }

    // KRX가 주문을 접수한 뒤, KRX에서 발급한 주문번호를 등록하는 메서드
    public void assignExchangeOrderId(String exchangeOrderId) {
        String normalized = requireText(exchangeOrderId, "exchange order id");
        if (this.exchangeOrderId == null) {
            this.exchangeOrderId = normalized;
            touch();
            return;
        }
        if (!this.exchangeOrderId.equals(normalized)) {
            throw new IllegalStateException("exchange order id is already assigned");
        }
    }

    // 거래소에서 체결통보가 왔을때, 이번에 체결된 수량을 누적하는 메서드
    public void recordFill(long quantity) {
        requireFillable();
        requirePositiveQuantity(quantity);
        if (quantity > remainingQuantity()) {
            throw new IllegalArgumentException("fill quantity exceeds remaining order quantity");
        }
        filledQuantity = Math.addExact(filledQuantity, quantity);
        status = filledQuantity == originalQuantity
                ? OrderStatus.FILLED
                : OrderStatus.PARTIALLY_FILLED;
        touch();
    }

    // 주문의 상태를 update하는 메서드
    public void cancel() {
        if (status != OrderStatus.OPEN && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("only an open order can be cancelled");
        }
        status = OrderStatus.CANCELLED;
        touch();
    }

    public void reject(String reason) {
        if (status != OrderStatus.OPEN || filledQuantity != 0) {
            throw new IllegalStateException("only an unfilled open order can be rejected");
        }
        rejectionReason = requireText(reason, "rejection reason");
        status = OrderStatus.REJECTED;
        touch();
    }

    // 현재 주문에 추가체결을 반영해도 되는 상태인지를 검사한다.(추가체결이 가능하려면 주문의 상태가 OPEN 또는 PARTIALLY_FILLED여야 한다.)
    private void requireFillable() {
        if (status != OrderStatus.OPEN && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("order is not fillable: " + status);
        }
    }

    // Order 엔티티를 변경한 시간을 update한다.
    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    private static Money requirePositivePrice(Money price) {
        Objects.requireNonNull(price, "limit price must not be null");
        if (price.amount().signum() <= 0) {
            throw new IllegalArgumentException("limit price must be positive");
        }
        return price;
    }

    private static long requirePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("order quantity must be positive");
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
