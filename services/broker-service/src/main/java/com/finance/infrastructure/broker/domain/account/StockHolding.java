package com.finance.infrastructure.broker.domain.account;

import com.finance.infrastructure.broker.domain.security.TradableSecurity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.Objects;

// 각 증권계좌의 결제완료 주식과 주문·미결제 수량을 종목별로 관리하는 엔티티
@Entity
@Table(
        name = "stock_holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_holding_account_security",
                columnNames = {"securities_account_id", "security_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "securities_account_id", nullable = false)
    private SecuritiesAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private TradableSecurity security;

    // KSD 증권결제가 완료되어 고객원장에 최종 반영된 수량
    @Column(nullable = false)
    private long settledQuantity;

    // 아직 체결되지 않은 매도 주문을 위해 매도가능수량에서 예약한 수량
    @Column(nullable = false)
    private long orderLockedQuantity;

    // 매수 체결 후 결제일에 수령할 예정이며 결제 전에도 다시 매도할 수 있는 수량
    @Column(nullable = false)
    private long pendingReceiptQuantity;

    // 매도 체결 후 결제일에 인도해야 하는 수량
    @Column(nullable = false)
    private long pendingDeliveryQuantity;

    public StockHolding(SecuritiesAccount account, TradableSecurity security) {
        this.account = Objects.requireNonNull(account, "securities account must not be null");
        this.security = Objects.requireNonNull(security, "tradable security must not be null");
    }

    // 다른 증권사로부터의 입고 등으로 결제완료 보유수량을 증가시킨다.
    public long deposit(long quantity) {
        requirePositive(quantity);
        settledQuantity = Math.addExact(settledQuantity, quantity);
        return settledQuantity;
    }

    // 결제완료 수량 중 미결제 인도의무와 주문예약에 사용되지 않은 수량을 출고한다.
    public long withdraw(long quantity) {
        requirePositive(quantity);
        if (withdrawableQuantity() < quantity) {
            throw new IllegalArgumentException("insufficient withdrawable stock");
        }
        settledQuantity -= quantity;
        return settledQuantity;
    }

    // 매도 주문 접수 시 결제 전 매수수량까지 포함한 매도가능수량에서 주문수량을 예약한다.
    public void lockForSellOrder(long quantity) {
        requirePositive(quantity);
        if (sellableQuantity() < quantity) {
            throw new IllegalArgumentException("insufficient sellable stock");
        }
        orderLockedQuantity = Math.addExact(orderLockedQuantity, quantity);
    }

    // 주문 취소 또는 거부 시 미체결 수량의 주문예약을 해제한다.
    public void releaseSellOrderLock(long quantity) {
        requirePositive(quantity);
        if (orderLockedQuantity < quantity) {
            throw new IllegalArgumentException("insufficient order-locked stock");
        }
        orderLockedQuantity -= quantity;
    }

    // 매수 체결수량을 결제예정 수령수량으로 기록하여 결제 전에도 매도가능수량에 포함한다.
    public void recordBuyExecution(long quantity) {
        requirePositive(quantity);
        pendingReceiptQuantity = Math.addExact(pendingReceiptQuantity, quantity);
    }

    // 매도 체결수량을 주문예약에서 결제예정 인도수량으로 전환한다.
    public void recordSellExecution(long quantity) {
        requirePositive(quantity);
        if (orderLockedQuantity < quantity) {
            throw new IllegalArgumentException("insufficient order-locked stock");
        }
        long newPendingDeliveryQuantity = Math.addExact(pendingDeliveryQuantity, quantity);
        orderLockedQuantity -= quantity;
        pendingDeliveryQuantity = newPendingDeliveryQuantity;
    }

    /**
     * 같은 결제일의 매수·매도 수량을 함께 반영한다.
     * 결제 전 매수주식을 다시 매도한 경우에도 수령과 인도를 순액으로 처리할 수 있다.
     */
    public void settle(long receiptQuantity, long deliveryQuantity) {
        requireNonNegative(receiptQuantity, "receipt quantity");
        requireNonNegative(deliveryQuantity, "delivery quantity");
        if (receiptQuantity == 0 && deliveryQuantity == 0) {
            throw new IllegalArgumentException("settlement quantity must not be zero");
        }
        if (pendingReceiptQuantity < receiptQuantity) {
            throw new IllegalArgumentException("insufficient pending stock receipt");
        }
        if (pendingDeliveryQuantity < deliveryQuantity) {
            throw new IllegalArgumentException("insufficient pending stock delivery");
        }

        long newSettledQuantity = Math.subtractExact(
                Math.addExact(settledQuantity, receiptQuantity),
                deliveryQuantity
        );
        if (newSettledQuantity < 0) {
            throw new IllegalArgumentException("insufficient settled stock for settlement");
        }
        pendingReceiptQuantity -= receiptQuantity;
        pendingDeliveryQuantity -= deliveryQuantity;
        settledQuantity = newSettledQuantity;
    }

    // 결제 전 매수수량을 포함하고 이미 매도됐거나 주문에 예약된 수량을 제외한 신규 매도가능수량
    public long sellableQuantity() {
        return Math.subtractExact(
                Math.subtractExact(
                        Math.addExact(settledQuantity, pendingReceiptQuantity),
                        pendingDeliveryQuantity
                ),
                orderLockedQuantity
        );
    }

    // 미결제 매수수량은 제외하고 현재 다른 증권사로 출고할 수 있는 결제완료 수량
    public long withdrawableQuantity() {
        return Math.min(settledQuantity, Math.max(sellableQuantity(), 0));
    }

    // 주문예약을 제외하고 체결까지 반영한 고객의 순주식자산
    public long totalQuantity() {
        return Math.subtractExact(
                Math.addExact(settledQuantity, pendingReceiptQuantity),
                pendingDeliveryQuantity
        );
    }

    private static void requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("stock quantity must be positive");
        }
    }

    private static void requireNonNegative(long quantity, String fieldName) {
        if (quantity < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
