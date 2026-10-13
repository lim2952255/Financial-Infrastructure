package com.finance.infrastructure.broker.domain.account;

import com.finance.infrastructure.broker.domain.customer.Customer;
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

import java.time.LocalDateTime;
import java.util.Objects;

// 각 고객이 소유하고 있는 증권계좌 정보를 담고 있는 엔티티
@Entity
@Table(name = "securities_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritiesAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SecuritiesAccount(String accountNumber, Customer customer) {
        this.accountNumber = requireText(accountNumber, "account number");
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.createdAt = LocalDateTime.now();
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
