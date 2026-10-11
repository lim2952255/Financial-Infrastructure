package com.finance.infrastructure.bank.service.transfer.interbank;

import java.util.List;
import java.util.Objects;

// KFTC가 은행에 전달하는 청산주기 단위의 최종 결제 통보다.
public record InterbankSettlementCommand(
        String clearingCycleId, // 결제를 처리한 KFTC 청사눚기
        String settlementBatchId, // 결제를 처리한 BOK 결제배치
        List<String> instructionIds // 해당 결제배치에 포함된 은행의 지급지시목록
) {

    public InterbankSettlementCommand {
        clearingCycleId = requireText(clearingCycleId, "clearing cycle id");
        settlementBatchId = requireText(settlementBatchId, "settlement batch id");
        Objects.requireNonNull(instructionIds, "instruction ids must not be null");
        instructionIds = instructionIds.stream()
                .map(instructionId -> requireText(instructionId, "instruction id"))
                .distinct() // 중복 제거
                .toList();
        if (instructionIds.isEmpty()) {
            throw new IllegalArgumentException("instruction ids must not be empty");
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
