package com.financialblockchain.simulator.bank.controller;

import com.financialblockchain.simulator.bank.dto.BankApiModels.OutgoingTransferRequest;
import com.financialblockchain.simulator.bank.dto.BankApiModels.TransferResponse;
import com.financialblockchain.simulator.bank.service.transfer.OutgoingTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 외부 사용자가 은행 간 송금을 시작하고 진행 상태를 조회하는 HTTP 진입점이다. */
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
