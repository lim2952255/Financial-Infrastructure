package com.finance.infrastructure.bok.domain.account;

import com.finance.infrastructure.bok.domain.participant.SettlementParticipant;
import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// 각 기관이 가지는 중앙은행의 당좌예금 계좌
@Entity
@Table(name = "settlement_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String accountNumber;

    // 각 금융기관은 하나의 중앙은행 당좌예금계좌만 개설할 수 있다.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, unique = true)
    private SettlementParticipant participant;

    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal balanceAmount;

    // 당좌예금 계좌의 상태를 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementAccountStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 새로운 기관의 당좌예금 계좌 생성
    public SettlementAccount(String accountNumber, SettlementParticipant participant) {
        this.accountNumber = requireAccountNumber(accountNumber);
        this.participant = Objects.requireNonNull(participant, "participant must not be null");
        if (!participant.isActive()) {
            throw new IllegalArgumentException("participant must be active");
        }
        participant.registerSettlementAccount(this);
        this.balanceAmount = Money.zero().amount();
        this.status = SettlementAccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // 당좌예금 입금
    public Money credit(Money amount) {
        requireActive();
        requirePositiveAmount(amount);
        Money newBalance = balance().add(amount);
        balanceAmount = newBalance.amount();
        return newBalance;
    }

    // 당좌예금 출금
    public Money debit(Money amount) {
        requireActive();
        requirePositiveAmount(amount);
        Money newBalance = balance().subtract(amount);
        if (newBalance.isNegative()) {
            throw new IllegalArgumentException("insufficient settlement account balance");
        }
        balanceAmount = newBalance.amount();
        return newBalance;
    }

    public Money balance() {
        return new Money(balanceAmount);
    }

    // 당좌예금 계좌 잠금
    public void freeze() {
        status = SettlementAccountStatus.FROZEN;
    }

    // 당좌예금 계좌 활성화
    public void activate() {
        if (!participant.isActive()) {
            throw new IllegalStateException("participant must be active");
        }
        status = SettlementAccountStatus.ACTIVE;
    }

    // 계좌를 close할때에는 당좌예금액이 0원이어야 한다.
    public void close() {
        if (!balance().equals(Money.zero())) {
            throw new IllegalStateException("only zero-balance accounts can be closed");
        }
        status = SettlementAccountStatus.CLOSED;
    }

    private void requireActive() {
        if (status != SettlementAccountStatus.ACTIVE || !participant.isActive()) {
            throw new IllegalStateException("settlement account is not active");
        }
    }

    private static void requirePositiveAmount(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static String requireAccountNumber(String accountNumber) {
        Objects.requireNonNull(accountNumber, "account number must not be null");
        String normalized = accountNumber.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("account number must not be blank");
        }
        return normalized;
    }
}
