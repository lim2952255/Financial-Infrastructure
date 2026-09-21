package com.financialblockchain.simulator.bank.support;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String accountNumber, BigDecimal balance, BigDecimal requested) {
        super("Insufficient balance for account %s: balance=%s, requested=%s"
                .formatted(accountNumber, balance, requested));
    }
}
