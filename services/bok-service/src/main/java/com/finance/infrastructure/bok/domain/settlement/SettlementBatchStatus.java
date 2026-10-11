package com.finance.infrastructure.bok.domain.settlement;

// KFTC가 BOK에 보낸 결제요청 묶음의 처리상태정보를 담은 Enum
public enum SettlementBatchStatus {
    RECEIVED, // KFTC의 결제 요청정보를 BOK가 접수 및 저장한 상태
    VALIDATED, // 결제를 실행할 수 있는지 검증한 상태
    SETTLED, // 결제가 최종 완료된 상태
    REJECTED // 결제가 거절된 상태
}
