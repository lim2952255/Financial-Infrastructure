package com.financialblockchain.simulator.bank.api;

import com.financialblockchain.simulator.bank.api.BankApiModels.IncomingCreditRequest;
import com.financialblockchain.simulator.bank.api.BankApiModels.LedgerEntryResponse;
import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerPostingService;
import com.financialblockchain.simulator.bank.support.SimulatedCreditFailureException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/transfers")
public class InternalTransferController {

    private final LedgerPostingService ledgerPostingService;
    private final BankProperties bankProperties;

    public InternalTransferController(
            LedgerPostingService ledgerPostingService,
            BankProperties bankProperties
    ) {
        this.ledgerPostingService = ledgerPostingService;
        this.bankProperties = bankProperties;
    }

    @PostMapping("/credits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryResponse credit(@Valid @RequestBody IncomingCreditRequest request) {
        if (!bankProperties.counterpartId().equals(request.sourceBankId())) {
            throw new IllegalArgumentException(
                    "This simulation instance only accepts credits from " + bankProperties.counterpartId()
            );
        }
        if (request.simulateFailure()) {
            throw new SimulatedCreditFailureException(request.transactionId());
        }
        return LedgerEntryResponse.from(ledgerPostingService.postCredit(
                request.transactionId(),
                request.destinationAccountNumber(),
                request.amount(),
                request.sourceBankId(),
                request.sourceAccountNumber()
        ));
    }
}
