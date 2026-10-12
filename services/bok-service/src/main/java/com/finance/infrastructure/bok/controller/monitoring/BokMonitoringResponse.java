package com.finance.infrastructure.bok.controller.monitoring;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// BOK의 참가은행, 참가은행들의 당좌예금 계좌, 결재 배치, 원장기록등을 record에 담아 대시보드에 응답한다.
public record BokMonitoringResponse(
        List<Participant> participants,
        List<SettlementAccount> settlementAccounts,
        List<SettlementBatch> batches,
        List<LedgerEntry> ledgerEntries
) {
    public record Participant(
            String institutionId,
            String name,
            String status,
            OffsetDateTime createdAt
    ) {
    }

    public record SettlementAccount(
            String accountNumber,
            String institutionId,
            BigDecimal balance,
            String status,
            OffsetDateTime createdAt
    ) {
    }

    public record SettlementBatch(
            String batchId,
            String clearingCycleId,
            String status,
            List<Position> positions,
            OffsetDateTime receivedAt,
            OffsetDateTime settledAt,
            String rejectionReason
    ) {
    }

    public record Position(String institutionId, BigDecimal netAmount) {
    }

    public record LedgerEntry(
            Long ledgerEntryId,
            String batchId,
            String clearingCycleId,
            String accountNumber,
            String institutionId,
            String entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            OffsetDateTime createdAt
    ) {
    }
}
