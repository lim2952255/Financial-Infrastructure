package com.financialblockchain.simulator.bank.domain.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ledger_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ledger_transaction_type",
                columnNames = {"transaction_id", "entry_type"}
        )
)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 80)
    private String transactionId;

    @Column(name = "bank_id", nullable = false, length = 40)
    private String bankId;

    @Column(name = "account_number", nullable = false, length = 80)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private LedgerEntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_bank_id", nullable = false, length = 40)
    private String counterpartyBankId;

    @Column(name = "counterparty_account_number", nullable = false, length = 80)
    private String counterpartyAccountNumber;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(
            String transactionId,
            String bankId,
            String accountNumber,
            LedgerEntryType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String counterpartyBankId,
            String counterpartyAccountNumber
    ) {
        this.transactionId = transactionId;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.counterpartyBankId = counterpartyBankId;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.occurredAt = Instant.now();
    }

    public static LedgerEntry posted(
            String transactionId,
            String bankId,
            String accountNumber,
            LedgerEntryType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String counterpartyBankId,
            String counterpartyAccountNumber
    ) {
        return new LedgerEntry(
                transactionId,
                bankId,
                accountNumber,
                type,
                amount,
                balanceBefore,
                balanceAfter,
                counterpartyBankId,
                counterpartyAccountNumber
        );
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getBankId() {
        return bankId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCounterpartyBankId() {
        return counterpartyBankId;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
