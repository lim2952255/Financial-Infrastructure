package com.finance.infrastructure.ksd.domain.security;

// 종목이 현재 거래가능한지 상태를 나타내는 Enum
public enum TradingStatus {
    ACTIVE, // 정상적으로 거래할 수 있는 종목
    SUSPENDED, // 일시적으로 거래가 정지된 종목
    DELISTED // 상장폐지된 종목
}
