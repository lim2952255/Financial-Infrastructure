package com.finance.infrastructure.krx.domain.order;

// KRX가 접수한 주문의 처리 및 체결 상태를 나타내는 Enum
public enum ExchangeOrderStatus {
    OPEN, // 주문이 접수되었고 아직 체결되지 않은 상태
    PARTIALLY_FILLED, // 주문수량 중 일부만 체결된 상태
    FILLED, // 주문수량이 모두 체결된 상태
    CANCELLED, // 미체결 잔여수량이 취소된 상태
    REJECTED // KRX가 주문접수를 거부한 상태
}
