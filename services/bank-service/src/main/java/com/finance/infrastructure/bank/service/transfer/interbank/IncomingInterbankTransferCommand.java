package com.finance.infrastructure.bank.service.transfer.interbank;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;

import java.util.Objects;

// KFTC가 수신은행에 전달하는 입금지시 정보를 담은 레코드이다.
public record IncomingInterbankTransferCommand(
        String instructionId,
        InstitutionId senderInstitutionId,
        String senderAccountNumber,
        InstitutionId receiverInstitutionId,
        String receiverAccountNumber,
        Money amount
) {
    public IncomingInterbankTransferCommand {
        Objects.requireNonNull(instructionId, "instruction id must not be null");
        Objects.requireNonNull(senderInstitutionId, "sender institution id must not be null");
        Objects.requireNonNull(receiverInstitutionId, "receiver institution id must not be null");
        Objects.requireNonNull(senderAccountNumber, "sender account number must not be null");
        Objects.requireNonNull(receiverAccountNumber, "receiver account number must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
