package com.finance.infrastructure.ksd.domain.ledger;

import com.finance.infrastructure.ksd.domain.account.SecuritiesPosition;
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

// KSD 참가기관의 증권계좌의 종목별 수량과 결제 잠금 상태 변화를 기록하는 원장
@Entity
@Table(name = "securities_ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KSD 참가기관의 증권계좌가 종목별로 보유한 총 집계 수량을 저장한 엔티티
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "securities_position_id", nullable = false)
    private SecuritiesPosition securitiesPosition;

    // KSD 증권원장에 기록된 수량변화가 어떤 이유로 발생했는지를 기록하는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SecuritiesLedgerEntryType entryType;

    // 이 수량 변화를 발생시킨 계좌대체 또는 결제지시의 식별자
    @Column(nullable = false, updatable = false, length = 64)
    private String referenceId;

    // 거래수량
    @Column(nullable = false, updatable = false)
    private long quantity;

    // 거래후 availableQuantity
    @Column(nullable = false, updatable = false)
    private long availableAfter;

    // 거래후 lockedQuantity
    @Column(nullable = false, updatable = false)
    private long lockedAfter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SecuritiesLedgerEntry(
            SecuritiesPosition securitiesPosition,
            SecuritiesLedgerEntryType entryType,
            String referenceId,
            long quantity
    ) {
        this.securitiesPosition = Objects.requireNonNull(
                securitiesPosition,
                "securities position must not be null"
        );
        this.entryType = Objects.requireNonNull(entryType, "ledger entry type must not be null");
        this.referenceId = requireText(referenceId, "reference id");
        this.quantity = requirePositive(quantity);
        this.availableAfter = securitiesPosition.getAvailableQuantity();
        this.lockedAfter = securitiesPosition.getLockedQuantity();
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
