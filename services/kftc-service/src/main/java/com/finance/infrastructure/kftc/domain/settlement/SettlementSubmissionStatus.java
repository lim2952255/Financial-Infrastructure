package com.finance.infrastructure.kftc.domain.settlement;

// KFTC가 청산 결과를 BOK에 결제 요청으로 제출하는 과정의 상태를 나타내는 Enum
public enum SettlementSubmissionStatus {
    CREATED, // 처안 주기에서 기관별 순포지션을 계산하고, BOK에 보낼 결제요청을 생성하고, 아직 요청을 보내지 않은 상태
    SUBMITTED, // KFTC가 결제요청을 BOK에 전송한 상태
    SETTLED, // BOK가 기관별 당좌예금 계좌에서 대변/차변을 처리하고, KFTC가 성공응답을 받은 상태
    REJECTED // BOK가 결제요청을 거부한 상태
}
