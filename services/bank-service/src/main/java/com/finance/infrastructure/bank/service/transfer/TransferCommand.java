package com.finance.infrastructure.bank.service.transfer;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;

import java.util.Objects;

// 이체 정보를 담은 Record
public record TransferCommand(
        String requestId,
        String senderAccountNumber,
        InstitutionId receiverInstitutionId,
        String receiverAccountNumber,
        Money amount
) {

    public TransferCommand {
        requestId = requireText(requestId, "request id");
        senderAccountNumber = requireText(senderAccountNumber, "sender account number");
        Objects.requireNonNull(receiverInstitutionId, "receiver institution id must not be null");
        receiverAccountNumber = requireText(receiverAccountNumber, "receiver account number");
        Objects.requireNonNull(amount, "amount must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
