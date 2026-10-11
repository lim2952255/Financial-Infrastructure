package com.finance.infrastructure.kftc.service.payment;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;

import java.util.Objects;

// 송신은행이 KFTC에 전달하는 개별 지급지시 명령이다.
public record PaymentInstructionCommand(
        String requestId,
        InstitutionId senderInstitutionId,
        String senderAccountNumber,
        InstitutionId receiverInstitutionId,
        String receiverAccountNumber,
        Money amount
) {
    public PaymentInstructionCommand {
        Objects.requireNonNull(requestId, "request id must not be null");
        Objects.requireNonNull(senderInstitutionId, "sender institution id must not be null");
        Objects.requireNonNull(senderAccountNumber, "sender account number must not be null");
        Objects.requireNonNull(receiverInstitutionId, "receiver institution id must not be null");
        Objects.requireNonNull(receiverAccountNumber, "receiver account number must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
