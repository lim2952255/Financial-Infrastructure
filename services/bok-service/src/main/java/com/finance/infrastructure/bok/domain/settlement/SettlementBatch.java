package com.finance.infrastructure.bok.domain.settlement;

import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
* KFTC가 BOK에 보낸 결제 요청 묶음
* 예:
* BANK_A: -40,000원
* BANK_B: +30,000원
* BANK_C: +10,000원
*
* 각각의 결제 요청 정보들은 SettlementPosition 객체로 표현한다.
* */
@Entity
@Table(name = "settlement_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String batchId;

    // KFTC가 여러 지급지시를 모아 청산한 청산 주기의 식별자
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String clearingCycleId;

    // SettlementPosition은 엔티티가 아니라 값 객체이기 때문에 @ElementCollection을 통해 컬렉션을 저장한다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable( // SettlementPosition의 Collection을 저장할 table 설정
            name = "settlement_positions",
            joinColumns = @JoinColumn(name = "settlement_batch_id")
    )
    private List<SettlementPosition> positions = new ArrayList<>();

    // 결제요청묶음의 처리상태를 저장하는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementBatchStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime settledAt;

    // 결제 거절 이유
    @Column(length = 255)
    private String rejectionReason;

    public SettlementBatch(
            String batchId,
            String clearingCycleId,
            List<SettlementPosition> positions
    ) {
        this.batchId = requireIdentifier(batchId, "batch id");
        this.clearingCycleId = requireIdentifier(clearingCycleId, "clearing cycle id");
        this.positions.addAll(requireBalancedPositions(positions));
        this.status = SettlementBatchStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public List<SettlementPosition> getPositions() {
        return List.copyOf(positions);
    }

    // 결제 상태를 update하는 메서드들
    public void validate() {
        requireStatus(SettlementBatchStatus.RECEIVED);
        status = SettlementBatchStatus.VALIDATED;
    }

    public void settle() {
        requireStatus(SettlementBatchStatus.VALIDATED);
        status = SettlementBatchStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    // 결제를 거절하고, 거절이유를 기록하며 상태를 update한다.
    public void reject(String reason) {
        if (status == SettlementBatchStatus.SETTLED || status == SettlementBatchStatus.REJECTED) {
            throw new IllegalStateException("terminal settlement batch cannot be rejected");
        }
        rejectionReason = requireIdentifier(reason, "rejection reason");
        status = SettlementBatchStatus.REJECTED;
    }

    private void requireStatus(SettlementBatchStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "settlement batch must be " + expected + " but was " + status
            );
        }
    }

    private static List<SettlementPosition> requireBalancedPositions(
            List<SettlementPosition> positions
    ) {
        Objects.requireNonNull(positions, "settlement positions must not be null");
        if (positions.size() < 2) {
            throw new IllegalArgumentException("settlement batch requires at least two positions");
        }
        // 결제요청 묶음에 각 기관들의 결제정보는 하나씩만 들어가야 한다(순포지션 정보이기 떄문애)
        Set<InstitutionId> institutionIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementPosition position : positions) {
            Objects.requireNonNull(position, "settlement position must not be null");
            // 만약 결제정보에 한 기관의 결제정보가 여러개가 들어가면 예외를 발생시킨다.
            if (!institutionIds.add(position.institutionId())) {
                throw new IllegalArgumentException(
                        "duplicate settlement position: " + position.institutionId()
                );
            }
            // 총결제액
            total = total.add(position.netAmount().amount());
        }
        // 모든 기관의 총 결제액을 합하면 0원이 나와야 한다(각 기관별 청산 결과이기 떄문에)
        if (total.signum() != 0) {
            throw new IllegalArgumentException("settlement positions must sum to zero");
        }
        return List.copyOf(positions);
    }

    private static String requireIdentifier(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
