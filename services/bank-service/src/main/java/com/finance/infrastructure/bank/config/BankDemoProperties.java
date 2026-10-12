package com.finance.infrastructure.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Objects;

// application.properties에 들어있는 demo.* 설정들을 바인딩하여 record로 저장한다.
// @ConfigurationProperties(prefix = "demo")를 설정하면 application.properties에서 demo.*에 해당하는 변수값들을 읽어서 레코드에 바인딩한다.
@ConfigurationProperties(prefix = "demo")
public record BankDemoProperties(
        boolean seedEnabled, // 데모데이터 자동생성여부
        String customerName, // 첫번째 고객명
        String accountNumber, // 첫번째 계좌번호
        BigDecimal accountBalance, // 첫번째 계좌 예금액
        String secondaryCustomerName, // 두번째 고객명
        String secondaryAccountNumber, // 두번째 계좌번호
        BigDecimal secondaryAccountBalance // 두번째 계좌 예금액
) {
    public BankDemoProperties {
        customerName = requireText(customerName, "demo.customer-name");
        accountNumber = requireText(accountNumber, "demo.account-number");
        Objects.requireNonNull(accountBalance, "demo.account-balance must not be null");
        secondaryCustomerName = requireText(
                secondaryCustomerName,
                "demo.secondary-customer-name"
        );
        secondaryAccountNumber = requireText(
                secondaryAccountNumber,
                "demo.secondary-account-number"
        );
        Objects.requireNonNull(
                secondaryAccountBalance,
                "demo.secondary-account-balance must not be null"
        );
        if (accountBalance.signum() < 0) {
            throw new IllegalArgumentException("demo.account-balance must not be negative");
        }
        if (secondaryAccountBalance.signum() < 0) {
            throw new IllegalArgumentException(
                    "demo.secondary-account-balance must not be negative"
            );
        }
        if (accountNumber.equals(secondaryAccountNumber)) {
            throw new IllegalArgumentException("demo account numbers must be different");
        }
        if (customerName.equals(secondaryCustomerName)) {
            throw new IllegalArgumentException("demo customer names must be different");
        }
    }

    private static String requireText(String value, String property) {
        Objects.requireNonNull(value, property + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(property + " must not be blank");
        }
        return normalized;
    }
}
