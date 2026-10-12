package com.finance.infrastructure.kftc.client.bank;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.kftc.config.KftcProperties;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

// KFTC가 수신은행에 입금지시를 전송하고, 송신은행과 수신은행에 BOK 최종 결제 결과를 전달한다.
@Component
@RequiredArgsConstructor
public class BankClient {

    private final RestClient.Builder restClientBuilder; // URL을 설정하고, HTTP 요청을 전송할 수 있는 RestClient를 생성
    private final KftcProperties properties; // 각 은행기관들의 URL 주소정보를 담고 있는 properties

    // 수신은행에게 입금지시를 전송하는 메서드
    public void creditReceiver(PaymentInstruction instruction) {
        // 수신은행의 url 정보를 읽는다.
        String baseUrl = properties.bankBaseUrl(
                instruction.getReceiverParticipant().getInstitutionId()
        );
        // 수신은행 url을 기반으로 RestClient를 생성한다.
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();

        // 수신은행에게 입금지시를 전송하고, 수신은행의 입금처리 결과를 리턴받는다.
        CreditResponse response = restClient.post()
                .uri("/api/interbank-transfers/credits")
                .body(new CreditRequest(
                        instruction.getInstructionId(),
                        instruction.getSenderParticipant().getInstitutionId().value(),
                        instruction.getSenderAccountNumber(),
                        instruction.getReceiverParticipant().getInstitutionId().value(),
                        instruction.getReceiverAccountNumber(),
                        instruction.amount().amount()
                ))
                .retrieve() // 수신은행에 입금지시를 전송하고, 수신은행으로부터 입금처리 결과를 받을때까지 대기한다.
                .body(CreditResponse.class); // 수신은행으로부터 받은 응답데이터(Json 문자열)을 CreditResponse 객체로 변환한다.

        // 만약 수신은행의 입급처리 결과가 null이거나 COMPLETED가 아니라면 예외를 발생시킨다.
        if (response == null || !"COMPLETED".equals(response.status())) {
            throw new IllegalStateException("receiver bank did not complete the credit");
        }
    }

    // 하나의 청산주기에 포함된 지급지시들을 기관별로 묶어 한 번에 통보한다.
    public void notifySettlement(
            InstitutionId institutionId,
            String clearingCycleId,
            String settlementBatchId,
            List<String> instructionIds
    ) {
        String baseUrl = properties.bankBaseUrl(institutionId);
        restClientBuilder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/api/interbank-settlements")
                .body(new SettlementNotificationRequest(
                        clearingCycleId,
                        settlementBatchId,
                        instructionIds
                ))
                .retrieve()
                .toBodilessEntity();
    }

    // KFTC가 수신은행에 전송하는 입금지시 요청
    public record CreditRequest(
            String instructionId,
            String senderInstitutionId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            BigDecimal amount
    ) {
    }

    // 수신은행의 입금처리 결과를 담은 객체
    public record CreditResponse(Long transactionId, String status) {
    }

    public record SettlementNotificationRequest(
            String clearingCycleId,
            String settlementBatchId,
            List<String> instructionIds
    ) {
    }
}
