package com.finance.infrastructure.bank.domain.transaction;

public enum TransactionStatus {
    PENDING, // 진행중
    COMPLETED, // 종료 (성공)
    FAILED // 종료 (실패)
}
