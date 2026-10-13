package com.finance.infrastructure.ksd.domain.ledger;

// KSD 증권원장에 기록된 수량변화가 어떤 이유로 발생했는지를 기록하는 Enum
public enum SecuritiesLedgerEntryType {
    TRANSFER_IN, // 계좌대체로 주식이 해당 증권사 계좌로 들어오는 경우
    TRANSFER_OUT, // 계좌대체로 주식 해당 증권사 계좌에서 나간 경우
    SETTLEMENT_LOCK, // 결제일에 지급해야 할 주식을 다른 결제에서 사용할 수 없도록 lock을 거는 경우
    SETTLEMENT_RELEASE, // 예약된 결제가 취소되거나 승인전에 실패하여 lock을 해제하는 경우
    SETTLEMENT_DELIVERY, // 송신 증권계좌에서 결제에 성공하여 예약된 주식을 실제로 차감하여 전달하는 경우
    SETTLEMENT_RECEIPT // 수신 증권계좌에서 결제에 성공하여 주식을 전달받는 경우
}
