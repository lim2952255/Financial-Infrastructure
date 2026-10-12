package com.finance.infrastructure.bank.domain.account;

import com.finance.infrastructure.common.institution.InstitutionId;

import java.util.Objects;

// 어떤 기관의 계좌인지를 나타내는 레코드
public record AccountReference(
        InstitutionId institutionId,
        String accountNumber
) {

    public AccountReference {
        Objects.requireNonNull(institutionId, "institution id must not be null");
        Objects.requireNonNull(accountNumber, "account number must not be null");
        accountNumber = accountNumber.trim();
        if (accountNumber.isEmpty()) {
            throw new IllegalArgumentException("account number must not be blank");
        }
    }

    public static AccountReference of(String institutionId, String accountNumber) {
        return new AccountReference(InstitutionId.of(institutionId), accountNumber);
    }
}
