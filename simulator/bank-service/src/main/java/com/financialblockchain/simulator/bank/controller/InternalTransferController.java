package com.financialblockchain.simulator.bank.controller;

import com.financialblockchain.simulator.bank.dto.BankApiModels.IncomingCreditRequest;
import com.financialblockchain.simulator.bank.dto.BankApiModels.LedgerEntryResponse;
import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.exception.SimulatedCreditFailureException;
import com.financialblockchain.simulator.bank.service.ledger.LedgerPostingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 상대 은행이 호출하는 내부 입금 API를 제공한다. 실제 인증은 PoC 범위에서 제외한다. */
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
        // 인증 기능은 없지만, 시뮬레이션 설정과 다른 출발 은행 ID는 잘못된 입력으로 거부한다.
        if (!bankProperties.counterpartId().equals(request.sourceBankId())) {
            throw new IllegalArgumentException(
                    "This simulation instance only accepts credits from " + bankProperties.counterpartId()
            );
        }
        // 분산 트랜잭션의 부분 성공을 재현하기 위해 DB 반영 전에 의도적으로 실패시킬 수 있다.
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
