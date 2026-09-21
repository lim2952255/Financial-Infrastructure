package com.financialblockchain.simulator.bank.exception;

/** 동일한 계좌 번호를 중복 생성하려 할 때 발생한다. */
public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(String accountNumber) {
        super("Account already exists: " + accountNumber);
    }
}
