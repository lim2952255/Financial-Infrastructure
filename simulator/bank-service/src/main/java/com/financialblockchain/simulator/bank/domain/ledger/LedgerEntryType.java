package com.financialblockchain.simulator.bank.domain.ledger;

/** 계좌 잔액을 감소시키는 출금과 증가시키는 입금을 구분한다. */
public enum LedgerEntryType {
    DEBIT,
    CREDIT
}
