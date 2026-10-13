package com.finance.infrastructure.ksd.domain.account;

import com.finance.infrastructure.ksd.domain.participant.DepositoryParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// KSD가 참가기관의 총 증권 보유량을 저장하고 관리하는 상위 계좌 엔티티
@Entity
@Table(name = "participant_securities_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantSecuritiesAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String accountNumber;

    // 참가 금융기관
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, unique = true)
    private DepositoryParticipant participant;

    // 증권계좌의 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DepositoryAccountStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ParticipantSecuritiesAccount(
            String accountNumber,
            DepositoryParticipant participant
    ) {
        this.accountNumber = requireText(accountNumber, "account number");
        this.participant = Objects.requireNonNull(participant, "participant must not be null");
        if (!participant.isActive()) {
            throw new IllegalArgumentException("participant must be active");
        }
        this.status = DepositoryAccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void freeze() {
        status = DepositoryAccountStatus.FROZEN;
    }

    public void activate() {
        if (!participant.isActive()) {
            throw new IllegalStateException("participant must be active");
        }
        status = DepositoryAccountStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == DepositoryAccountStatus.ACTIVE && participant.isActive();
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
