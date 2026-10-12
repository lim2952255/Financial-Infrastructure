package com.finance.infrastructure.bok.service.settlement;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;

import java.util.List;
import java.util.Objects;

// KFTC가 BOK에 요청한 청산결과 및 결제요청 레코드
public record SettlementCommand(
        String batchId,
        String clearingCycleId,
        List<PositionCommand> positions
) {
    public SettlementCommand {
        Objects.requireNonNull(batchId, "batch id must not be null");
        Objects.requireNonNull(clearingCycleId, "clearing cycle id must not be null");
        positions = List.copyOf(Objects.requireNonNull(positions, "positions must not be null"));
    }

    public record PositionCommand(InstitutionId institutionId, Money netAmount) {
        public PositionCommand {
            Objects.requireNonNull(institutionId, "institution id must not be null");
            Objects.requireNonNull(netAmount, "net amount must not be null");
        }
    }
}
