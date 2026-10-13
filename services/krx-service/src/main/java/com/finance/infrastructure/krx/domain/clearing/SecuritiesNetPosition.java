package com.finance.infrastructure.krx.domain.clearing;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.krx.domain.converter.InstitutionIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Locale;

// 상계 결과로 계산된 참가기관·종목별 증권 순결제 수량을 나타내는 값 객체
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesNetPosition {

    // 증권을 인도하거나 수령할 참가기관의 식별자
    @Convert(converter = InstitutionIdConverter.class)
    @Column(name = "participant_id", nullable = false, updatable = false, length = 64)
    private InstitutionId participantId;

    // 순결제 대상인 상장종목의 종목코드
    @Column(name = "security_code", nullable = false, updatable = false, length = 32)
    private String securityCode;

    // 참가기관이 해당 종목을 최종 인도하거나 수령할 수량으로 0인 포지션은 저장하지 않는다.
    // 양수는 증권 수령, 음수는 증권 인도를 의미한다.
    @Column(name = "net_quantity", nullable = false, updatable = false)
    private long netQuantity;

    public SecuritiesNetPosition(InstitutionId participantId, String securityCode, long netQuantity) {
        this.participantId = Objects.requireNonNull(participantId, "participant id must not be null");
        this.securityCode = requireText(securityCode, "security code").toUpperCase(Locale.ROOT);
        if (netQuantity == 0) {
            throw new IllegalArgumentException("net quantity must not be zero");
        }
        this.netQuantity = netQuantity;
    }

    public InstitutionId participantId() {
        return participantId;
    }

    public String securityCode() {
        return securityCode;
    }

    public long netQuantity() {
        return netQuantity;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
