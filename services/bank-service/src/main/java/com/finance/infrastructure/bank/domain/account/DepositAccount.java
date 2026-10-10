package com.finance.infrastructure.bank.domain.account;

import com.finance.infrastructure.bank.domain.customer.Customer;
import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "deposit_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 매개변수가 없는 protected 생성자 자동생성
public class DepositAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // 한 사용자는 여러 예금계좌를 보유할 수 있다.

    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal balanceAmount;

    public DepositAccount(String accountNumber, Customer customer) {
        this.accountNumber = requireAccountNumber(accountNumber);
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.balanceAmount = Money.zero().amount();
    }

    // 예금을 증가시키는 메서드
    public Money increase(Money amount) {
        requirePositiveAmount(amount);
        Money newBalance = balance().add(amount);
        balanceAmount = newBalance.amount();
        return newBalance;
    }

    // 예금을 감소시키는 메서드
    public Money decrease(Money amount) {
        requirePositiveAmount(amount);
        Money newBalance = balance().subtract(amount);
        if (newBalance.isNegative()) {
            throw new IllegalArgumentException("insufficient account balance");
        }
        balanceAmount = newBalance.amount();
        return newBalance;
    }

    public Money balance() {
        return new Money(balanceAmount);
    }

    private void requirePositiveAmount(Money amount) {
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
