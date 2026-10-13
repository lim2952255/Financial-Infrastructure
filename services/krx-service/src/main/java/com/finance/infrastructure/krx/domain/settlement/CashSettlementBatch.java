package com.finance.infrastructure.krx.domain.settlement;

import com.finance.infrastructure.krx.domain.clearing.CashNetPosition;
import com.finance.infrastructure.krx.domain.clearing.ClearingCycle;
import com.finance.infrastructure.krx.domain.clearing.ClearingCycleStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// KRX가 BOK에 제출하는 회원별 현금 순포지션 결제 배치
@Entity
@Table(name = "krx_cash_settlement_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashSettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 발급하고 BOK의 결제 배치 식별자로도 사용하는 값
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String batchId;

    // 기관별 현금 순포지션이 확정된 상계 주기
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clearing_cycle_id", nullable = false, updatable = false, unique = true)
    private ClearingCycle clearingCycle;

    // 결제 예정일 (체결일자 + 2일)
    @Column(nullable = false, updatable = false)
    private LocalDate settlementDate;

    // 결제상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementBatchStatus status;

    // 실패원인
    @Column(length = 255)
    private String failureReason;

    // 현금 결제배치를 생성한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // KRX가 해당 결제 배치를 BOK에 제출한 시각
    private LocalDateTime submittedAt;
    // BOK에서 현금 결제를 완료한 시각
    private LocalDateTime settledAt;
    // 결제가 정상적으로 수행되지 않아 실패한 시각
    private LocalDateTime failedAt;

    public CashSettlementBatch(String batchId, ClearingCycle clearingCycle) {
        this.batchId = requireText(batchId, "batch id");
        this.clearingCycle = requireCalculatedCycle(clearingCycle);
        if (clearingCycle.getCashPositions().isEmpty()) {
            throw new IllegalArgumentException("cash settlement requires at least one net position");
        }
        this.settlementDate = clearingCycle.getSettlementDate();
        this.status = SettlementBatchStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // 기관별 현금 순포지션 정보들을 조회한다.
    public List<CashNetPosition> positions() {
        return clearingCycle.getCashPositions();
    }

    public void submit() {
        requireStatus(SettlementBatchStatus.CREATED);
        status = SettlementBatchStatus.SUBMITTED;
        submittedAt = LocalDateTime.now();
    }

    public void settle() {
        requireStatus(SettlementBatchStatus.SUBMITTED);
        status = SettlementBatchStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (status == SettlementBatchStatus.SETTLED || status == SettlementBatchStatus.FAILED) {
            throw new IllegalStateException("terminal cash settlement batch cannot fail");
        }
        failureReason = requireText(reason, "failure reason");
        status = SettlementBatchStatus.FAILED;
        failedAt = LocalDateTime.now();
    }

    private void requireStatus(SettlementBatchStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "cash settlement batch must be " + expected + " but was " + status
            );
        }
    }
    // 상계주기가 상계를 모두 완료한 상태여야 결제요청을 보낼 수 있다.
    private static ClearingCycle requireCalculatedCycle(ClearingCycle clearingCycle) {
        Objects.requireNonNull(clearingCycle, "clearing cycle must not be null");
        if (clearingCycle.getStatus() != ClearingCycleStatus.CALCULATED
                && clearingCycle.getStatus() != ClearingCycleStatus.SETTLEMENT_PENDING) {
            throw new IllegalArgumentException("clearing cycle must be calculated");
        }
        return clearingCycle;
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
