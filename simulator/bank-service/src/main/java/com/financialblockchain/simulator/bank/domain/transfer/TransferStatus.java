package com.financialblockchain.simulator.bank.domain.transfer;

/** 은행 간 송금이 로컬 출금과 원격 입금을 거치며 이동하는 상태를 정의한다. */
public enum TransferStatus {
    PENDING,
    DEBIT_POSTED,
    COMPLETED,
    DEBIT_FAILED,
    CREDIT_FAILED
}
