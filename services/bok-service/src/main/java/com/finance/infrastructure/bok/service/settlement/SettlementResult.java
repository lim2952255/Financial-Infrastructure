package com.finance.infrastructure.bok.service.settlement;

import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;

import java.time.LocalDateTime;

// BOK에서 결제 처리 결과를 담은 레코드
public record SettlementResult(
        String batchId,
        String status,
        LocalDateTime settledAt
) {
    public static SettlementResult from(SettlementBatch batch) {
        return new SettlementResult(
                batch.getBatchId(),
                batch.getStatus().name(),
                batch.getSettledAt()
        );
    }
}
