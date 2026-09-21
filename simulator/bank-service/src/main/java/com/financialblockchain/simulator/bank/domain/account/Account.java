package com.financialblockchain.simulator.bank.domain.account;

import com.financialblockchain.simulator.bank.support.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_number", columnNames = "account_number")
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, length = 40)
    private String bankId;

    @Column(name = "account_number", nullable = false, length = 80)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 100)
    private String accountHolder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected Account() {
    }

    private Account(String bankId, String accountNumber, String accountHolder, BigDecimal openingBalance) {
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = openingBalance;
    }

    public static Account open(
            String bankId,
            String accountNumber,
            String accountHolder,
            BigDecimal openingBalance
    ) {
        if (openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        return new Account(bankId, accountNumber, accountHolder, openingBalance);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountNumber, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public Long getId() {
        return id;
    }

    public String getBankId() {
        return bankId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
