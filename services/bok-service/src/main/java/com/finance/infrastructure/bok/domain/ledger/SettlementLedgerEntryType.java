package com.finance.infrastructure.bok.domain.ledger;

// 당좌예금 거래종류를 분류하는 Enum
public enum SettlementLedgerEntryType {
    DEBIT, // 출금(차변)
    CREDIT // 입금(대변)
}
