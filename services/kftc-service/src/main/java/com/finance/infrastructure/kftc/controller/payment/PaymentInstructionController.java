package com.finance.infrastructure.kftc.controller.payment;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.kftc.service.payment.PaymentInstructionCommand;
import com.finance.infrastructure.kftc.service.payment.PaymentInstructionResult;
import com.finance.infrastructure.kftc.service.payment.PaymentInstructionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// 송신은행이 지급지시를 접수하는 KFTC API다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-instructions")
public class PaymentInstructionController {

    // 송신은행으로부터 지급지시를 접수하고, 수신은행에게 입금요청을 전송하는 서비스
    private final PaymentInstructionService paymentInstructionService;

    @PostMapping
    public ResponseEntity<PaymentInstructionResult> receive(
            @RequestBody PaymentInstructionRequest request
    ) {
        // 송신은행으로부터 받은 지급지시정보를 기반으로 지급지시를 생성하고, 수신은행에 입금요청을 보내고, 입금처리결과를 리턴받는다.
        PaymentInstructionResult result = paymentInstructionService.receive(
                new PaymentInstructionCommand(
                        request.requestId(),
                        InstitutionId.of(request.senderInstitutionId()),
                        request.senderAccountNumber(),
                        InstitutionId.of(request.receiverInstitutionId()),
                        request.receiverAccountNumber(),
                        Money.of(request.amount())
                )
        );
        // 서비스 처리 결과를 그대로 송신은행에 응답한다.
        // Enum 상태값은 JSON에서 "RECEIVER_CREDITED"와 같은 문자열로 변환된다.
        return ResponseEntity.ok(result);
    }

    // 송신은행으로부터 받은 지급지시 요청 정보
    public record PaymentInstructionRequest(
            String requestId,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount
    ) {
    }

}
