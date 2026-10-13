package com.finance.infrastructure.krx.domain.participant;

// KRX 거래 참가기관의 주문 제출 가능 상태를 나타내는 Enum
public enum ParticipantStatus {
    ACTIVE, // 정상적으로 주문을 제출할 수 있는 상태
    SUSPENDED // KRX가 주문 제출 권한을 일시적으로 정지한 상태
}
