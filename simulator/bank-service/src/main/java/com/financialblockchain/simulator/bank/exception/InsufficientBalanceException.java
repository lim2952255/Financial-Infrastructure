package com.financialblockchain.simulator.bank.exception;

import java.math.BigDecimal;

/** 출금 요청 금액이 현재 계좌 잔액보다 클 때 발생한다. */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String accountNumber, BigDecimal balance, BigDecimal requested) {
        super("Insufficient balance for account %s: balance=%s, requested=%s"
                .formatted(accountNumber, balance, requested));
    }
}
