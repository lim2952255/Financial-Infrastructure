package com.finance.infrastructure.broker.domain.trade;

// 체결된 거래가 상계·결제 과정에서 현재 어느 단계에 있는지 나타내는 Enum
public enum SettlementStatus {
    PENDING_CLEARING, // 거래가 체결되었지만, 아직 상계에 포함되지 않은 상태
    CLEARED, // 거래가 상계 과정에 포함되어 증권사별 최종 지급·수령 의무가 계산된 상태
    SETTLEMENT_PENDING, // 상계 결과를 바탕으로 결제 요청이 전달되어 실제 자산 이전을 기다리는 상태
    SETTLED, // 결제가 완료되어 돈과 주식의 이전이 완료된 상태
    FAILED // 상계 또는 결제 처리 중 문제가 발생해 정상적으로 완료되지 못한 상태
}
