package com.financialblockchain.simulator.bank.api;

import com.financialblockchain.simulator.bank.domain.account.Account;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntry;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntryType;
import com.financialblockchain.simulator.bank.domain.transfer.OutgoingTransfer;
import com.financialblockchain.simulator.bank.domain.transfer.TransferStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class BankApiModels {

    private BankApiModels() {
    }

    public record CreateAccountRequest(
            @NotBlank String accountNumber,
            @NotBlank String accountHolder,
            @NotNull @DecimalMin("0.00") BigDecimal openingBalance
    ) {
    }

    public record AccountResponse(
            String bankId,
            String accountNumber,
            String accountHolder,
            BigDecimal balance
    ) {
        public static AccountResponse from(Account account) {
            return new AccountResponse(
                    account.getBankId(),
                    account.getAccountNumber(),
                    account.getAccountHolder(),
                    account.getBalance()
            );
        }
    }

    public record LedgerPostRequest(
            @NotBlank String transactionId,
            @NotBlank String accountNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String counterpartyBankId,
            @NotBlank String counterpartyAccountNumber
    ) {
    }

    public record LedgerEntryResponse(
            String transactionId,
            String bankId,
            String accountNumber,
            LedgerEntryType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String counterpartyBankId,
            String counterpartyAccountNumber,
            Instant occurredAt
    ) {
        public static LedgerEntryResponse from(LedgerEntry entry) {
            return new LedgerEntryResponse(
                    entry.getTransactionId(),
                    entry.getBankId(),
                    entry.getAccountNumber(),
                    entry.getType(),
                    entry.getAmount(),
                    entry.getBalanceBefore(),
                    entry.getBalanceAfter(),
                    entry.getCounterpartyBankId(),
                    entry.getCounterpartyAccountNumber(),
                    entry.getOccurredAt()
            );
        }
    }

    public record OutgoingTransferRequest(
            @NotBlank String transactionId,
            @NotBlank String sourceAccountNumber,
            @NotBlank String destinationBankId,
            @NotBlank String destinationAccountNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            boolean simulateDestinationFailure
    ) {
    }

    public record IncomingCreditRequest(
            @NotBlank String transactionId,
            @NotBlank String destinationAccountNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String sourceBankId,
            @NotBlank String sourceAccountNumber,
            boolean simulateFailure
    ) {
    }

    public record TransferResponse(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount,
            TransferStatus status,
            String failureReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TransferResponse from(OutgoingTransfer transfer) {
            return new TransferResponse(
                    transfer.getTransactionId(),
                    transfer.getSourceAccountNumber(),
                    transfer.getDestinationBankId(),
                    transfer.getDestinationAccountNumber(),
                    transfer.getAmount(),
                    transfer.getStatus(),
                    transfer.getFailureReason(),
                    transfer.getCreatedAt(),
                    transfer.getUpdatedAt()
            );
        }
    }

    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {
    }
}
