package com.finance.infrastructure.bok.domain.participant;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.converter.InstitutionIdConverter;
import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// 중앙은행의 당좌예금 계좌를 소유하는 금융기관들을 저장하는 엔티티
@Entity
@Table(name = "settlement_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = InstitutionIdConverter.class)
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private InstitutionId institutionId;

    @Column(nullable = false, length = 100)
    private String name;

    // 각 금융기관의 상태 관리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementParticipantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "participant", fetch = FetchType.LAZY)
    private SettlementAccount settlementAccount;

    public SettlementParticipant(InstitutionId institutionId, String name) {
        this.institutionId = Objects.requireNonNull(
                institutionId,
                "institution id must not be null"
        );
        this.name = requireName(name);
        this.status = SettlementParticipantStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // 금융기관의 상태를 update
    public void suspend() {
        status = SettlementParticipantStatus.SUSPENDED;
    }

    public void activate() {
        status = SettlementParticipantStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == SettlementParticipantStatus.ACTIVE;
    }

    // 하나의 금융기관에는 하나의 중앙은행 당좌예금계좌만 연결할 수 있다.
    public void registerSettlementAccount(SettlementAccount account) {
        Objects.requireNonNull(account, "settlement account must not be null");
        if (account.getParticipant() != this) {
            throw new IllegalArgumentException("settlement account belongs to another participant");
        }
        if (settlementAccount != null && settlementAccount != account) {
            throw new IllegalStateException("participant already has a settlement account");
        }
        settlementAccount = account;
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "participant name must not be null");
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("participant name must not be blank");
        }
        return normalized;
    }
}
