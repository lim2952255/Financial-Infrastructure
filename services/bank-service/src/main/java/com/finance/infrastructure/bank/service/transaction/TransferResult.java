package com.finance.infrastructure.bank.service.transaction;

import com.finance.infrastructure.bank.domain.account.AccountReference;
import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import com.finance.infrastructure.bank.domain.transaction.BankTransactionType;
import com.finance.infrastructure.bank.domain.transaction.TransactionStatus;
import com.finance.infrastructure.common.money.Money;

import java.time.LocalDateTime;

// 이체 결과 정보를 담는 레코드
public record TransferResult(
        Long transactionId,
        String requestId,
        BankTransactionType type,
        TransactionStatus status,
        AccountReference sender,
        AccountReference receiver,
        Money amount,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static TransferResult from(BankTransaction transaction) {
        return new TransferResult(
                transaction.getId(),
                transaction.getRequestId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.sender(),
                transaction.receiver(),
                transaction.amount(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }
}
