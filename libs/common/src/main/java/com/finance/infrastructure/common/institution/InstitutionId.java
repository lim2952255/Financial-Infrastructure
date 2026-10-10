package com.finance.infrastructure.common.institution;

import java.util.Locale;
import java.util.Objects;

/**
 * 현재 실행 주체를 식별한다. 예: BANK_A, BANK_B, BOK.
 */
public record InstitutionId(String value) {

    public InstitutionId {
        Objects.requireNonNull(value, "institution id must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);

        if (!value.matches("[A-Z][A-Z0-9_-]{0,63}")) {
            throw new IllegalArgumentException(
                    "institution id must contain only uppercase letters, numbers, '_' or '-'"
            );
        }
    }

    // Institution Id 생성
    public static InstitutionId of(String value) {
        return new InstitutionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
