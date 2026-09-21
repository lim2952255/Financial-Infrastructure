package com.financialblockchain.simulator.bank.controller;

import com.financialblockchain.simulator.bank.dto.BankApiModels.LedgerEntryResponse;
import com.financialblockchain.simulator.bank.dto.BankApiModels.LedgerPostRequest;
import com.financialblockchain.simulator.bank.service.ledger.LedgerPostingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 출금·입금 원장을 직접 주입하고 거래 ID별 원장을 조회하는 시뮬레이션 API다. */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerPostingService ledgerPostingService;

    public LedgerController(LedgerPostingService ledgerPostingService) {
        this.ledgerPostingService = ledgerPostingService;
    }

    @PostMapping("/debits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryResponse debit(@Valid @RequestBody LedgerPostRequest request) {
        return LedgerEntryResponse.from(ledgerPostingService.postDebit(
                request.transactionId(),
                request.accountNumber(),
                request.amount(),
                request.counterpartyBankId(),
                request.counterpartyAccountNumber()
        ));
    }

    @PostMapping("/credits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryResponse credit(@Valid @RequestBody LedgerPostRequest request) {
        return LedgerEntryResponse.from(ledgerPostingService.postCredit(
                request.transactionId(),
                request.accountNumber(),
                request.amount(),
                request.counterpartyBankId(),
                request.counterpartyAccountNumber()
        ));
    }

    @GetMapping
    public List<LedgerEntryResponse> find(
            @RequestParam(required = false) String transactionId
    ) {
        if (transactionId == null || transactionId.isBlank()) {
            return ledgerPostingService.findAll().stream()
                    .map(LedgerEntryResponse::from)
                    .toList();
        }
        return ledgerPostingService.findByTransactionId(transactionId).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }
}
