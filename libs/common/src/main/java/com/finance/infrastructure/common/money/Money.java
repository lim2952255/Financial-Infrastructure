package com.finance.infrastructure.common.money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 금액과 통화를 함께 전달하기 위한 기관 중립적 값 객체다.
 * 반올림, 허용 부호, 한도 같은 업무 규칙은 각 기관이 결정한다.
 */
public record Money(BigDecimal amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = normalize(amount);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, CurrencyCode.of(currencyCode));
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("money operations require the same currency");
        }
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}
