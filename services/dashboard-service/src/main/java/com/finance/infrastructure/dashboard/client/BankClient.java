package com.finance.infrastructure.dashboard.client;

import com.finance.infrastructure.dashboard.controller.DashboardTransferRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
// 선택한 송신은행으로 이체 요청을 전달하는 송신고객 클라이언트
public class BankClient extends UpstreamClientSupport {

    private final RestClient restClient;

    public BankClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public JsonNode transfer(String bankBaseUrl, DashboardTransferRequest request) {
        BankTransferRequest bankRequest = new BankTransferRequest(
                request.requestId(),
                request.senderAccountNumber(),
                request.receiverInstitutionId(),
                request.receiverAccountNumber(),
                request.amount()
        );

        return call(request.senderInstitutionId(), () -> restClient.post()
                .uri(bankBaseUrl + "/api/customers/{customerId}/transfers", request.senderCustomerId())
                .body(bankRequest)
                .retrieve()
                .body(JsonNode.class));
    }

    private record BankTransferRequest(
            String requestId,
            String senderAccountNumber,
            String receiverInstitutionId,
            String receiverAccountNumber,
            java.math.BigDecimal amount
    ) {
    }
}
