package com.finance.infrastructure.krx.domain.settlement;

// KRX가 외부 결제기관에 제출하는 결제 배치의 공통 처리상태
public enum SettlementBatchStatus {
    CREATED, // 상계 결과를 바탕으로 결제 배치를 생성한 상태
    SUBMITTED, // KSD 또는 BOK에 결제 배치를 제출한 상태
    SETTLED, // 외부 결제기관이 배치의 자산이전을 완료한 상태
    FAILED // 제출 또는 결제 준비 중 실패한 상태
}
