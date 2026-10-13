package com.finance.infrastructure.ksd.domain.account;

import com.finance.infrastructure.ksd.domain.security.DepositorySecurity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// KSD 참가기관의 증권계좌가 종목별로 보유한 총 집계 수량을 저장한 엔티티
@Entity
@Table(
        name = "securities_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_position_account_security",
                columnNames = {"participant_account_id", "security_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KSD 참가 금융기관의 증권계좌
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_account_id", nullable = false)
    private ParticipantSecuritiesAccount account;

    // 종목정보
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private DepositorySecurity security;

    // 계좌대체나 새로운 결제에 사용할 수 있는 수량
    @Column(nullable = false)
    private long availableQuantity;

    // 이미 접수된 결제지시를 이행하기 위해 예약되어 다른 결제에 사용할 수 없는 수량
    // 결제를 동시에 수행할때 특정 결제에서 사용되는 수량이 다른 결제에서 사용되지 않도록 격리하는 역할을 수행한다.
    @Column(nullable = false)
    private long lockedQuantity;

    public SecuritiesPosition(
            ParticipantSecuritiesAccount account,
            DepositorySecurity security
    ) {
        this.account = Objects.requireNonNull(account, "participant account must not be null");
        this.security = Objects.requireNonNull(security, "depository security must not be null");
    }

    // 증권계좌의 availableQuantity를 증가시키는 메서드
    public long creditAvailable(long quantity) {
        requireOperational();
        availableQuantity = Math.addExact(availableQuantity, requirePositive(quantity));
        return availableQuantity;
    }

    // availableQuantity를 차감하는 메서드
    public long debitAvailable(long quantity) {
        validateSettlementLock(quantity);
        availableQuantity -= quantity;
        return availableQuantity;
    }

    // lockedQuantity를 증가시키는 메서드
    public void lockForSettlement(long quantity) {
        debitAvailable(quantity);
        lockedQuantity = Math.addExact(lockedQuantity, quantity);
    }

    // 결제가 취소될 경우, 해당 종목 수량만큼 lockedQuantity에서 차감하고, availableQuantity를 증가시킨다.
    public void releaseSettlementLock(long quantity) {
        validateLockedSettlementDelivery(quantity);
        lockedQuantity -= quantity;
        availableQuantity = Math.addExact(availableQuantity, quantity);
    }

    // 결제가 성공하면 해당 종목 수량만큼 lockedQuantity에서 차감한다.
    public void deliverLockedForSettlement(long quantity) {
        validateLockedSettlementDelivery(quantity);
        lockedQuantity -= quantity;
    }

    public void validateSettlementLock(long quantity) {
        requireOperational();
        requirePositive(quantity);
        if (availableQuantity < quantity) {
            throw new IllegalArgumentException("insufficient available securities");
        }
    }

    public void validateLockedSettlementDelivery(long quantity) {
        requireOperational();
        requirePositive(quantity);
        if (lockedQuantity < quantity) {
            throw new IllegalArgumentException("insufficient locked securities");
        }
    }

    public void validateSettlementCredit(long quantity) {
        requireOperational();
        requirePositive(quantity);
        Math.addExact(availableQuantity, quantity);
    }

    public long totalQuantity() {
        return Math.addExact(availableQuantity, lockedQuantity);
    }

    private void requireOperational() {
        if (!account.isActive()) {
            throw new IllegalStateException("participant securities account is not active");
        }
        if (!security.isActive()) {
            throw new IllegalStateException("security is not active");
        }
    }

    private static long requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("securities quantity must be positive");
        }
        return quantity;
    }
}
