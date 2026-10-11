package com.finance.infrastructure.kftc.domain.clearing;

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

// 일정 시간 동안의 지급지시를 묶어 기관별 순포지션을 계산하는 청산 주기
@Entity
@Table(name = "clearing_cycles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClearingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String cycleId;

    // 청산 주기에 계산된 금융기관별 순포지션을 저장한다.
    @ElementCollection(fetch = FetchType.LAZY) // NetPosition은 엔티티가 아니라 값객체이기 때문에 @ElementCollection를 사용한다.
    @CollectionTable( // NetPosition List를 저장할 table을 설정한다.
            name = "clearing_net_positions",
            joinColumns = @JoinColumn(name = "clearing_cycle_id")
    )
    private List<NetPosition> positions = new ArrayList<>();

    // 청산 주기의 상태를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClearingCycleStatus status;

    // 청산주기가 시작된 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime openedAt;

    // 지급지시 접수를 마감한 시간
    private LocalDateTime closedAt;
    // 기관별 순포지션 계산을 완료한 시간
    private LocalDateTime calculatedAt;
    // BOK가 당좌예금 차변/대변 처리를 완료한 시간
    private LocalDateTime settledAt;

    @Column(length = 255)
    private String failureReason;

    public ClearingCycle(String cycleId) {
        this.cycleId = requireText(cycleId, "cycle id");
        this.status = ClearingCycleStatus.OPEN;
        this.openedAt = LocalDateTime.now();
    }

    public List<NetPosition> getPositions() {
        return List.copyOf(positions);
    }

    public boolean isOpen() {
        return status == ClearingCycleStatus.OPEN;
    }

    // 청산주기의 상태를 update하는 메서드
    public void close() {
        requireStatus(ClearingCycleStatus.OPEN);
        status = ClearingCycleStatus.CLOSED;
        closedAt = LocalDateTime.now();
    }

    public void calculate(List<NetPosition> positions) {
        requireStatus(ClearingCycleStatus.CLOSED);
        this.positions.addAll(requireBalancedPositions(positions));
        status = ClearingCycleStatus.CALCULATED;
        calculatedAt = LocalDateTime.now();
    }

    public void settle() {
        requireStatus(ClearingCycleStatus.CALCULATED);
        status = ClearingCycleStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (status == ClearingCycleStatus.SETTLED || status == ClearingCycleStatus.FAILED) {
            throw new IllegalStateException("terminal clearing cycle cannot fail again");
        }
        failureReason = requireText(reason, "failure reason");
        status = ClearingCycleStatus.FAILED;
    }

    private void requireStatus(ClearingCycleStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "clearing cycle must be " + expected + " but was " + status
            );
        }
    }

    private static List<NetPosition> requireBalancedPositions(List<NetPosition> positions) {
        Objects.requireNonNull(positions, "net positions must not be null");
        if (positions.size() < 2) {
            throw new IllegalArgumentException("clearing cycle requires at least two positions");
        }

        Set<InstitutionId> institutionIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (NetPosition position : positions) {
            Objects.requireNonNull(position, "net position must not be null");
            // 청산과정에서는 각 금융기관별로 순포지션을 계산하기 떄문에, 한 금융기관에 대해서는 하나의 청산정보만 존재해야 한다.
            if (!institutionIds.add(position.institutionId())) {
                throw new IllegalArgumentException(
                        "duplicate net position: " + position.institutionId()
                );
            }
            total = total.add(position.netAmount().amount());
        }
        // 각 금융기관의 순포지션을 모두 합치면 0원이 나와야한다.
        if (total.signum() != 0) {
            throw new IllegalArgumentException("net positions must sum to zero");
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
