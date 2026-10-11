package com.finance.infrastructure.kftc.domain.clearing;

// 특정 청산주기가 현재 어느 처리단계에 있는지를 나타낸다.
public enum ClearingCycleStatus {
    OPEN, // 아직 지급지시를 받고 모으고 있는 상태
    CLOSED, // 청산 대상 지급지시 접수를 마감한 상태
    CALCULATED, // 마감된 지급지시 정보들을 바탕으로 금융기관별 순포지션 계산을 완료한 상태
    SETTLED, // BOK에서 기관별 당좌예금계좌의 차변/대변 처리를 완료한 상태
    FAILED // 청산 주기를 정상적으로 완료할 수 없는 상태
}
