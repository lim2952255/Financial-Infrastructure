package com.finance.infrastructure.kftc.domain.payment;

/**
 * KFTC가 관리하는 개별 지급지시의 처리상태
 * BANK_A -> KFTC -> BANK_B
 * 이후 청산주기마다 개별 지급지시들을 모아 청산후 기관별 결제정보를 BOK에게 전달.
 * BOK 결제 결과를 각 은행에 다시 전달
 * */

public enum PaymentInstructionStatus {
    RECEIVED, // 송신은행으로부터 지급지시를 접수한 상태
    FORWARDED, // KFTC가 수신은행에 입금지시를 전송한 상태
    RECEIVER_CREDITED, // 수신은행에서 예금 증가와 원장 기록을 완료하여 청산대상에 포함할 수 있는 상태
    CLEARED, // 특정 ClearingCycle에 포함되어 청산되어, 기관별 순포지션 계산이 끝난 상태
    SETTLED, // KFTC가 BOK에 결제를 요청하고, BOK가 기관별 당좌예금계좌의 차변/대변 처리를 완료한 상태
    REJECTED // 수신자은행의 입금 전에 지급지시가 거절된 상태입니다.
}
