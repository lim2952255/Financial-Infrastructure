package com.finance.infrastructure.common.money;

import java.util.Locale;
import java.util.Objects;

/**
 * ISO 4217 형식의 영문 3자리 통화 코드다.
 */
public record CurrencyCode(String value) {

    public CurrencyCode {
        Objects.requireNonNull(value, "currency code must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);

        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency code must be exactly three letters");
        }
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
