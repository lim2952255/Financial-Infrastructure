package com.finance.infrastructure.broker.domain.order;

// 고객이 제출한 종목이 현재 얼마나 체결되었는지를 나타낸다.
public enum OrderStatus {
    OPEN, // 증권사가 주문을 접수했지만 아직 체결되지 않은 상태
    PARTIALLY_FILLED, // 주문수량중, 일부만 체결된 상태
    FILLED, // 주문수량 전체가 체결된 상태
    CANCELLED, // 미체결수량이 취소된 상태
    REJECTED // 증권사나 KRX에 주문을 거절한 상태
}
