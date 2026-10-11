package com.finance.infrastructure.kftc.service.clearing;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.NetPosition;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;

import java.math.BigDecimal;
import java.util.List;

// KFTC의 청산과 BOK의 결제 처리상태를 관리하는 레코드
public record ClearingResult(
        String cycleId,
        String cycleStatus,
        String submissionId,
        String submissionStatus,
        List<PositionResult> positions
) {
    public static ClearingResult from(
            ClearingCycle cycle,
            SettlementSubmission submission
    ) {
        return new ClearingResult(
                cycle.getCycleId(),
                cycle.getStatus().name(),
                submission.getSubmissionId(),
                submission.getStatus().name(),
                cycle.getPositions().stream()
                        .map(PositionResult::from)
                        .toList()
        );
    }

    // 각 기관별 순포지션 정보를 저장한다.
    public record PositionResult(String institutionId, BigDecimal netAmount) {
        private static PositionResult from(NetPosition position) {
            return new PositionResult(
                    position.institutionId().value(),
                    position.netAmount().amount()
            );
        }
    }
}
