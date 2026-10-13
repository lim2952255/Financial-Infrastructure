package com.finance.infrastructure.ksd.domain.participant;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.ksd.domain.converter.InstitutionIdConverter;
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

// KSD에 계좌를 개설하고 예탁·결제 업무에 참여하는 금융기관을 저장하는 엔티티
@Entity
@Table(name = "depository_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositoryParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기관정보
    @Convert(converter = InstitutionIdConverter.class)
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private InstitutionId institutionId;

    @Column(nullable = false, length = 100)
    private String name;

    // 참여기관의 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParticipantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DepositoryParticipant(InstitutionId institutionId, String name) {
        this.institutionId = Objects.requireNonNull(institutionId, "institution id must not be null");
        this.name = requireText(name, "participant name");
        this.status = ParticipantStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void suspend() {
        status = ParticipantStatus.SUSPENDED;
    }

    public void activate() {
        status = ParticipantStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
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
