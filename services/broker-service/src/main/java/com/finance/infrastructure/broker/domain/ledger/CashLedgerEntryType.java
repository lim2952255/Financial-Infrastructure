package com.finance.infrastructure.broker.domain.ledger;

// 예수금 원장에 기록된 기록이 어떤 거래로 인해 발생한 기록인지를 나타내는 Enum
public enum CashLedgerEntryType {
    DEPOSIT, // 예수금 입금
    WITHDRAWAL, // 예수금 출금
    BUY_ORDER_LOCK, // 미체결 매수주문을 위한 금액 예약
    BUY_ORDER_RELEASE, // 주문 취소 또는 거부로 인한 예약금액 해제
    BUY_EXECUTION, // 매수주문 예약금액을 체결 결제예정 지급액으로 전환
    SELL_EXECUTION, // 매도 체결대금을 결제예정 수령액으로 반영
    SETTLEMENT_DEBIT, // 매수거래 결제대금 지급
    SETTLEMENT_CREDIT // 매도거래 결제대금 수령
}
