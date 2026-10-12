package com.finance.infrastructure.kftc.controller.monitoring;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// KFTC의 참가 은행 목록, 지급지시 정보, 청산주기 및 청산정보, 결제요청 정보등을 record에 담아 대시보드에 응답한다.
public record KftcMonitoringResponse(
        List<Participant> participants,
        List<PaymentInstruction> paymentInstructions,
        List<ClearingCycle> clearingCycles,
        List<SettlementSubmission> settlementSubmissions
) {
    public record Participant(
            String institutionId,
            String status,
            OffsetDateTime createdAt
    ) {
    }

    public record PaymentInstruction(
            String instructionId,
            String requestId,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount,
            String status,
            String clearingCycleId,
            OffsetDateTime receivedAt,
            OffsetDateTime forwardedAt,
            OffsetDateTime receiverCreditedAt,
            OffsetDateTime settledAt,
            String failureReason
    ) {
    }

    public record ClearingCycle(
            String cycleId,
            String status,
            List<Position> positions,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            OffsetDateTime calculatedAt,
            OffsetDateTime settledAt,
            String failureReason
    ) {
    }

    public record Position(String institutionId, BigDecimal netAmount) {
    }

    public record SettlementSubmission(
            String submissionId,
            String clearingCycleId,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime submittedAt,
            OffsetDateTime settledAt,
            String rejectionReason
    ) {
    }
}
