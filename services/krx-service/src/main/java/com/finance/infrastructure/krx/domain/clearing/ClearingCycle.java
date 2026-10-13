package com.finance.infrastructure.krx.domain.clearing;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.krx.domain.trade.Execution;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

// 동일한 거래일과 결제일을 가진 체결정보를 모아 증권사별 증권·현금 의무를 상계하는 엔티티
@Entity
@Table(name = "krx_clearing_cycles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClearingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 상계 주기마다 발급하는 고유 식별자
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String cycleId;

    // 상계 대상 체결정보가 발생한 거래일
    @Column(nullable = false, updatable = false)
    private LocalDate tradeDate;

    // 상계 결과에 따라 증권과 현금을 이전할 결제 예정일(체결일 + 2일)
    @Column(nullable = false, updatable = false)
    private LocalDate settlementDate;

    // 동일한 거래일과 결제일을 기준으로 이 상계 주기에 포함된 체결정보 목록
    // 해당 체결정보 목록들을 기반으로 상계하여 기관별 현금 순포지션 및 기관 + 종목별 종목 순포지션을 계산한다.
    @OneToMany(mappedBy = "clearingCycle", fetch = FetchType.LAZY)
    private List<Execution> executions = new ArrayList<>();

    // 종목별로 인도 수량과 수령 수량의 합이 0이 되도록 계산된 증권 순포지션
    // 즉 (기관, 종목) 별 순포지션을 계산한 SecuritiesNetPosition을 list로 관리한다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "krx_securities_net_positions", joinColumns = @JoinColumn(name = "clearing_cycle_id"))
    @OrderColumn(name = "position_order")
    private List<SecuritiesNetPosition> securitiesPositions = new ArrayList<>();

    // 모든 참가기관의 지급액과 수령액의 합이 0원이 되도록 계산된 현금 순포지션
    // 기관별 현금 순포지션을 계산한 CashNetPosition을 list로 관리한다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "krx_cash_net_positions", joinColumns = @JoinColumn(name = "clearing_cycle_id"))
    @OrderColumn(name = "position_order")
    private List<CashNetPosition> cashPositions = new ArrayList<>();

    // 상계 주기의 현재 처리상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ClearingCycleStatus status;

    // 상계 주기가 생성되어 체결정보를 포함하기 시작한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime openedAt;
    // 상계 대상 체결정보의 포함을 마감한 시각
    private LocalDateTime closedAt;
    // 증권·현금 순포지션의 계산을 완료한 시각 (상계 완료 시각)
    private LocalDateTime calculatedAt;
    // 상계 결과를 외부 결제기관(BOK, KSD)에 제출한 시각
    private LocalDateTime settlementSubmittedAt;
    // BOK 현금결제가 완료된 시각
    private LocalDateTime cashSettledAt;
    // KSD 증권결제가 완료된 시각
    private LocalDateTime securitiesSettledAt;
    // 상계 주기에 포함된 모든 체결의 결제를 완료한 시각 (결제 완료 시각)
    private LocalDateTime settledAt;

    // 상계 또는 결제 처리 중 실패한 이유
    @Column(length = 255)
    private String failureReason;

    public ClearingCycle(String cycleId, LocalDate tradeDate, LocalDate settlementDate) {
        this.cycleId = requireText(cycleId, "cycle id");
        this.tradeDate = Objects.requireNonNull(tradeDate, "trade date must not be null");
        this.settlementDate = requireSettlementDate(tradeDate, settlementDate);
        this.status = ClearingCycleStatus.OPEN;
        this.openedAt = LocalDateTime.now();
    }

    // 체결거래 정보들을 조회하는 메서드
    public List<Execution> getExecutions() {
        return List.copyOf(executions);
    }

    // (기관, 종목) 순포지션을 조회하는 메서드
    public List<SecuritiesNetPosition> getSecuritiesPositions() {
        return List.copyOf(securitiesPositions);
    }

    // 기관별 현금 순포지션을 조회하는 메서드
    public List<CashNetPosition> getCashPositions() {
        return List.copyOf(cashPositions);
    }

    // 거래일과 결제일이 같은 미상계 체결정보를 상계 주기에 포함한다.
    public void include(Execution execution) {
        requireStatus(ClearingCycleStatus.OPEN); // 현재 상계주기가 OPEN되어 있어야 미상계 체결정보를 포함시킬 수 있다.
        Objects.requireNonNull(execution, "execution must not be null");
        if (execution.getClearingCycle() != null) { // 이미 체결정보가 다른 상계주기에 포함되어 있으면 예외를 발생시킨다.
            throw new IllegalArgumentException("execution is already assigned to a clearing cycle");
        }
        if (!tradeDate.equals(execution.getTradeDate()) || !settlementDate.equals(execution.getSettlementDate())) {
            throw new IllegalArgumentException("execution dates do not match clearing cycle dates");
        }
        // 이미 상계주기에 포함되어 있는지를 검사
        boolean alreadyIncluded = executions.stream()
                .anyMatch(included -> included.getExecutionId().equals(execution.getExecutionId()));
        if (alreadyIncluded) {
            throw new IllegalArgumentException("execution is already included");
        }
        execution.assignTo(this);
        executions.add(execution);
    }

    // 더 이상 체결정보를 포함할 수 없도록 상계 주기를 마감한다.
    public void close() {
        requireStatus(ClearingCycleStatus.OPEN);
        status = ClearingCycleStatus.CLOSED;
        closedAt = LocalDateTime.now();
    }

    // 상계 결과인 증권·현금 순포지션의 균형과 중복 여부를 검증하고 체결정보의 상계를 완료한다.
    public void calculate(
            List<SecuritiesNetPosition> securitiesPositions, // (기관, 종목) 순포지션 정보
            List<CashNetPosition> cashPositions // 기관별 현금 순포지션 정보
    ) {
        requireStatus(ClearingCycleStatus.CLOSED); // 상계를 완료하려면 상계주기가 CLOSED 상태여야 한다.
        if (executions.isEmpty()) {
            throw new IllegalStateException("clearing cycle must contain at least one execution");
        }
        // (기관, 종목) 순포지션 정보를 검사한다.
        List<SecuritiesNetPosition> validatedSecuritiesPositions =
                requireBalancedSecuritiesPositions(securitiesPositions);
        // 기관별 현금 순포지션 정보를 검사한다.
        List<CashNetPosition> validatedCashPositions = requireBalancedCashPositions(cashPositions);
        this.securitiesPositions.addAll(validatedSecuritiesPositions);
        this.cashPositions.addAll(validatedCashPositions);
        status = ClearingCycleStatus.CALCULATED;
        calculatedAt = LocalDateTime.now();
    }

    // 계산된 상계 결과를 KSD와 BOK에 제출하여 실제 결제를 기다리는 상태로 변경한다.
    public void submitForSettlement() {
        requireStatus(ClearingCycleStatus.CALCULATED);
        status = ClearingCycleStatus.SETTLEMENT_PENDING;
        settlementSubmittedAt = LocalDateTime.now();
    }

    // BOK가 상계 주기의 현금 순포지션 결제를 완료한 결과를 반영한다.
    public void completeCashSettlement() {
        requireStatus(ClearingCycleStatus.SETTLEMENT_PENDING);
        if (cashSettledAt != null) {
            throw new IllegalStateException("cash settlement is already completed");
        }
        cashSettledAt = LocalDateTime.now();
    }

    // 현금결제 완료 후 KSD의 모든 증권 계좌대체가 끝나면 상계 주기 전체를 완료한다.
    public void completeSecuritiesSettlement() {
        requireStatus(ClearingCycleStatus.SETTLEMENT_PENDING);
        if (cashSettledAt == null) {
            throw new IllegalStateException("cash settlement must be completed first");
        }
        if (securitiesSettledAt != null) {
            throw new IllegalStateException("securities settlement is already completed");
        }
        securitiesSettledAt = LocalDateTime.now();
        status = ClearingCycleStatus.SETTLED;
        settledAt = securitiesSettledAt;
    }

    // 상계 주기와 아직 완료되지 않은 체결정보에 실패 이유를 기록한다.
    public void fail(String reason) {
        if (cashSettledAt != null) {
            throw new IllegalStateException(
                    "cash-settled clearing cycle must retry securities settlement"
            );
        }
        if (status == ClearingCycleStatus.SETTLED || status == ClearingCycleStatus.FAILED) {
            throw new IllegalStateException("terminal clearing cycle cannot fail");
        }
        failureReason = requireText(reason, "failure reason");
        status = ClearingCycleStatus.FAILED;
    }

     // (기관, 종목) 순포지션 정보를 검증한다.
    private static List<SecuritiesNetPosition> requireBalancedSecuritiesPositions(
            List<SecuritiesNetPosition> positions
    ) {
        Objects.requireNonNull(positions, "securities positions must not be null");
        Set<String> participantSecurities = new HashSet<>();
        // (기관, 종목) 순포지션 정보들을 조회하면서, 각 증권별 순포지션의 합계가 0이 나오는지를 검증한다.
        Map<String, Long> totals = new HashMap<>(); // 종목별
        for (SecuritiesNetPosition position : positions) {
            Objects.requireNonNull(position, "securities position must not be null");
            // key를 참가기관 + 종목정보로 구성하면서 (기관, 종목) 순포지션 정보가 중복해서 저장되어 있는지를 검사한다. 순포지션정보이기 떄문에 중복해서 저장되어 있으면 안된다.
            String key = position.participantId().value() + "\u0000" + position.securityCode();
            if (!participantSecurities.add(key)) {
                throw new IllegalArgumentException("duplicate participant and security position");
            }
            // 각 종목별로 순포지션 정보를 더한다.
            totals.merge(position.securityCode(), position.netQuantity(), Math::addExact);
        }
        // 각 종목별로 총 손포지션정보의 합계는 0이어야 한다.
        if (totals.values().stream().anyMatch(quantity -> quantity != 0)) {
            throw new IllegalArgumentException("securities positions must sum to zero for each security");
        }
        return List.copyOf(positions);
    }

    // 기관별 현금 순포지션 정보를 검증한다.
    private static List<CashNetPosition> requireBalancedCashPositions(List<CashNetPosition> positions) {
        Objects.requireNonNull(positions, "cash positions must not be null");
        Set<InstitutionId> participants = new HashSet<>(); // 참가기관 목록
        BigDecimal total = BigDecimal.ZERO; // 기관별 현금 순포지션 정보의 총합은 0이어야 한다.
        for (CashNetPosition position : positions) {
            Objects.requireNonNull(position, "cash position must not be null");
            // 참가기관별 현금 순포지션 정보는 한건만 존재해야 한다.
            if (!participants.add(position.participantId())) {
                throw new IllegalArgumentException("duplicate cash position: " + position.participantId());
            }
            total = total.add(position.netAmount().amount());
        }
        // 기관별 현금 순포지션 정보의 총합이 0인지를 검사한다.
        if (total.signum() != 0) {
            throw new IllegalArgumentException("cash positions must sum to zero");
        }
        return List.copyOf(positions);
    }

    private void requireStatus(ClearingCycleStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("clearing cycle must be " + expected + " but was " + status);
        }
    }

    private static LocalDate requireSettlementDate(LocalDate tradeDate, LocalDate settlementDate) {
        Objects.requireNonNull(settlementDate, "settlement date must not be null");
        if (settlementDate.isBefore(tradeDate)) {
            throw new IllegalArgumentException("settlement date must not be before trade date");
        }
        return settlementDate;
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
