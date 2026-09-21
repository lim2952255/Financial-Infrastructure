package com.financialblockchain.simulator.bank.domain.transfer;

public enum TransferStatus {
    PENDING,
    DEBIT_POSTED,
    COMPLETED,
    DEBIT_FAILED,
    CREDIT_FAILED
}
