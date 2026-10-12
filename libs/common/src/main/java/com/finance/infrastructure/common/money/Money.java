package com.finance.infrastructure.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 시스템에서 사용하는 원화 금액을 표현한다.
 */
public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        amount = normalize(amount);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    // Record는 불변객체이기 때문에 Money의 amount를 수정하기 위해서는 새로운 객체를 생성해야 한다.
    public Money add(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        return new Money(amount.subtract(other.amount));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private static BigDecimal normalize(BigDecimal value) {
        try {
            return value.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "money must not contain decimal places",
                    exception
            );
        }
    }
}
