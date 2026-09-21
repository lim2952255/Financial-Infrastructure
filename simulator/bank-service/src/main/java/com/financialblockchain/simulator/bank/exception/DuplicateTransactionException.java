package com.financialblockchain.simulator.bank.exception;

/** 이미 처리한 거래 ID가 다시 전달되어 중복 원장이 생길 수 있을 때 발생한다. */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists: " + transactionId);
    }
}
