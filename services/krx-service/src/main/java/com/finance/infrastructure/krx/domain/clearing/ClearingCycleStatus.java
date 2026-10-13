package com.finance.infrastructure.krx.domain.clearing;

// 특정 거래일의 체결정보를 묶어 상계하고 결제로 넘기는 상계 주기의 처리상태를 나타내는 Enum
public enum ClearingCycleStatus {
    OPEN, // 상계 대상 체결정보를 포함할 수 있는 상태
    CLOSED, // 체결정보의 포함을 마감하고 순포지션 계산을 기다리는 상태
    CALCULATED, // 종목별 증권 순포지션과 기관별 현금 순포지션의 계산이 완료된 상태
    SETTLEMENT_PENDING, // 상계 결과를 KSD와 BOK에 제출하여 실제 결제를 기다리는 상태
    SETTLED, // 상계 주기에 포함된 모든 체결의 증권·현금 결제가 완료된 상태
    FAILED // 상계 또는 결제 처리 중 문제가 발생해 정상적으로 완료되지 못한 상태
}
