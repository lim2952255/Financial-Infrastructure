package com.financialblockchain.simulator.bank.exception;

/** 요청한 계좌 번호가 현재 은행 DB에 없을 때 발생한다. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
