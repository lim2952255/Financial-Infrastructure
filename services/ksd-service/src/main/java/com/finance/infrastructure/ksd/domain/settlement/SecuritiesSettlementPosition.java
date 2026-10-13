package com.finance.infrastructure.ksd.domain.settlement;

import com.finance.infrastructure.ksd.domain.account.SecuritiesPosition;
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

import java.util.Objects;

/**
 * KSD가 처리하는 (참가기관·종목)별 증권 순포지션
 *
 * 양수는 해당 계좌가 받을 수량, 음수는 해당 계좌가 인도할 수량을 의미한다.
 */
@Entity
@Table(name = "securities_settlement_positions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesSettlementPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // (참가기관·종목) 증권 순포지션이 속한 증권 결제배치
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_batch_id", nullable = false, updatable = false)
    private SecuritiesSettlementBatch settlementBatch;

    // 결제 결과를 반영할 참가기관 계좌의 종목별 보유수량
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "securities_position_id", nullable = false, updatable = false)
    private SecuritiesPosition securitiesPosition;

    // 양수는 수령, 음수는 인도 의무를 의미하는 KRX의 순결제수량
    @Column(nullable = false, updatable = false)
    private long netQuantity;

    // 음수 순포지션을 이행하기 위해 실제로 예약한 수량.
    @Column(nullable = false)
    private long reservedQuantity;

    // 실제 결제한 순수량. 인도는 음수, 수령은 양수로 기록한다.
    @Column(nullable = false)
    private long settledQuantity;

    public SecuritiesSettlementPosition(SecuritiesPosition securitiesPosition, long netQuantity) {
        this.securitiesPosition = Objects.requireNonNull(
                securitiesPosition,
                "securities position must not be null"
        );
        if (netQuantity == 0) {
            throw new IllegalArgumentException("net quantity must not be zero");
        }
        if (netQuantity < 0) {
            Math.negateExact(netQuantity);
        }
        this.netQuantity = netQuantity;
    }

    void assignTo(SecuritiesSettlementBatch settlementBatch) {
        Objects.requireNonNull(settlementBatch, "settlement batch must not be null");
        if (this.settlementBatch != null) {
            throw new IllegalStateException("settlement position is already assigned to a batch");
        }
        this.settlementBatch = settlementBatch;
    }

    void validateReservation() {
        if (isDelivery()) {
            securitiesPosition.validateSettlementLock(deliveryQuantity());
        }
    }

    // 음수순포지션의 경우에는 해당 종목수량만큼 결제를 위해 lock을 획득한다.
    void reserveSecurities() {
        if (!isDelivery()) {
            return;
        }
        long quantity = deliveryQuantity();
        securitiesPosition.lockForSettlement(quantity);
        reservedQuantity = quantity;
    }

    void validateSettlement() {
        if (settledQuantity != 0) {
            throw new IllegalStateException("settlement position is already settled");
        }
        if (isDelivery()) {
            long quantity = deliveryQuantity();
            if (reservedQuantity != quantity) {
                throw new IllegalStateException("delivery quantity must be fully reserved");
            }
            securitiesPosition.validateLockedSettlementDelivery(quantity);
            return;
        }
        securitiesPosition.validateSettlementCredit(netQuantity);
    }

    void settle() {
        if (isDelivery()) {
            securitiesPosition.deliverLockedForSettlement(deliveryQuantity());
        } else {
            securitiesPosition.creditAvailable(netQuantity);
        }
        settledQuantity = netQuantity;
    }

    void releaseReservation() {
        if (reservedQuantity == 0) {
            return;
        }
        securitiesPosition.releaseSettlementLock(reservedQuantity);
        reservedQuantity = 0;
    }

    public boolean isDelivery() {
        return netQuantity < 0;
    }

    public long remainingQuantity() {
        return Math.subtractExact(netQuantity, settledQuantity);
    }

    private long deliveryQuantity() {
        return Math.negateExact(netQuantity);
    }
}
