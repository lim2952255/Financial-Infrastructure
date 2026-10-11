package com.finance.infrastructure.bok.domain.settlement;

import com.finance.infrastructure.bok.domain.converter.InstitutionIdConverter;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// 특정기관의 순결제 금액을 나타내는 객체
@Embeddable // SettlementPosition은 엔티티가 아니라, 다른 엔티티에 포함될수 있는 값 객체이다.
@EqualsAndHashCode // EqualsAndHashCode는 Lombok이 equals()와 HashCode()를 자동으로 생성하여, 객체를 참조값이 아니라 필드값 기반으로 비교하게 만든다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPosition {

    // 기관정보
    @Convert(converter = InstitutionIdConverter.class)
    @Column(name = "institution_id", nullable = false, updatable = false, length = 64)
    private InstitutionId institutionId;

    // 기관의 당좌예금 계좌에서 결제해야 하는 결제액
    @Column(name = "net_amount", nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal netAmountValue;

    public SettlementPosition(InstitutionId institutionId, Money netAmount) {
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
