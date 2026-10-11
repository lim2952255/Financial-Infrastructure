package com.finance.infrastructure.kftc.domain.participant;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.kftc.domain.converter.InstitutionIdConverter;
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

// 금융결제원의 지급지시 중계와 청산에 참가하는 금융기관을 저장하는 엔티티
@Entity
@Table(name = "clearing_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClearingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 금융결제원의 지급지시 중계와 청산에 참가하는 금융기관
    @Convert(converter = InstitutionIdConverter.class)
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private InstitutionId institutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClearingParticipantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ClearingParticipant(InstitutionId institutionId) {
        this.institutionId = Objects.requireNonNull(
                institutionId,
                "institution id must not be null"
        );
        this.status = ClearingParticipantStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // 금융기관의 상태를 update하는 메서드
    public void suspend() {
        status = ClearingParticipantStatus.SUSPENDED;
    }

    public void activate() {
        status = ClearingParticipantStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ClearingParticipantStatus.ACTIVE;
    }
}
