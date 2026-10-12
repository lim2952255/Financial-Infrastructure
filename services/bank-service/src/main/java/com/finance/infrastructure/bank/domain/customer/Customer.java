package com.finance.infrastructure.bank.domain.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bank_customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성시간

    public Customer(String name) {
        this.name = requireName(name);
        this.createdAt = LocalDateTime.now();
    }

    // 사용자 이름 검사
    private static String requireName(String name) {
        Objects.requireNonNull(name, "customer name must not be null");
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("customer name must not be blank");
        }
        return normalized;
    }
}
