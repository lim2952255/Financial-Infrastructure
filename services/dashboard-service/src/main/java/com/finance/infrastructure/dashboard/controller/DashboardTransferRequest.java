package com.finance.infrastructure.dashboard.controller;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// 프론트에서 선택한 송수신 기관·계좌와 requestId를 전달한다.
public record DashboardTransferRequest(
        @NotBlank String requestId,
        @NotBlank String senderInstitutionId,
        @NotNull @Positive Long senderCustomerId,
        @NotBlank String senderAccountNumber,
        @NotBlank String receiverInstitutionId,
        @NotBlank String receiverAccountNumber,
        @NotNull @Positive @Digits(integer = 19, fraction = 0) BigDecimal amount
) {
}
