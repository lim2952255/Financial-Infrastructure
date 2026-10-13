package com.finance.infrastructure.ksd.domain.settlement;

import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// KRX가 확정한 (기관, 종목)별 증권 순포지션 전체를 KSD가 처리하는 결제 배치
@Entity
@Table(name = "securities_settlement_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesSettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String batchId;

    // KRX 상계 주기의 식별자
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String clearingReferenceId;

    @OneToMany(
            mappedBy = "settlementBatch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderColumn(name = "position_order")
    private List<SecuritiesSettlementPosition> positions = new ArrayList<>();

    // 결제 예정일 (체결일 +2일)
    @Column(nullable = false, updatable = false)
    private LocalDate settlementDate;

    // 증권결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SecuritiesSettlementStatus status;

    // KRX가 현금결제 완료 후 최종 증권이전을 승인하면서 발급한 식별자
    @Column(length = 64)
    private String transferAuthorizationId;

    // KSD가 KRX의 결제배치를 접수한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    // 지급해야 할 증권을 모두 예약한 시각
    private LocalDateTime securitiesReservedAt;
    // KRX로부터 최종 증권이전 승인을 받은 시각
    private LocalDateTime transferAuthorizedAt;
    // 증권 계좌대체를 완료한 시각
    private LocalDateTime settledAt;
    // 결재배치 처리가 정상적으로 수행되지 않아 실패한 시각
    private LocalDateTime failedAt;

    // 실패원인
    @Column(length = 255)
    private String failureReason;

    public SecuritiesSettlementBatch(
            String batchId,
            String clearingReferenceId,
            LocalDate settlementDate,
            List<SecuritiesSettlementPosition> positions
    ) {
        this.batchId = requireText(batchId, "batch id");
        this.clearingReferenceId = requireText(clearingReferenceId, "clearing reference id");
        this.settlementDate = Objects.requireNonNull(settlementDate, "settlement date must not be null");
        for (SecuritiesSettlementPosition position : requireBalancedPositions(positions)) {
            position.assignTo(this);
            this.positions.add(position);
        }
        this.status = SecuritiesSettlementStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public List<SecuritiesSettlementPosition> getPositions() {
        return List.copyOf(positions);
    }

    // 모든 인도 의무를 먼저 검증한 뒤 음수 순포지션에 해당하는 증권을 예약한다.
    public void reserveSecurities() {
        requireStatus(SecuritiesSettlementStatus.RECEIVED);
        positions.forEach(SecuritiesSettlementPosition::validateReservation);
        positions.forEach(SecuritiesSettlementPosition::reserveSecurities);
        status = SecuritiesSettlementStatus.SECURITIES_RESERVED;
        securitiesReservedAt = LocalDateTime.now();
    }

    public void authorizeTransfer(String transferAuthorizationId) {
        requireStatus(SecuritiesSettlementStatus.SECURITIES_RESERVED);
        this.transferAuthorizationId = requireText(transferAuthorizationId, "transfer authorization id");
        status = SecuritiesSettlementStatus.TRANSFER_AUTHORIZED;
        transferAuthorizedAt = LocalDateTime.now();
    }

    // 예약된 인도 수량을 차감하고 모든 수령 수량을 반영하여 배치 전체를 결제한다.
    public void settle() {
        requireStatus(SecuritiesSettlementStatus.TRANSFER_AUTHORIZED);
        positions.forEach(SecuritiesSettlementPosition::validateSettlement);
        positions.forEach(SecuritiesSettlementPosition::settle);
        status = SecuritiesSettlementStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    // 최종 이전 승인 전 실패한 배치는 예약한 증권을 다시 사용 가능 수량으로 되돌린다.
    public void fail(String reason) {
        if (status == SecuritiesSettlementStatus.TRANSFER_AUTHORIZED) {
            throw new IllegalStateException("authorized settlement batch must retry securities settlement");
        }
        if (status == SecuritiesSettlementStatus.SETTLED || status == SecuritiesSettlementStatus.FAILED) {
            throw new IllegalStateException("terminal settlement batch cannot fail");
        }
        if (status == SecuritiesSettlementStatus.SECURITIES_RESERVED) {
            positions.forEach(SecuritiesSettlementPosition::releaseReservation);
        }
        failureReason = requireText(reason, "failure reason");
        status = SecuritiesSettlementStatus.FAILED;
        failedAt = LocalDateTime.now();
    }

    private void requireStatus(SecuritiesSettlementStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "settlement batch must be " + expected + " but was " + status
            );
        }
    }

    private static List<SecuritiesSettlementPosition> requireBalancedPositions(
            List<SecuritiesSettlementPosition> positions
    ) {
        Objects.requireNonNull(positions, "settlement positions must not be null");
        if (positions.size() < 2) {
            throw new IllegalArgumentException("settlement batch requires at least two positions");
        }

        Set<String> participantSecurities = new HashSet<>();
        Map<String, Long> totalsBySecurity = new HashMap<>();
        for (SecuritiesSettlementPosition position : positions) {
            Objects.requireNonNull(position, "settlement position must not be null");
            InstitutionId participantId = position.getSecuritiesPosition()
                    .getAccount()
                    .getParticipant()
                    .getInstitutionId();
            String securityCode = position.getSecuritiesPosition().getSecurity().getSecurityCode();
            String key = participantId.value() + "\u0000" + securityCode;
            if (!participantSecurities.add(key)) {
                throw new IllegalArgumentException("duplicate participant and security position");
            }
            totalsBySecurity.merge(securityCode, position.getNetQuantity(), Math::addExact);
        }
        if (totalsBySecurity.values().stream().anyMatch(quantity -> quantity != 0)) {
            throw new IllegalArgumentException("settlement positions must sum to zero for each security");
        }
        return List.copyOf(positions);
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
