package com.financialblockchain.simulator.bank.support;

public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(String accountNumber) {
        super("Account already exists: " + accountNumber);
    }
}
