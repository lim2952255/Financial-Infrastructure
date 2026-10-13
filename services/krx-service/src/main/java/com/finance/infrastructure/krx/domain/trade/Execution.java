package com.finance.infrastructure.krx.domain.trade;

import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.krx.domain.clearing.ClearingCycle;
import com.finance.infrastructure.krx.domain.order.ExchangeOrder;
import com.finance.infrastructure.krx.domain.order.OrderSide;
import com.finance.infrastructure.krx.domain.security.ListedSecurity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// KRX에서 매수주문과 매도주문이 매칭되어 생성된 체결정보를 저장하는 엔티티
@Entity
@Table(name = "krx_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 체결 건마다 발급하는 고유 식별자
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String executionId;

    // 체결에 참여한 매수주문
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buy_order_id", nullable = false, updatable = false)
    private ExchangeOrder buyOrder;

    // 체결에 참여한 매도주문
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_order_id", nullable = false, updatable = false)
    private ExchangeOrder sellOrder;

    // 매수주문과 매도주문이 거래한 상장종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false, updatable = false)
    private ListedSecurity security;

    // 주문의 지정가 범위 안에서 결정된 체결가격 (호가창을 기반으로 체결)
    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal priceValue;

    // 매수주문과 매도주문에서 실제로 체결된 수량 (호가창을 기반으로 체결)
    @Column(nullable = false, updatable = false)
    private long quantity;

    // KRX의 주문 매칭으로 체결이 성립된 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;

    // 체결이 발생한 거래일
    @Column(nullable = false, updatable = false)
    private LocalDate tradeDate;

    // 체결된 증권과 현금을 실제로 이전할 결제 예정일 (결제 예정일은 체결이 발생한 거래일 + 2일 뒤에 수행하는것이 정책이다)
    @Column(nullable = false, updatable = false)
    private LocalDate settlementDate;

    // 체결정보가 포함된 상계 주기
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearing_cycle_id")
    private ClearingCycle clearingCycle;

    public Execution(
            String executionId,
            ExchangeOrder buyOrder,
            ExchangeOrder sellOrder,
            ListedSecurity security,
            Money price,
            long quantity,
            LocalDateTime executedAt,
            LocalDate tradeDate,
            LocalDate settlementDate
    ) {
        this.executionId = requireText(executionId, "execution id");
        this.buyOrder = requireOrderSide(buyOrder, OrderSide.BUY, "buy order");
        this.sellOrder = requireOrderSide(sellOrder, OrderSide.SELL, "sell order");
        if (buyOrder.getExchangeOrderId().equals(sellOrder.getExchangeOrderId())) {
            throw new IllegalArgumentException("buy and sell orders must be different");
        }
        this.security = Objects.requireNonNull(security, "security must not be null");
        requireSecurity(buyOrder, security, "buy order");
        requireSecurity(sellOrder, security, "sell order");
        this.priceValue = requireExecutionPrice(price, buyOrder, sellOrder).amount();
        this.quantity = requirePositive(quantity, "execution quantity");
        if (quantity > buyOrder.getOriginalQuantity() || quantity > sellOrder.getOriginalQuantity()) {
            throw new IllegalArgumentException("execution quantity exceeds an order quantity");
        }
        this.executedAt = Objects.requireNonNull(executedAt, "executed at must not be null");
        this.tradeDate = Objects.requireNonNull(tradeDate, "trade date must not be null");
        if (!executedAt.toLocalDate().equals(tradeDate)) {
            throw new IllegalArgumentException("execution time must belong to trade date");
        }
        this.settlementDate = requireSettlementDate(tradeDate, settlementDate);
    }

    public Money price() {
        return new Money(priceValue);
    }

    // 총 체결금액: 체결가격 * 체결수량
    public Money amount() {
        return Money.of(priceValue.multiply(BigDecimal.valueOf(quantity)));
    }

    // 체결정보를 상계 주기에 포함한다. 하나의 체결은 서로 다른 상계 주기에 중복 배정될 수 없다. 이때 해당 엔티티의 clearingCycle에 상계주기 id를 추가한다.
    public void assignTo(ClearingCycle clearingCycle) {
        Objects.requireNonNull(clearingCycle, "clearing cycle must not be null");
        if (this.clearingCycle != null
                && !this.clearingCycle.getCycleId().equals(clearingCycle.getCycleId())) {
            throw new IllegalStateException("execution is already assigned to another clearing cycle");
        }
        this.clearingCycle = clearingCycle;
    }

    private static ExchangeOrder requireOrderSide(ExchangeOrder order, OrderSide side, String fieldName) {
        Objects.requireNonNull(order, fieldName + " must not be null");
        if (order.getSide() != side) {
            throw new IllegalArgumentException(fieldName + " must have " + side + " side");
        }
        return order;
    }

    private static void requireSecurity(ExchangeOrder order, ListedSecurity security, String fieldName) {
        if (!order.getSecurity().getSecurityCode().equals(security.getSecurityCode())) {
            throw new IllegalArgumentException(fieldName + " security does not match execution security");
        }
    }

    private static Money requireExecutionPrice(Money price, ExchangeOrder buyOrder, ExchangeOrder sellOrder) {
        Objects.requireNonNull(price, "execution price must not be null");
        if (price.amount().signum() <= 0) {
            throw new IllegalArgumentException("execution price must be positive");
        }
        if (price.amount().compareTo(buyOrder.limitPrice().amount()) > 0
                || price.amount().compareTo(sellOrder.limitPrice().amount()) < 0) {
            throw new IllegalArgumentException("execution price violates an order limit price");
        }
        return price;
    }

    private static LocalDate requireSettlementDate(LocalDate tradeDate, LocalDate settlementDate) {
        Objects.requireNonNull(settlementDate, "settlement date must not be null");
        if (settlementDate.isBefore(tradeDate)) {
            throw new IllegalArgumentException("settlement date must not be before trade date");
        }
        return settlementDate;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
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
