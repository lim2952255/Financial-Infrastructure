package com.finance.infrastructure.krx.domain.order;

import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.krx.domain.participant.ExchangeParticipant;
import com.finance.infrastructure.krx.domain.security.ListedSecurity;
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

/**
 * 증권사로부터 접수한 주문정보와 체결상태를 관리하는 KRX 주문 원장 엔티티
 * 증권사가 발급한 주문번호는 참가 기관별로 유일하며, KRX는 접수된 주문마다 별도의 거래소 주문번호를 발급한다.
 */
@Entity
@Table(
        name = "exchange_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exchange_order_participant_broker_order",
                columnNames = {"participant_id", "broker_order_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 주문 접수 후 발급한 주문번호
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String exchangeOrderId;

    // 주문을 제출한 KRX 거래 참가기관
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private ExchangeParticipant participant;

    // 증권사가 자체적으로 발급한 주문번호
    @Column(name = "broker_order_id", nullable = false, updatable = false, length = 64)
    private String brokerOrderId;

    // 주문 대상 상장종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false, updatable = false)
    private ListedSecurity security;

    // 매수 또는 매도 주문 포지션
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 8)
    private OrderSide side;

    // 접수한 주문가격
    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal limitPriceValue;

    // 접수한 전체 주문수량
    @Column(nullable = false, updatable = false)
    private long originalQuantity;

    // 현재까지 체결된 누적 수량
    @Column(nullable = false)
    private long filledQuantity;

    // 주문의 접수·체결·취소·거부 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ExchangeOrderStatus status;

    // 주문접수가 거부된 이유
    @Column(length = 255)
    private String rejectionReason;

    // 동일 가격 주문의 시간 우선순위를 결정하는 KRX 접수 순번
    // 동일 가격 주문에 대해서는 먼저 주문한 주문이 더 높은 우선순위를 가진다.
    @Column(nullable = false, unique = true, updatable = false)
    private long receivedSequence;

    // KRX가 주문을 접수한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    // 주문 상태가 마지막으로 변경된 시각
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ExchangeOrder(
            String exchangeOrderId,
            ExchangeParticipant participant,
            String brokerOrderId,
            ListedSecurity security,
            OrderSide side,
            Money limitPrice,
            long quantity,
            long receivedSequence
    ) {
        this.exchangeOrderId = requireText(exchangeOrderId, "exchange order id");
        this.participant = Objects.requireNonNull(participant, "exchange participant must not be null");
        this.participant.validateTradingPermission();
        this.brokerOrderId = requireText(brokerOrderId, "broker order id");
        this.security = Objects.requireNonNull(security, "listed security must not be null");
        this.side = Objects.requireNonNull(side, "order side must not be null");
        Money validatedPrice = requirePositivePrice(limitPrice);
        security.validateOrder(quantity, validatedPrice);
        this.limitPriceValue = validatedPrice.amount();
        this.originalQuantity = quantity;
        this.receivedSequence = requirePositiveSequence(receivedSequence);
        this.status = ExchangeOrderStatus.OPEN;
        this.receivedAt = LocalDateTime.now();
        this.updatedAt = receivedAt;
    }

    // 주문의 지정가격을 Money 값 객체로 반환한다.
    public Money limitPrice() {
        return new Money(limitPriceValue);
    }

    // 전체 주문수량에서 지금까지 체결된 수량을 제외한 미체결 수량을 계산한다.
    public long remainingQuantity() {
        return originalQuantity - filledQuantity;
    }

    // 추가 체결이나 취소가 가능한 활성 주문인지 확인한다.
    public boolean isActive() {
        return status == ExchangeOrderStatus.OPEN || status == ExchangeOrderStatus.PARTIALLY_FILLED;
    }

    // 이번에 체결된 수량을 누적하고 잔여수량이 남아있는지에 따라 부분체결 또는 전량체결 상태로 변경한다.
    public void fill(long quantity) {
        requireActive();
        if (quantity <= 0) {
            throw new IllegalArgumentException("fill quantity must be positive");
        }
        if (quantity > remainingQuantity()) {
            throw new IllegalArgumentException("fill quantity exceeds remaining order quantity");
        }

        filledQuantity = Math.addExact(filledQuantity, quantity);
        status = remainingQuantity() == 0
                ? ExchangeOrderStatus.FILLED
                : ExchangeOrderStatus.PARTIALLY_FILLED;
        touch();
    }

    // 체결 가능한 활성 주문의 잔여 수량을 취소한다.
    public void cancel() {
        requireActive();
        status = ExchangeOrderStatus.CANCELLED;
        touch();
    }

    // 아직 체결되지 않은 신규 주문의 접수를 거부하고 사유를 기록한다.
    public void reject(String reason) {
        if (status != ExchangeOrderStatus.OPEN || filledQuantity != 0) {
            throw new IllegalStateException("only an unfilled open order can be rejected");
        }
        rejectionReason = requireText(reason, "rejection reason");
        status = ExchangeOrderStatus.REJECTED;
        touch();
    }

    // 현재 주문이 추가 체결이나 취소가 가능한 상태인지 검사한다.
    private void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("order is not active: " + status);
        }
    }

    // 주문정보가 마지막으로 변경된 시각을 update한다.
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

    private static long requirePositiveSequence(long sequence) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("received sequence must be positive");
        }
        return sequence;
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
