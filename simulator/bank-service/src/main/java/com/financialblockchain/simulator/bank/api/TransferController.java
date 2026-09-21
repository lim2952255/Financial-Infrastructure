package com.financialblockchain.simulator.bank.api;

import com.financialblockchain.simulator.bank.api.BankApiModels.OutgoingTransferRequest;
import com.financialblockchain.simulator.bank.api.BankApiModels.TransferResponse;
import com.financialblockchain.simulator.bank.domain.transfer.OutgoingTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final OutgoingTransferService transferService;

    public TransferController(OutgoingTransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody OutgoingTransferRequest request) {
        return TransferResponse.from(transferService.transfer(
                request.transactionId(),
                request.sourceAccountNumber(),
                request.destinationBankId(),
                request.destinationAccountNumber(),
                request.amount(),
                request.simulateDestinationFailure()
        ));
    }

    @GetMapping("/{transactionId}")
    public TransferResponse get(@PathVariable String transactionId) {
        return TransferResponse.from(transferService.get(transactionId));
    }
}
