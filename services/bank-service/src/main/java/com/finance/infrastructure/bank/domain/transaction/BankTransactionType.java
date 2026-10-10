package com.finance.infrastructure.bank.domain.transaction;

public enum BankTransactionType {
    INTERNAL_TRANSFER, // 은행 내 거래
    INTERBANK_OUTGOING, // 은행간 거래
    INTERBANK_INCOMING
}
