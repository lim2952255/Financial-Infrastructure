package com.finance.infrastructure.kftc.domain.settlement;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// 청산 결과를 BOK에 제출하고 처리 결과를 관리하는 엔티티
@Entity
@Table(name = "settlement_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String submissionId;

    // 하나의 결제정보는 하나의 청산주기의 청산결과에서 나와야 한다.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clearing_cycle_id", nullable = false, unique = true, updatable = false)
    private ClearingCycle clearingCycle;

    // 결제 상태를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementSubmissionStatus status;

    // KFTC가 BOK에 보낼 결제요청을 생성한 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // KFTC가 결제요청을 BOK에 제출한 시간
    private LocalDateTime submittedAt;
    // BOK가 결제를 완료한 시간
    private LocalDateTime settledAt;

    // BOK에서 결제요청을 거절한 이유
    @Column(length = 255)
    private String rejectionReason;

    public SettlementSubmission(String submissionId, ClearingCycle clearingCycle) {
        this.submissionId = requireText(submissionId, "submission id");
        this.clearingCycle = Objects.requireNonNull(
                clearingCycle,
                "clearing cycle must not be null"
        );
        if (clearingCycle.getStatus() != ClearingCycleStatus.CALCULATED) {
            throw new IllegalArgumentException("clearing cycle must be calculated");
        }
        this.status = SettlementSubmissionStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // 결제요청의 상태를 update하는 메서드
    public void submit() {
        requireStatus(SettlementSubmissionStatus.CREATED);
        status = SettlementSubmissionStatus.SUBMITTED;
        submittedAt = LocalDateTime.now();
    }

    public void settle() {
        requireStatus(SettlementSubmissionStatus.SUBMITTED);
        status = SettlementSubmissionStatus.SETTLED;
        settledAt = LocalDateTime.now();
        clearingCycle.settle();
    }

    public void reject(String reason) {
        requireStatus(SettlementSubmissionStatus.SUBMITTED);
        rejectionReason = requireText(reason, "rejection reason");
        status = SettlementSubmissionStatus.REJECTED;
        clearingCycle.fail(reason);
    }

    private void requireStatus(SettlementSubmissionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "settlement submission must be " + expected + " but was " + status
            );
        }
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
