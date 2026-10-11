package com.finance.infrastructure.kftc.service.payment;

import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstructionStatus;

// 수신은행의 입금요청 처리결과를 저장하는 레코드이다.
public record PaymentInstructionResult(
        String instructionId,
        PaymentInstructionStatus status
) {
    public static PaymentInstructionResult from(PaymentInstruction instruction) {
        return new PaymentInstructionResult(
                instruction.getInstructionId(),
                instruction.getStatus()
        );
    }
}
