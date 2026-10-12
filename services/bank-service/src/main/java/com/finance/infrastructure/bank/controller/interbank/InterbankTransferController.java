package com.finance.infrastructure.bank.controller.interbank;

import com.finance.infrastructure.bank.service.transfer.TransferResult;
import com.finance.infrastructure.bank.service.transfer.interbank.IncomingInterbankTransferCommand;
import com.finance.infrastructure.bank.service.transfer.interbank.IncomingInterbankTransferService;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// KFTC가 수신은행에 고객 계좌 입금을 요청하는 내부 API다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interbank-transfers")
public class InterbankTransferController {

    // 수신은행의 입금요청을 처리하는 서비스
    private final IncomingInterbankTransferService incomingTransferService;

    // KFTC가 수신은행에게 수신계좌의 입금을 요청하는 API
    @PostMapping("/credits")
    public ResponseEntity<CreditResponse> credit(@RequestBody CreditRequest request) {
        // 수신은행의 입금요청을 처리하고 결과를 리턴받는다.
        TransferResult result = incomingTransferService.credit(
                new IncomingInterbankTransferCommand(
                        request.instructionId(),
                        InstitutionId.of(request.senderInstitutionId()),
                        request.senderAccountNumber(),
                        InstitutionId.of(request.receiverInstitutionId()),
                        request.receiverAccountNumber(),
                        Money.of(request.amount())
                )
        );
        // KFTC에 수신계좌의 입금 처리를 완료했다는 응답을 전송한다.
        return ResponseEntity.ok(new CreditResponse(
                result.transactionId(),
                result.status().name()
        ));
    }

    // KFTC가 수신은행에 전송하는 이체정보를 담은 record
    public record CreditRequest(
            String instructionId,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount
    ) {
    }

    // 수신은행에서 수신계좌의 입금처리 요청의 성공 여부를 담은 레코드
    public record CreditResponse(Long transactionId, String status) {
    }
}
