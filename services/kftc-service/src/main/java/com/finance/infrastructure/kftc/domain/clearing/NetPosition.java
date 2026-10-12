package com.finance.infrastructure.kftc.domain.clearing;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.kftc.domain.converter.InstitutionIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// 청산 주기에서 계산된 금융기관별 순지급 또는 순수취 금액
@Embeddable // NetPosition은 엔티티가 아니라 ClearingCycle에 포함되는 값 객체이다.
@EqualsAndHashCode // Lombok이 자동으로 equals()와 hashCode()를 생성하여 객체를 참조값기반으로 비교하는 것이 아니라 필드값을 기반으로 비교한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NetPosition {

    @Convert(converter = InstitutionIdConverter.class)
    @Column(name = "institution_id", nullable = false, updatable = false, length = 64)
    private InstitutionId institutionId;

    // 청산 주기에서 계산된 순지급 or 순수취액
    @Column(name = "net_amount", nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal netAmountValue;

    public NetPosition(InstitutionId institutionId, Money netAmount) {
        this.institutionId = Objects.requireNonNull(
                institutionId,
                "institution id must not be null"
        );
        Objects.requireNonNull(netAmount, "net amount must not be null");
        if (netAmount.amount().signum() == 0) {
            throw new IllegalArgumentException("net amount must not be zero");
        }
        this.netAmountValue = netAmount.amount();
    }

    public InstitutionId institutionId() {
        return institutionId;
    }

    public Money netAmount() {
        return new Money(netAmountValue);
    }
}
