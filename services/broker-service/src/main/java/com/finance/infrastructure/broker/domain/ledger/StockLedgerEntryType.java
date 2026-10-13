package com.finance.infrastructure.broker.domain.ledger;

// 종목 원장에 기록된 기록이 어떤 거래로 인해 발생한 기록인지를 나타내는 Enum
public enum StockLedgerEntryType {
    DEPOSIT, // 다른 증권사로부터 주식 입고
    WITHDRAWAL, // 다른 증권사등으로 주식 출고
    SELL_ORDER_LOCK, // 미체결 매도주문을 위한 수량 예약
    SELL_ORDER_RELEASE, // 주문 취소 또는 거부로 인한 예약수량 해제
    BUY_EXECUTION, // 매수 체결수량을 결제예정 수령수량으로 반영
    SELL_EXECUTION, // 매도주문 예약수량을 결제예정 인도수량으로 전환
    SETTLEMENT_DELIVERY, // 매도한 주식을 결제로 인도
    SETTLEMENT_RECEIPT // 매수한 주식을 결제로 수령
}
