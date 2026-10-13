package com.finance.infrastructure.ksd.domain.settlement;

// KSD가 KRX로부터 받은 증권결제 요청이 어느단계까지 처리되었는지를 나타내는 Enum
public enum SecuritiesSettlementStatus {
    RECEIVED, // KRX가 확정한 증권 결제지시를 KSD가 접수한 상태
    SECURITIES_RESERVED, // 주식을 지급해야 하는 증권사 계좌에서 해당 결제 수량을 예약한 상태
    TRANSFER_AUTHORIZED, // KRX로부터 예약한 주식을 최종 이전해도 된다는 승인을 받은 상태
    SETTLED, // 예약한 주식을 지급 증권사 계좌에서 차감하고 수령 증권사 계좌에 반영한 상태
    FAILED // 최종 이전 승인 전에 증권 예약 또는 결제 준비에 실패한 상태
}
