package com.finance.infrastructure.bank.controller.transaction.dto;

import com.finance.infrastructure.bank.domain.account.AccountReference;
import com.finance.infrastructure.bank.domain.transaction.BankTransactionType;
import com.finance.infrastructure.bank.domain.transaction.TransactionStatus;
import com.finance.infrastructure.bank.service.transaction.TransferResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 이체 결과정보
public record TransferResponse(
        Long transactionId,
        String requestId,
        BankTransactionType type,
        TransactionStatus status,
        AccountResponse sender,
        AccountResponse receiver,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static TransferResponse from(TransferResult result) {
        return new TransferResponse(
                result.transactionId(),
                result.requestId(),
                result.type(),
                result.status(),
                AccountResponse.from(result.sender()),
                AccountResponse.from(result.receiver()),
                result.amount().amount(),
                result.createdAt(),
                result.completedAt()
        );
    }

    public record AccountResponse(String institutionId, String accountNumber) {

        private static AccountResponse from(AccountReference reference) {
            return new AccountResponse(
                    reference.institutionId().value(),
                    reference.accountNumber()
            );
        }
    }
}
