package com.financialblockchain.simulator.bank.exception;

/** 출발 은행 출금 후 상대 은행 입금 실패를 재현하기 위해 의도적으로 발생시키는 예외다. */
public class SimulatedCreditFailureException extends RuntimeException {

    public SimulatedCreditFailureException(String transactionId) {
        super("Simulated destination credit failure for transaction: " + transactionId);
    }
}
