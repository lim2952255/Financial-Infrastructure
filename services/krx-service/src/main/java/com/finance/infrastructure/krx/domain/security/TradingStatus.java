package com.finance.infrastructure.krx.domain.security;

// KRX 상장종목의 현재 거래 가능 상태를 나타내는 Enum
public enum TradingStatus {
    ACTIVE, // 정상적으로 주문을 접수할 수 있는 상태
    SUSPENDED, // 주문접수와 거래가 일시적으로 정지된 상태
    DELISTED // 상장이 폐지되어 더 이상 거래할 수 없는 상태
}
