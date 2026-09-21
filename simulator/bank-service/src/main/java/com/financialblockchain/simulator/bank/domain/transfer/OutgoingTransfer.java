package com.financialblockchain.simulator.bank.domain.transfer;

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
        name = "outgoing_transfers",
        uniqueConstraints = @UniqueConstraint(name = "uk_outgoing_transaction", columnNames = "transaction_id")
)
public class OutgoingTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 80)
    private String transactionId;

    @Column(name = "source_account_number", nullable = false, length = 80)
    private String sourceAccountNumber;

    @Column(name = "destination_bank_id", nullable = false, length = 40)
    private String destinationBankId;

    @Column(name = "destination_account_number", nullable = false, length = 80)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutgoingTransfer() {
    }

    private OutgoingTransfer(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount
    ) {
        this.transactionId = transactionId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationBankId = destinationBankId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.status = TransferStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static OutgoingTransfer start(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount
    ) {
        return new OutgoingTransfer(
                transactionId,
                sourceAccountNumber,
                destinationBankId,
                destinationAccountNumber,
                amount
        );
    }

    public void markDebitPosted() {
        changeStatus(TransferStatus.DEBIT_POSTED, null);
    }

    public void markCompleted() {
        changeStatus(TransferStatus.COMPLETED, null);
    }

    public void markDebitFailed(String reason) {
        changeStatus(TransferStatus.DEBIT_FAILED, reason);
    }

    public void markCreditFailed(String reason) {
        changeStatus(TransferStatus.CREDIT_FAILED, reason);
    }

    private void changeStatus(TransferStatus nextStatus, String reason) {
        this.status = nextStatus;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationBankId() {
        return destinationBankId;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
