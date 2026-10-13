package com.finance.infrastructure.krx.domain.participant;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.krx.domain.converter.InstitutionIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// KRX에 주문을 제출하고 거래에 참여하는 금융기관 정보를 저장하는 엔티티
@Entity
@Table(name = "exchange_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공통 기관 식별자
    @Convert(converter = InstitutionIdConverter.class)
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private InstitutionId institutionId;

    // 거래 참가기관 이름
    @Column(nullable = false, length = 100)
    private String name;

    // KRX 거래 참가 가능 여부를 나타내는 상태정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParticipantStatus status;

    // 참가기관 정보를 등록한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExchangeParticipant(InstitutionId institutionId, String name) {
        this.institutionId = Objects.requireNonNull(institutionId, "institution id must not be null");
        this.name = requireText(name, "participant name");
        this.status = ParticipantStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // 참가기관의 KRX 주문 제출 권한을 정지한다.
    public void suspend() {
        status = ParticipantStatus.SUSPENDED;
    }

    // 참가기관의 KRX 주문 제출 권한을 활성화한다.
    public void activate() {
        status = ParticipantStatus.ACTIVE;
    }

    // 현재 KRX에 주문을 제출할 수 있는 활성 참가기관인지 확인한다.
    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    // 주문접수 전에 참가기관의 거래 권한이 유효한지 검사한다.
    public void validateTradingPermission() {
        if (!isActive()) {
            throw new IllegalStateException("participant is not allowed to trade: " + status);
        }
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
