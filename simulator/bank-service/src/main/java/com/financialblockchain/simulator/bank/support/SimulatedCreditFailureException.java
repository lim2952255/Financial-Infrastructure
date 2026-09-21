package com.financialblockchain.simulator.bank.support;

public class SimulatedCreditFailureException extends RuntimeException {

    public SimulatedCreditFailureException(String transactionId) {
        super("Simulated destination credit failure for transaction: " + transactionId);
    }
}
