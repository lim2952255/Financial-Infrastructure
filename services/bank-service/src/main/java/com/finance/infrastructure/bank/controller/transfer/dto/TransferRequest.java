package com.finance.infrastructure.bank.controller.transfer.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// 이체 요청
public record TransferRequest(
        @NotBlank String requestId,
        @NotBlank String senderAccountNumber,
        @NotBlank String receiverInstitutionId,
        @NotBlank String receiverAccountNumber,
        @NotNull @Positive @Digits(integer = 19, fraction = 0) BigDecimal amount
) {
}
