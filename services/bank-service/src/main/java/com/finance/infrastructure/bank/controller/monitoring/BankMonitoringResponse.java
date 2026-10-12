package com.finance.infrastructure.bank.controller.monitoring;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// 은행 내부의 계좌정보, Transaction정보, 원장정보들을 record로 담아 대시보드에 전달한다.
public record BankMonitoringResponse(
        String institutionId,
        List<Account> accounts,
        List<Transaction> transactions,
        List<LedgerEntry> ledgerEntries
) {
    public record Account(
            Long customerId,
            String customerName,
            String accountNumber,
            BigDecimal balance
    ) {
    }

    public record Transaction(
            Long transactionId,
            String requestId,
            String paymentInstructionId,
            String type,
            String status,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount,
            String clearingCycleId,
            String settlementBatchId,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt,
            OffsetDateTime settlementCompletedAt,
            String failureReason
    ) {
    }

    public record LedgerEntry(
            Long ledgerEntryId,
            Long transactionId,
            String requestId,
            String paymentInstructionId,
            String accountNumber,
            String changeType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            OffsetDateTime createdAt
    ) {
    }
}
