package com.financialblockchain.simulator.bank.domain.transfer;

import com.financialblockchain.simulator.bank.api.BankApiModels.IncomingCreditRequest;
import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerPostingService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OutgoingTransferService {

    private final LedgerPostingService ledgerPostingService;
    private final TransferLifecycleService lifecycleService;
    private final RestClient counterpartRestClient;
    private final BankProperties bankProperties;

    public OutgoingTransferService(
            LedgerPostingService ledgerPostingService,
            TransferLifecycleService lifecycleService,
            RestClient counterpartRestClient,
            BankProperties bankProperties
    ) {
        this.ledgerPostingService = ledgerPostingService;
        this.lifecycleService = lifecycleService;
        this.counterpartRestClient = counterpartRestClient;
        this.bankProperties = bankProperties;
    }

    public OutgoingTransfer transfer(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount,
            boolean simulateDestinationFailure
    ) {
        requireConfiguredCounterpart(destinationBankId);
        lifecycleService.create(
                transactionId,
                sourceAccountNumber,
                destinationBankId,
                destinationAccountNumber,
                amount
        );

        try {
            ledgerPostingService.postDebit(
                    transactionId,
                    sourceAccountNumber,
                    amount,
                    destinationBankId,
                    destinationAccountNumber
            );
            lifecycleService.markDebitPosted(transactionId);
        } catch (RuntimeException exception) {
            lifecycleService.markDebitFailed(transactionId, exception.getMessage());
            throw exception;
        }

        try {
            counterpartRestClient.post()
                    .uri("/internal/transfers/credits")
                    .body(new IncomingCreditRequest(
                            transactionId,
                            destinationAccountNumber,
                            amount,
                            bankProperties.id(),
                            sourceAccountNumber,
                            simulateDestinationFailure
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return lifecycleService.markCompleted(transactionId);
        } catch (RestClientException exception) {
            return lifecycleService.markCreditFailed(transactionId, exception.getMessage());
        }
    }

    public OutgoingTransfer get(String transactionId) {
        return lifecycleService.get(transactionId);
    }

    private void requireConfiguredCounterpart(String destinationBankId) {
        if (!bankProperties.counterpartId().equals(destinationBankId)) {
            throw new IllegalArgumentException(
                    "This simulation instance only supports counterpart " + bankProperties.counterpartId()
            );
        }
    }
}
