package com.finance.infrastructure.krx.domain.clearing;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.krx.domain.converter.InstitutionIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// 상계 결과로 계산된 특정 참가기관의 현금 순결제 금액을 나타내는 값 객체
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashNetPosition {

    // 현금을 지급하거나 수령할 참가기관의 식별자
    @Convert(converter = InstitutionIdConverter.class)
    @Column(name = "participant_id", nullable = false, updatable = false, length = 64)
    private InstitutionId participantId;

    // 참가기관의 현금 순결제 금액으로 0원인 포지션은 저장하지 않는다.
    // 양수는 현금 수령, 음수는 현금 지급을 의미한다.
    @Column(name = "net_amount", nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal netAmountValue;

    public CashNetPosition(InstitutionId participantId, Money netAmount) {
        this.participantId = Objects.requireNonNull(participantId, "participant id must not be null");
        Objects.requireNonNull(netAmount, "net amount must not be null");
        if (netAmount.amount().signum() == 0) {
            throw new IllegalArgumentException("net amount must not be zero");
        }
        this.netAmountValue = netAmount.amount();
    }

    public InstitutionId participantId() {
        return participantId;
    }

    public Money netAmount() {
        return new Money(netAmountValue);
    }
}
