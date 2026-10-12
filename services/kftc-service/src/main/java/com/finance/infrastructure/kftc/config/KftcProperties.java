package com.finance.infrastructure.kftc.config;

import com.finance.infrastructure.common.institution.InstitutionId;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Objects;

// KFTC가 통신할 BOK와 각 은행의 API 주소를 관리한다.
@ConfigurationProperties(prefix = "finance")
public record KftcProperties(String bokBaseUrl, Map<String, String> bankBaseUrls) {

    public KftcProperties {
        Objects.requireNonNull(bokBaseUrl, "finance.bok-base-url must not be null");
        bankBaseUrls = Map.copyOf(Objects.requireNonNull(
                bankBaseUrls,
                "finance.bank-base-urls must not be null"
        ));
    }

    public String bankBaseUrl(InstitutionId institutionId) {
        String baseUrl = bankBaseUrls.get(institutionId.value());
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "bank API URL is not configured: " + institutionId.value()
            );
        }
        return baseUrl;
    }
}
