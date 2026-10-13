package com.finance.infrastructure.krx.domain.settlement;

import com.finance.infrastructure.krx.domain.clearing.ClearingCycle;
import com.finance.infrastructure.krx.domain.clearing.ClearingCycleStatus;
import com.finance.infrastructure.krx.domain.clearing.SecuritiesNetPosition;
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

/**
 * KRX가 KSD에 제출하는 (기관, 증권) 순결제 배치
 *
 * 상계 주기에서 계산한 (기관, 증권)별 순포지션 전체를 하나의 결제 단위로 전달한다.
 */
@Entity
@Table(name = "krx_securities_settlement_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesSettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 발급하고 KSD의 결제 배치 식별자로도 사용하는 값
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String batchId;

    // 기관·종목별 증권 순포지션이 확정된 상계 주기
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

    // KSD가 모든 음수 순포지션의 증권의 매도 예약(lock)을 획득했다고 응답한 시간
    private LocalDateTime securitiesReservedAt;

    // BOK 현금결제 완료 후 KRX가 KSD의 최종 증권이전을 승인하면서 발급한 식별자
    @Column(length = 64)
    private String transferAuthorizationId;

    // 실패이유
    @Column(length = 255)
    private String failureReason;

    // 증권 결제배치를 생성한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // KRX가 해당 결제 배치를 KSD에 제출한 시각
    private LocalDateTime submittedAt;
    // BOK가 현금결제를 완료한 후, KRX가 KSD에 최종 증권이전을 승인한 시각
    private LocalDateTime transferAuthorizedAt;
    // KSD로부터 증권결제가 완료됐다는 결과를 받은 시각
    private LocalDateTime settledAt;
    // 결제배치가 정상적으로 수행되지 않아 실패한 시각
    private LocalDateTime failedAt;

    public SecuritiesSettlementBatch(String batchId, ClearingCycle clearingCycle) {
        this.batchId = requireText(batchId, "batch id");
        this.clearingCycle = requireCalculatedCycle(clearingCycle);
        if (clearingCycle.getSecuritiesPositions().isEmpty()) {
            throw new IllegalArgumentException("securities settlement requires at least one net position");
        }
        this.settlementDate = clearingCycle.getSettlementDate();
        this.status = SettlementBatchStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // (기관, 증권) 별 순포지션정보를 조회한다.
    public List<SecuritiesNetPosition> positions() {
        return clearingCycle.getSecuritiesPositions();
    }

    public void submit() {
        requireStatus(SettlementBatchStatus.CREATED);
        status = SettlementBatchStatus.SUBMITTED;
        submittedAt = LocalDateTime.now();
    }

    public void confirmSecuritiesReserved() {
        requireStatus(SettlementBatchStatus.SUBMITTED);
        if (securitiesReservedAt != null) {
            throw new IllegalStateException("securities are already reserved");
        }
        securitiesReservedAt = LocalDateTime.now();
    }

    public void authorizeTransfer(String transferAuthorizationId) {
        requireStatus(SettlementBatchStatus.SUBMITTED);
        if (securitiesReservedAt == null) {
            throw new IllegalStateException("securities must be reserved first");
        }
        if (this.transferAuthorizationId != null) {
            throw new IllegalStateException("securities transfer is already authorized");
        }
        this.transferAuthorizationId = requireText(transferAuthorizationId, "transfer authorization id");
        transferAuthorizedAt = LocalDateTime.now();
    }

    public void settle() {
        requireStatus(SettlementBatchStatus.SUBMITTED);
        if (transferAuthorizationId == null) {
            throw new IllegalStateException("securities transfer must be authorized first");
        }
        status = SettlementBatchStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (transferAuthorizationId != null) {
            throw new IllegalStateException("authorized securities transfer must be retried");
        }
        requireNonTerminal();
        failureReason = requireText(reason, "failure reason");
        status = SettlementBatchStatus.FAILED;
        failedAt = LocalDateTime.now();
    }

    private void requireNonTerminal() {
        if (status == SettlementBatchStatus.SETTLED || status == SettlementBatchStatus.FAILED) {
            throw new IllegalStateException("terminal securities settlement batch cannot fail");
        }
    }

    private void requireStatus(SettlementBatchStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "securities settlement batch must be " + expected + " but was " + status
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
