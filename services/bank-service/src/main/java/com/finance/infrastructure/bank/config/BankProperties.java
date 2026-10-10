package com.finance.infrastructure.bank.config;

import com.finance.infrastructure.common.institution.InstitutionId;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

// 환경변수에서 기관ID 정보를 읽어서 저장한다.
@ConfigurationProperties(prefix = "finance")
public record BankProperties(String institutionId) {

    public BankProperties {
        Objects.requireNonNull(institutionId, "finance.institution-id must not be null");
    }

    // 현재 기관정보를 리턴한다.
    public InstitutionId currentInstitutionId() {
        return InstitutionId.of(institutionId);
    }
}
