package com.finance.infrastructure.bank.config;

import com.finance.infrastructure.common.institution.InstitutionId;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

// 현재 은행의 기관 ID와 KFTC API 주소를 환경설정에서 읽는다.
@ConfigurationProperties(prefix = "finance")
public record BankProperties(String institutionId, String kftcBaseUrl) {

    public BankProperties {
        Objects.requireNonNull(institutionId, "finance.institution-id must not be null");
        Objects.requireNonNull(kftcBaseUrl, "finance.kftc-base-url must not be null");
    }

    // 현재 기관정보를 리턴한다.
    public InstitutionId currentInstitutionId() {
        return InstitutionId.of(institutionId);
    }
}
