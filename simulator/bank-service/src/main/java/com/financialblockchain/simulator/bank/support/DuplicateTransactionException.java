package com.financialblockchain.simulator.bank.support;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists: " + transactionId);
    }
}
