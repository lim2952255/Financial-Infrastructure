package com.finance.infrastructure.bank.client.kftc;

import com.finance.infrastructure.bank.config.BankProperties;
import com.finance.infrastructure.bank.service.transfer.TransferCommand;
import com.finance.infrastructure.common.institution.InstitutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

// 송신은행이 KFTC에 지급지시를 전달하는 HTTP 클라이언트이다.
@Component
@RequiredArgsConstructor
public class KftcClient {

    private final RestClient.Builder restClientBuilder; // 스프링빈으로 등록된 RestClient.Builder를 가져온다
    private final BankProperties bankProperties; // 현재 금융기관 정보와 KFTC의 url정보를 담고있는 객채

    // 송신은행에서 KFTC에 지급지시요청을 보내고, KFTC로부터 지급지시처리 결과를 받는 메서드
    public PaymentInstructionResponse createPaymentInstruction(TransferCommand command) {
        // url을 KFTC의 url로 설정한뒤 restClient를 생성한다.
        RestClient restClient = restClientBuilder
                .baseUrl(bankProperties.kftcBaseUrl())
                .build();

        // KFTC에 지급지시 요청을 전송한다.
        PaymentInstructionResponse response = restClient.post()
                .uri("/api/payment-instructions")
                .body(new PaymentInstructionRequest(
                        command.requestId(),
                        bankProperties.currentInstitutionId().value(),
                        command.senderAccountNumber(),
                        command.receiverInstitutionId().value(),
                        command.receiverAccountNumber(),
                        command.amount().amount()
                ))
                .retrieve() // KFTC에 HTTP요청을 보낸 뒤, KFTC 기관으로부터 지급지시 처리결과 응답을 받을 떄까지 기다린다.
                .body(PaymentInstructionResponse.class); // KFTC 기관으로부터 받은 지급지시 처리결과(Json 문자열)를 응답객체로 변환한다.

        // 만약 응답 결과가 없으면 예의를 발생시킨다.
        if (response == null) {
            throw new IllegalStateException("KFTC returned an empty response");
        }
        return response;
    }

    // 송신기관이 KFTC로 보내는 지급지시정보를 담은 레코드
    public record PaymentInstructionRequest(
            String requestId,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount
    ) {
    }

    // KFTC가 송신은행에게 지급지시 처리 결과를 알려주는 응답 record이다.
    public record PaymentInstructionResponse(String instructionId, String status) {

        // 응답 상태가 아래 3가지중 하나라면 수신은행의 입금이 성공적으로 수행되었다고 판단한다.
        public boolean receiverCredited() {
            return "RECEIVER_CREDITED".equals(status)
                    || "CLEARED".equals(status)
                    || "SETTLED".equals(status);
        }
    }
}
